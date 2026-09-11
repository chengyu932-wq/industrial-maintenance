package com.cq.maintenance.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LineRequest(
    @NotNull(message = "所属车间不能为空") Long workshopId,
    @NotBlank(message = "产线编号不能为空") @Size(max = 50, message = "产线编号不能超过50个字符") String lineNo,
    @NotBlank(message = "产线名称不能为空") @Size(max = 100, message = "产线名称不能超过100个字符") String lineName,
    @Pattern(regexp = "ENABLED|DISABLED", message = "产线状态不合法") String status
) {}
