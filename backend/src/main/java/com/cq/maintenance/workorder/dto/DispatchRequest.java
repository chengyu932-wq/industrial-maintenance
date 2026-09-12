package com.cq.maintenance.workorder.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DispatchRequest(@NotNull(message="请选择工程师") Long engineerId,Long teamId,
    @Size(max=500,message="派单说明不能超过500字") String reason) {}
