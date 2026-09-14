package com.cq.maintenance.sla.vo;
import com.cq.maintenance.repair.entity.RepairPriority;
import java.time.LocalDateTime;
public record SlaRuleVO(Long id,RepairPriority priority,Integer responseMinutes,Integer resolveMinutes,Boolean enabled,LocalDateTime updatedAt) {}
