package com.cq.maintenance.sla.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SlaEvent {
    private Long id;private Long workOrderId;private SlaEventType eventType;private Long ruleId;
    private LocalDateTime occurredAt;private Long escalatedToUserId;private Long messageId;
    private Boolean handled;private LocalDateTime handledAt;private String remark;
}
