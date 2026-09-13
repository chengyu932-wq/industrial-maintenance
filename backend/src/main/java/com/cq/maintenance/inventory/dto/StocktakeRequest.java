package com.cq.maintenance.inventory.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record StocktakeRequest(
    @NotNull(message="仓库不能为空") Long warehouseId,
    @NotNull(message="备件不能为空") Long sparePartId,
    @NotNull(message="实际数量不能为空") @DecimalMin(value="0",message="实际数量不能小于0") @Digits(integer=10,fraction=2,message="数量最多10位整数和2位小数") BigDecimal actualQty,
    @NotBlank(message="盘点说明不能为空") @Size(max=500,message="盘点说明不能超过500个字符") String remark
) {}
