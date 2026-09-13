package com.cq.maintenance.maintenance.dto;

import com.cq.maintenance.maintenance.entity.MaintenanceResult;
import jakarta.validation.constraints.*;

public record MaintenanceExecutionItemRequest(
    @NotNull Long planItemId,
    @NotNull MaintenanceResult result,
    @Size(max=100) String measuredValue,
    @Size(max=500) String remark) {}
