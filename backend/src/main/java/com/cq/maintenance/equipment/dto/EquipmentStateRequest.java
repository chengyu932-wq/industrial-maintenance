package com.cq.maintenance.equipment.dto;

import com.cq.maintenance.equipment.entity.EquipmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EquipmentStateRequest(@NotNull(message="目标状态不能为空") EquipmentStatus targetStatus,
                                    @NotBlank(message="状态变更原因不能为空") @Size(max=500,message="原因不能超过500个字符") String reason) {}
