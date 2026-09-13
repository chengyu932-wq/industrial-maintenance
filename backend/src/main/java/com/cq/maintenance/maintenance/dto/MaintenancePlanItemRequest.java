package com.cq.maintenance.maintenance.dto;

import jakarta.validation.constraints.*;

public record MaintenancePlanItemRequest(
    @NotBlank @Size(max=100) String itemName,
    @Size(max=500) String standardDescription,
    @NotNull @Min(0) Integer sortNo,
    @NotNull Boolean required) {}
