package com.cq.maintenance.equipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record ScrapRequest(@NotBlank(message="报废原因不能为空") @Size(max=500,message="报废原因不能超过500个字符") String reason) {}
