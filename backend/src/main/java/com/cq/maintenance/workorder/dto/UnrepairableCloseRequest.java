package com.cq.maintenance.workorder.dto;

import com.cq.maintenance.equipment.entity.EquipmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UnrepairableCloseRequest(@NotBlank String reason,@NotNull EquipmentStatus equipmentTargetStatus) {}
