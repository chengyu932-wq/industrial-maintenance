package com.cq.maintenance.repair.dto;

import com.cq.maintenance.repair.entity.RepairPriority;
import com.cq.maintenance.repair.entity.RepairSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RepairRequestCreateRequest(
    @NotNull(message="请选择设备") Long equipmentId,
    @NotNull(message="请选择故障等级") RepairPriority priority,
    @NotBlank(message="故障描述不能为空") @Size(max=5000,message="故障描述不能超过5000字") String faultDescription,
    @NotNull(message="请选择报修来源") RepairSource source
) {}
