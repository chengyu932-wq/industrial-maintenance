package com.cq.maintenance.repair.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("mnt_repair_request")
public class RepairRequest {
    private Long id;
    private String requestNo;
    private Long equipmentId;
    private Long reporterId;
    private RepairSource source;
    private RepairPriority priority;
    private String faultDescription;
    private LocalDateTime reportedAt;
    private RepairRequestStatus status;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
