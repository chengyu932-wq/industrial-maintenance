package com.cq.maintenance.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StationRequest(
    @NotNull(message = "所属产线不能为空") Long lineId,
    @NotBlank(message = "工位编号不能为空") @Size(max = 50, message = "工位编号不能超过50个字符") String stationNo,
    @NotBlank(message = "工位名称不能为空") @Size(max = 100, message = "工位名称不能超过100个字符") String stationName,
    @Pattern(regexp = "ENABLED|DISABLED", message = "工位状态不合法") String status
) {}
