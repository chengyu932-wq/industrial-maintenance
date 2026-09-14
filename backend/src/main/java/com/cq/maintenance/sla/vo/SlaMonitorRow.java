package com.cq.maintenance.sla.vo;
import com.cq.maintenance.sla.entity.SlaEventType;
import java.time.LocalDateTime;
public record SlaMonitorRow(Long workOrderId,String workOrderNo,Long ruleId,SlaEventType eventType,
    LocalDateTime deadline,Long escalationUserId,String escalationUserName) {}
