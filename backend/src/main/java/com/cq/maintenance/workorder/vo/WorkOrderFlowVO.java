package com.cq.maintenance.workorder.vo;

import com.cq.maintenance.workorder.entity.WorkOrderStatus;
import java.time.LocalDateTime;

public record WorkOrderFlowVO(Long id,Long workOrderId,WorkOrderStatus fromStatus,WorkOrderStatus toStatus,
    String action,Long operatorId,String operatorName,String remark,LocalDateTime createdAt) {}
