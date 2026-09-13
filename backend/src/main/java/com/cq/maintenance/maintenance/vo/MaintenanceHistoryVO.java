package com.cq.maintenance.maintenance.vo;

import com.cq.maintenance.workorder.entity.WorkOrderStatus;
import java.time.LocalDateTime;

public record MaintenanceHistoryVO(Long workOrderId,String workOrderNo,Long planId,String planNo,String planName,
    WorkOrderStatus status,String engineerName,LocalDateTime createdAt,LocalDateTime completedAt,long completedItems,long abnormalItems) {}
