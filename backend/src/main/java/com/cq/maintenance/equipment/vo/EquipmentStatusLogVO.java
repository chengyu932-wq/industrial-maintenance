package com.cq.maintenance.equipment.vo;

import com.cq.maintenance.equipment.entity.EquipmentStatus;
import java.time.LocalDateTime;
public record EquipmentStatusLogVO(Long id,Long equipmentId,EquipmentStatus fromStatus,EquipmentStatus toStatus,
    String sourceType,Long sourceId,String reason,Long operatorId,String operatorName,LocalDateTime changedAt) {}
