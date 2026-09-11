package com.cq.maintenance.equipment.vo;

import com.cq.maintenance.equipment.entity.EquipmentStatus;
import java.time.LocalDate;

public record EquipmentListVO(Long id,String equipmentNo,String equipmentName,Long typeId,String typeCode,String typeName,
    String model,String manufacturer,String specifications,LocalDate manufactureDate,Long workshopId,String workshopNo,String workshopName,
    Long lineId,String lineNo,String lineName,Long stationId,String stationNo,String stationName,
    Long responsibleUserId,String responsibleUsername,String responsibleUserName,Long responsibleTeamId,String responsibleTeamNo,String responsibleTeamName,
    LocalDate commissioningDate,LocalDate warrantyExpireDate,EquipmentStatus status,String qrCode) {}
