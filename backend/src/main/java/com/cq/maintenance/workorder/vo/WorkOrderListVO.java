package com.cq.maintenance.workorder.vo;

import com.cq.maintenance.repair.entity.RepairPriority;
import com.cq.maintenance.workorder.entity.*;
import java.time.LocalDateTime;

public record WorkOrderListVO(Long id,String workOrderNo,WorkOrderType workOrderType,Long repairRequestId,
    String requestNo,Long equipmentId,String equipmentNo,String equipmentName,Long workshopId,String workshopName,
    RepairPriority priority,WorkOrderStatus status,Long reporterId,String reporterName,Long assignedEngineerId,
    String assignedEngineerName,Long assignedTeamId,String assignedTeamName,LocalDateTime createdAt,
    LocalDateTime assignedAt,LocalDateTime acceptedAt,LocalDateTime startedAt,LocalDateTime submittedAt,
    LocalDateTime completedAt,Integer acceptanceReturnCount) {}
