package com.cq.maintenance.maintenance.dto;

import com.cq.maintenance.maintenance.entity.MaintenanceCycleType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MaintenancePlanRequest(
    @NotBlank @Size(max=50) String planNo,
    @NotBlank @Size(max=100) String planName,
    @NotNull Long equipmentId,
    @NotNull MaintenanceCycleType cycleType,
    @NotNull @Min(1) @Max(100) Integer cycleValue,
    @NotNull LocalDate nextExecuteDate,
    @DecimalMin(value="0.00") BigDecimal runningHourThreshold) {}
