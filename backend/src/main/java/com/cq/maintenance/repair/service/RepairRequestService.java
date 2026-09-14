package com.cq.maintenance.repair.service;

import com.cq.maintenance.common.exception.*;
import com.cq.maintenance.common.response.PageResult;
import com.cq.maintenance.equipment.entity.*;
import com.cq.maintenance.equipment.service.EquipmentStateService;
import com.cq.maintenance.repair.dto.*;
import com.cq.maintenance.repair.entity.*;
import com.cq.maintenance.repair.vo.*;
import com.cq.maintenance.security.SecurityUtils;
import com.cq.maintenance.workorder.dto.CancelWorkOrderRequest;
import com.cq.maintenance.workorder.entity.*;
import com.cq.maintenance.workorder.mapper.WorkOrderMapper;
import com.cq.maintenance.workorder.service.*;
import com.cq.maintenance.sla.service.SlaService;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepairRequestService {
    private static final Set<String> CREATE_ROLES=Set.of("ADMIN","MAINTENANCE_SUPERVISOR","ENGINEER","REPORTER");
    private final WorkOrderMapper mapper;private final WorkOrderScopeService scopes;private final BusinessNumberService numbers;
    private final EquipmentStateService equipmentStates;private final WorkOrderStateService states;private final WorkOrderService workOrders;private final SlaService sla;
    public RepairRequestService(WorkOrderMapper mapper,WorkOrderScopeService scopes,BusinessNumberService numbers,EquipmentStateService equipmentStates,WorkOrderStateService states,WorkOrderService workOrders,SlaService sla){this.mapper=mapper;this.scopes=scopes;this.numbers=numbers;this.equipmentStates=equipmentStates;this.states=states;this.workOrders=workOrders;this.sla=sla;}
    @Transactional public RepairCreateVO create(RepairRequestCreateRequest input){
        var user=SecurityUtils.currentUser();if(user.roleCodes().stream().noneMatch(CREATE_ROLES::contains))throw new BusinessException(ErrorCode.DATA_FORBIDDEN,"当前角色不能创建故障报修");
        if(input.source()==RepairSource.ENGINEER&&!user.roleCodes().contains("ENGINEER")&&!user.roleCodes().contains("ADMIN")&&!user.roleCodes().contains("MAINTENANCE_SUPERVISOR"))throw new BusinessException(ErrorCode.DATA_FORBIDDEN,"仅工程师或管理人员可以代报修");
        Equipment equipment=mapper.lockEquipment(input.equipmentId());if(equipment==null)throw new BusinessException(ErrorCode.NOT_FOUND,"设备不存在");
        if(equipment.getStatus()==EquipmentStatus.SCRAPPED)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"报废设备禁止报修");
        if(equipment.getStatus()!=EquipmentStatus.RUNNING)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"仅运行状态设备可以创建普通故障报修");
        if(mapper.countActiveRepairOrders(equipment.getId(),null)>0)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"该设备存在未完成维修工单");
        RepairRequest request=new RepairRequest();request.setRequestNo(numbers.next("RR"));request.setEquipmentId(equipment.getId());request.setReporterId(user.userId());request.setSource(input.source());request.setPriority(input.priority());request.setFaultDescription(input.faultDescription().trim());request.setStatus(RepairRequestStatus.SUBMITTED);mapper.insertRepairRequest(request);
        WorkOrder order=new WorkOrder();order.setWorkOrderNo(numbers.next("WO"));order.setWorkOrderType(WorkOrderType.REPAIR);order.setRepairRequestId(request.getId());order.setEquipmentId(equipment.getId());order.setPriority(input.priority());order.setStatus(WorkOrderStatus.PENDING_ASSIGN);order.setCreatedBy(user.userId());mapper.insertWorkOrder(order);sla.bindRepairOrder(order.getId(),input.priority());
        if(mapper.markRepairConverted(request.getId())!=1)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"报修状态已变化");
        equipmentStates.transition(equipment.getId(),EquipmentStatus.FAULT,"故障报修："+request.getRequestNo(),"WORK_ORDER",order.getId(),user.userId());
        states.created(order,user.userId());return new RepairCreateVO(request.getId(),request.getRequestNo(),order.getId(),order.getWorkOrderNo());
    }
    public PageResult<RepairRequestVO> page(RepairRequestQuery query){var scope=scopes.current();return PageResult.of(mapper.findRepairPage(query,scope),mapper.countRepairPage(query,scope),query.getPage(),query.getSize());}
    public RepairRequestVO detail(Long id){RepairRequestVO result=mapper.findRepairRequest(id,scopes.current());if(result==null)throw new BusinessException(ErrorCode.DATA_FORBIDDEN,"报修不存在或无权访问");return result;}
    @Transactional public void cancel(Long repairRequestId,CancelWorkOrderRequest input){RepairRequestVO request=detail(repairRequestId);if(request.workOrderId()==null)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"报修尚未生成维修工单");workOrders.cancelByReporter(request.workOrderId(),input);}
}
