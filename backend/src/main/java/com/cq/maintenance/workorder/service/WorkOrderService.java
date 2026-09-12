package com.cq.maintenance.workorder.service;

import com.cq.maintenance.common.exception.*;
import com.cq.maintenance.common.response.PageResult;
import com.cq.maintenance.equipment.entity.EquipmentStatus;
import com.cq.maintenance.equipment.service.EquipmentStateService;
import com.cq.maintenance.security.*;
import com.cq.maintenance.workorder.dto.*;
import com.cq.maintenance.workorder.entity.*;
import com.cq.maintenance.workorder.mapper.WorkOrderMapper;
import com.cq.maintenance.workorder.vo.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkOrderService {
    private final WorkOrderMapper mapper;private final WorkOrderScopeService scopes;private final WorkOrderStateService states;private final EquipmentStateService equipmentStates;
    public WorkOrderService(WorkOrderMapper mapper,WorkOrderScopeService scopes,WorkOrderStateService states,EquipmentStateService equipmentStates){this.mapper=mapper;this.scopes=scopes;this.states=states;this.equipmentStates=equipmentStates;}
    public PageResult<WorkOrderListVO> page(WorkOrderQuery query){var scope=scopes.current();return PageResult.of(mapper.findWorkOrderPage(query,scope),mapper.countWorkOrderPage(query,scope),query.getPage(),query.getSize());}
    public WorkOrderDetailVO detail(Long id){scopes.assertVisible(id);WorkOrderListVO summary=requiredSummary(id);return new WorkOrderDetailVO(summary,summary.repairRequestId()==null?null:mapper.findRepairRequest(summary.repairRequestId(),scopes.current()),mapper.findRepairRecordVO(id),mapper.findFlows(id));}
    public List<WorkOrderFlowVO> flows(Long id){scopes.assertVisible(id);return mapper.findFlows(id);}
    public List<EngineerOptionVO> engineers(Long id){scopes.assertCanAssign(id);LoginUser user=SecurityUtils.currentUser();List<EngineerOptionVO> all=mapper.findEngineers();if(user.roleCodes().contains("ADMIN"))return all;return all.stream().filter(e->(user.workshopId()!=null&&user.workshopId().equals(e.workshopId()))||(e.teamId()!=null&&user.teamIds().contains(e.teamId()))).toList();}

    @Transactional public void assign(Long id,DispatchRequest input){
        scopes.assertCanAssign(id);WorkOrder order=requiredLocked(id);EngineerOptionVO engineer=mapper.findEngineer(input.engineerId());if(engineer==null)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"目标用户不存在、已禁用或不具备工程师角色");
        Long teamId=input.teamId()==null?engineer.teamId():input.teamId();if(teamId==null||mapper.countEnabledTeam(teamId)==0)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"目标工程师未关联有效班组");
        if(!teamId.equals(engineer.teamId()))throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"目标工程师不属于所选班组");
        if(!SecurityUtils.currentUser().roleCodes().contains("ADMIN")&&engineers(id).stream().noneMatch(e->e.id().equals(engineer.id())))throw new BusinessException(ErrorCode.DATA_FORBIDDEN,"目标工程师不在当前主管允许范围");
        states.assign(order,engineer.id(),teamId,input.reason(),SecurityUtils.currentUser().userId());
    }
    @Transactional public void acceptResponse(Long id){WorkOrder order=requiredLockedVisible(id);scopes.assertAssignedEngineer(order);states.acceptResponse(order,SecurityUtils.currentUser().userId());}
    @Transactional public void start(Long id){WorkOrder order=requiredLockedVisible(id);scopes.assertAssignedEngineer(order);Long operator=SecurityUtils.currentUser().userId();states.transition(order,WorkOrderStatus.PROCESSING,"START","开始维修",operator);equipmentStates.transition(order.getEquipmentId(),EquipmentStatus.REPAIRING,"维修工单开始处理："+order.getWorkOrderNo(),"WORK_ORDER",order.getId(),operator);}
    @Transactional public void suspend(Long id,ReasonRequest input){WorkOrder order=requiredLockedVisible(id);scopes.assertAssignedEngineer(order);states.transition(order,WorkOrderStatus.SUSPENDED,"SUSPEND",input.reason(),SecurityUtils.currentUser().userId());}
    @Transactional public void resume(Long id,ReasonRequest input){WorkOrder order=requiredLockedVisible(id);scopes.assertAssignedEngineer(order);states.transition(order,WorkOrderStatus.PROCESSING,"RESUME",input.reason(),SecurityUtils.currentUser().userId());}
    @Transactional public void saveRepairRecord(Long id,RepairRecordRequest input){
        WorkOrder order=requiredLockedVisible(id);scopes.assertAssignedEngineer(order);if(order.getStatus()!=WorkOrderStatus.PROCESSING)throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION,"仅处理中的工单可以填写维修记录");
        RepairRecord record=mapper.findRepairRecord(id);if(record==null){record=new RepairRecord();record.setWorkOrderId(id);record.setCreatedBy(SecurityUtils.currentUser().userId());copy(record,input);mapper.insertRepairRecord(record);}else{copy(record,input);mapper.updateRepairRecord(record);}
    }
    @Transactional public void submitAcceptance(Long id){
        WorkOrder order=requiredLockedVisible(id);scopes.assertAssignedEngineer(order);RepairRecord record=mapper.findRepairRecord(id);assertComplete(record);
        states.transition(order,WorkOrderStatus.PENDING_ACCEPT,"SUBMIT","维修完成，提交验收",SecurityUtils.currentUser().userId());
    }
    @Transactional public void pass(Long id,AcceptanceRequest input){
        WorkOrderListVO summary=requiredSummary(id);scopes.assertCanAccept(summary);WorkOrder order=requiredLocked(id);assertComplete(mapper.findRepairRecord(id));Long operator=SecurityUtils.currentUser().userId();
        states.transition(order,WorkOrderStatus.COMPLETED,"ACCEPT_PASS",input.remark(),operator);equipmentStates.transition(order.getEquipmentId(),EquipmentStatus.RUNNING,"维修验收通过："+order.getWorkOrderNo(),"WORK_ORDER",order.getId(),operator);
    }
    @Transactional public void reject(Long id,ReasonRequest input){WorkOrderListVO summary=requiredSummary(id);scopes.assertCanAccept(summary);WorkOrder order=requiredLocked(id);states.transition(order,WorkOrderStatus.PROCESSING,"ACCEPT_RETURN",input.reason(),SecurityUtils.currentUser().userId());}
    @Transactional public void cancel(Long id,CancelWorkOrderRequest input){scopes.assertCanAssign(id);cancelInternal(requiredLocked(id),input);}
    @Transactional public void cancelByReporter(Long id,CancelWorkOrderRequest input){WorkOrderListVO summary=requiredSummary(id);scopes.assertReporter(summary);cancelInternal(requiredLocked(id),input);}
    private void cancelInternal(WorkOrder order,CancelWorkOrderRequest input){
        if(input.equipmentTargetStatus()!=EquipmentStatus.RUNNING&&input.equipmentTargetStatus()!=EquipmentStatus.STOPPED)throw new BusinessException(ErrorCode.PARAMETER_ERROR,"取消后的设备状态只能为运行或停用");
        Long operator=SecurityUtils.currentUser().userId();states.cancel(order,input.reason(),operator);if(order.getRepairRequestId()!=null&&mapper.cancelRepairRequest(order.getRepairRequestId(),input.reason())!=1)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"关联报修状态异常");
        if(mapper.countActiveRepairOrders(order.getEquipmentId(),order.getId())==0)equipmentStates.cancelWorkOrder(order.getEquipmentId(),input.equipmentTargetStatus(),input.reason(),order.getId(),operator);
    }
    private WorkOrder requiredLockedVisible(Long id){scopes.assertVisible(id);return requiredLocked(id);}
    private WorkOrder requiredLocked(Long id){WorkOrder order=mapper.lockWorkOrder(id);if(order==null)throw new BusinessException(ErrorCode.NOT_FOUND,"工单不存在");return order;}
    private WorkOrderListVO requiredSummary(Long id){WorkOrderListVO result=mapper.findWorkOrderSummary(id);if(result==null)throw new BusinessException(ErrorCode.NOT_FOUND,"工单不存在");return result;}
    private void copy(RepairRecord target,RepairRecordRequest input){target.setInspectionProcess(trim(input.inspectionProcess()));target.setRootCause(trim(input.rootCause()));target.setRepairAction(trim(input.repairAction()));target.setRepairResult(trim(input.repairResult()));target.setLaborHours(input.laborHours());target.setDowntimeMinutes(input.downtimeMinutes());target.setRepairable(input.repairable());}
    private void assertComplete(RepairRecord r){if(r==null||blank(r.getInspectionProcess())||blank(r.getRootCause())||blank(r.getRepairAction())||blank(r.getRepairResult())||r.getRepairable()==null)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"维修记录不完整，请填写排查过程、根因、处理措施、维修结果和是否修复");if(!r.getRepairable())throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"无法修复需走设备停用或报废异常处置，不能提交普通验收");}
    private boolean blank(String value){return value==null||value.isBlank();}private String trim(String value){return value==null?null:value.trim();}
}
