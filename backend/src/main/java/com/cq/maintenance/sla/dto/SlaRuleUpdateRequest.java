package com.cq.maintenance.sla.dto;

import jakarta.validation.constraints.*;

public record SlaRuleUpdateRequest(@NotNull @Min(1) Integer responseMinutes,
    @NotNull @Min(1) Integer resolveMinutes,@NotNull Boolean enabled) {}
