package com.cq.maintenance.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WarehouseRequest(
    @NotBlank(message="仓库编号不能为空") @Size(max=50,message="仓库编号不能超过50个字符") String warehouseNo,
    @NotBlank(message="仓库名称不能为空") @Size(max=100,message="仓库名称不能超过100个字符") String warehouseName,
    @Size(max=255,message="仓库位置不能超过255个字符") String location,
    Long managerId,
    @Pattern(regexp="ENABLED|DISABLED",message="仓库状态不合法") String status
) {}
