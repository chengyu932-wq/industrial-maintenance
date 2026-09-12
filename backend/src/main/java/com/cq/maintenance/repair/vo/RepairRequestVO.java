package com.cq.maintenance.repair.vo;

import com.cq.maintenance.repair.entity.*;
import com.cq.maintenance.workorder.entity.WorkOrderStatus;
import java.time.LocalDateTime;

public record RepairRequestVO(Long id,String requestNo,Long equipmentId,String equipmentNo,String equipmentName,
    Long reporterId,String reporterName,RepairSource source,RepairPriority priority,String faultDescription,
    LocalDateTime reportedAt,RepairRequestStatus status,Long workOrderId,String workOrderNo,WorkOrderStatus workOrderStatus) {}
