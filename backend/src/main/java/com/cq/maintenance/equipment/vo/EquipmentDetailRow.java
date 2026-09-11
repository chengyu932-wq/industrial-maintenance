package com.cq.maintenance.equipment.vo;

import com.cq.maintenance.equipment.entity.EquipmentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
public record EquipmentDetailRow(Long id,String equipmentNo,String equipmentName,Long typeId,String typeCode,String typeName,
    String model,String manufacturer,String specifications,LocalDate manufactureDate,LocalDate commissioningDate,
    Long responsibleUserId,String responsibleUserName,Long responsibleTeamId,String responsibleTeamName,
    Long workshopId,String workshopNo,String workshopName,Long lineId,String lineNo,String lineName,
    Long stationId,String stationNo,String stationName,LocalDate warrantyExpireDate,EquipmentStatus status,
    BigDecimal runningHours,String qrCode,LocalDateTime createdAt,LocalDateTime updatedAt) {}
