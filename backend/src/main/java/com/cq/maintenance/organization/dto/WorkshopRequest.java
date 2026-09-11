package com.cq.maintenance.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WorkshopRequest(
    @NotBlank(message = "车间编号不能为空") @Size(max = 50, message = "车间编号不能超过50个字符") String workshopNo,
    @NotBlank(message = "车间名称不能为空") @Size(max = 100, message = "车间名称不能超过100个字符") String workshopName,
    Long managerId,
    @Pattern(regexp = "ENABLED|DISABLED", message = "车间状态不合法") String status
) {}
