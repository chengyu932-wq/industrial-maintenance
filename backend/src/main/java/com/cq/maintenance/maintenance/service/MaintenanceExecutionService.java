package com.cq.maintenance.maintenance.service;

import com.cq.maintenance.common.exception.*;
import com.cq.maintenance.maintenance.dto.MaintenanceExecutionItemRequest;
import com.cq.maintenance.maintenance.mapper.MaintenanceMapper;
import com.cq.maintenance.maintenance.vo.MaintenanceWorkOrderVO;
import com.cq.maintenance.security.*;
import com.cq.maintenance.workorder.entity.*;
import com.cq.maintenance.workorder.mapper.WorkOrderMapper;
import com.cq.maintenance.workorder.service.WorkOrderScopeService;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenanceExecutionService {
    private final MaintenanceMapper mapper;private final WorkOrderMapper orders;private final WorkOrderScopeService scopes;
    public MaintenanceExecutionService(MaintenanceMapper mapper,WorkOrderMapper orders,WorkOrderScopeService scopes){this.mapper=mapper;this.orders=orders;this.scopes=scopes;}
    public MaintenanceWorkOrderVO detail(Long workOrderId){scopes.assertVisible(workOrderId);WorkOrder order=requiredOrder(workOrderId);assertMaintenance(order);var plan=mapper.findWorkOrderPlan(workOrderId);return new MaintenanceWorkOrderVO(plan.planId(),plan.planNo(),plan.planName(),mapper.findExecutionItems(workOrderId));}
    @Transactional public void save(Long workOrderId,List<MaintenanceExecutionItemRequest> input){WorkOrder order=orders.lockWorkOrder(workOrderId);if(order==null)throw new BusinessException(ErrorCode.NOT_FOUND,"工单不存在");scopes.assertVisible(workOrderId);scopes.assertAssignedEngineer(order);assertMaintenance(order);if(order.getStatus()!=WorkOrderStatus.PROCESSING)throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION,"仅处理中的保养工单可以填写检查结果");if(input==null||input.isEmpty())throw new BusinessException(ErrorCode.PARAMETER_ERROR,"至少提交一个检查结果");Set<Long> ids=new HashSet<>();for(var item:input){if(!ids.add(item.planItemId()))throw new BusinessException(ErrorCode.PARAMETER_ERROR,"检查项目不能重复提交");if(mapper.countPlanItem(order.getPmPlanId(),item.planItemId())==0)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"检查项目不属于当前保养计划");mapper.upsertExecution(workOrderId,item.planItemId(),item.result(),trim(item.measuredValue()),trim(item.remark()),SecurityUtils.currentUser().userId());}}
    public void assertComplete(Long workOrderId){if(mapper.countIncompleteRequired(workOrderId)>0)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"必检项目尚未全部完成");}
    private WorkOrder requiredOrder(Long id){WorkOrder o=orders.findWorkOrder(id);if(o==null)throw new BusinessException(ErrorCode.NOT_FOUND,"工单不存在");return o;}
    private void assertMaintenance(WorkOrder o){if(o.getWorkOrderType()!=WorkOrderType.MAINTENANCE)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"当前工单不是保养工单");}
    private String trim(String value){return value==null||value.isBlank()?null:value.trim();}
}
