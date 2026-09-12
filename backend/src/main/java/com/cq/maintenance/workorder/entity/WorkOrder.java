package com.cq.maintenance.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cq.maintenance.repair.entity.RepairPriority;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("mnt_work_order")
public class WorkOrder {
    private Long id;
    private String workOrderNo;
    private WorkOrderType workOrderType;
    private Long repairRequestId;
    private Long pmPlanId;
    private Long equipmentId;
    private RepairPriority priority;
    private WorkOrderStatus status;
    private Long assignedEngineerId;
    private Long assignedTeamId;
    private LocalDateTime createdAt;
    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private Integer acceptanceReturnCount;
    private Long createdBy;
    private LocalDateTime updatedAt;
}
