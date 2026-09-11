package com.cq.maintenance.equipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record EquipmentCreateRequest(
    @NotBlank(message="设备编号不能为空") @Size(max=50,message="设备编号不能超过50个字符") String equipmentNo,
    @NotBlank(message="设备名称不能为空") @Size(max=100,message="设备名称不能超过100个字符") String equipmentName,
    @NotNull(message="设备类型不能为空") Long typeId,
    @Size(max=100,message="型号不能超过100个字符") String model,
    @Size(max=100,message="厂商不能超过100个字符") String manufacturer,
    String specifications, LocalDate manufactureDate, LocalDate commissioningDate,
    Long responsibleUserId, Long responsibleTeamId,
    @NotNull(message="工位不能为空") Long stationId,
    LocalDate warrantyExpireDate
) {}
