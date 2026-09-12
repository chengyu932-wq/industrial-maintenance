package com.cq.maintenance.workorder.service;

import com.cq.maintenance.common.exception.*;
import com.cq.maintenance.workorder.entity.*;
import com.cq.maintenance.workorder.mapper.WorkOrderMapper;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class WorkOrderStateService {
    private static final Map<WorkOrderStatus,Set<WorkOrderStatus>> TRANSITIONS=Map.of(
        WorkOrderStatus.PENDING_ASSIGN,Set.of(WorkOrderStatus.ASSIGNED,WorkOrderStatus.CANCELLED),
        WorkOrderStatus.ASSIGNED,Set.of(WorkOrderStatus.PROCESSING,WorkOrderStatus.CANCELLED),
        WorkOrderStatus.PROCESSING,Set.of(WorkOrderStatus.SUSPENDED,WorkOrderStatus.PENDING_ACCEPT),
        WorkOrderStatus.SUSPENDED,Set.of(WorkOrderStatus.PROCESSING),
        WorkOrderStatus.PENDING_ACCEPT,Set.of(WorkOrderStatus.PROCESSING,WorkOrderStatus.COMPLETED),
        WorkOrderStatus.COMPLETED,Set.of(),WorkOrderStatus.CANCELLED,Set.of());
    private final WorkOrderMapper mapper;
    public WorkOrderStateService(WorkOrderMapper mapper){this.mapper=mapper;}
    public void created(WorkOrder order,Long operatorId){mapper.insertFlow(order.getId(),null,WorkOrderStatus.PENDING_ASSIGN,"CREATE",operatorId,"故障报修自动生成维修工单");}
    public void assign(WorkOrder order,Long engineerId,Long teamId,String reason,Long operatorId){
        require(order,WorkOrderStatus.PENDING_ASSIGN,WorkOrderStatus.ASSIGNED);
        if(mapper.assign(order.getId(),engineerId,teamId)!=1)duplicate();
        mapper.insertAssignment(order.getId(),engineerId,teamId,reason,operatorId);
        mapper.insertFlow(order.getId(),order.getStatus(),WorkOrderStatus.ASSIGNED,"ASSIGN",operatorId,blankToNull(reason));
    }
    public void acceptResponse(WorkOrder order,Long operatorId){
        if(order.getStatus()!=WorkOrderStatus.ASSIGNED)illegal(order,WorkOrderStatus.ASSIGNED);
        if(order.getAcceptedAt()!=null)throw new BusinessException(ErrorCode.DUPLICATE_OPERATION,"工单已接单");
        if(mapper.acceptResponse(order.getId())!=1)duplicate();
        mapper.insertFlow(order.getId(),WorkOrderStatus.ASSIGNED,WorkOrderStatus.ASSIGNED,"ACCEPT",operatorId,"工程师确认接单");
    }
    public void transition(WorkOrder order,WorkOrderStatus target,String action,String remark,Long operatorId){
        require(order,order.getStatus(),target);
        if(mapper.updateState(order.getId(),order.getStatus(),target,action)!=1)duplicate();
        mapper.insertFlow(order.getId(),order.getStatus(),target,action,operatorId,blankToNull(remark));
    }
    public void cancel(WorkOrder order,String reason,Long operatorId){
        require(order,order.getStatus(),WorkOrderStatus.CANCELLED);
        if(mapper.cancel(order.getId(),order.getStatus(),reason)!=1)duplicate();
        mapper.insertFlow(order.getId(),order.getStatus(),WorkOrderStatus.CANCELLED,"CANCEL",operatorId,reason.trim());
    }
    private void require(WorkOrder order,WorkOrderStatus current,WorkOrderStatus target){if(order==null)throw new BusinessException(ErrorCode.NOT_FOUND,"工单不存在");if(order.getStatus()!=current||!TRANSITIONS.getOrDefault(current,Set.of()).contains(target))illegal(order,target);}
    private void illegal(WorkOrder order,WorkOrderStatus target){throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION,"工单状态不能从"+order.getStatus()+"变更为"+target);}
    private void duplicate(){throw new BusinessException(ErrorCode.DUPLICATE_OPERATION,"工单状态已变化，请刷新后重试");}
    private String blankToNull(String value){return value==null||value.isBlank()?null:value.trim();}
}
