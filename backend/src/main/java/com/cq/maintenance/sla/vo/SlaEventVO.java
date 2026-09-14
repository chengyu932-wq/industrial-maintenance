package com.cq.maintenance.sla.vo;
import com.cq.maintenance.repair.entity.RepairPriority;
import com.cq.maintenance.sla.entity.SlaEventType;
import java.time.LocalDateTime;
public record SlaEventVO(Long id,Long workOrderId,String workOrderNo,RepairPriority priority,SlaEventType eventType,
    LocalDateTime deadline,LocalDateTime occurredAt,Long escalatedToUserId,String escalatedToUserName,
    Boolean handled,LocalDateTime handledAt,String remark) {}
