package com.cq.maintenance.workorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReasonRequest(@NotBlank(message="原因不能为空") @Size(max=500,message="原因不能超过500字") String reason) {}
