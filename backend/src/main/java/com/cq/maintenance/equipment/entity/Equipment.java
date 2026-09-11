package com.cq.maintenance.equipment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("eqp_equipment")
public class Equipment {
    private Long id;
    private String equipmentNo;
    private String equipmentName;
    private Long typeId;
    private String model;
    private String manufacturer;
    private String specifications;
    private LocalDate manufactureDate;
    private LocalDate commissioningDate;
    private Long responsibleUserId;
    private Long responsibleTeamId;
    private Long stationId;
    private LocalDate warrantyExpireDate;
    private EquipmentStatus status;
    private BigDecimal runningHours;
    private String qrCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
