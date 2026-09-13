package com.cq.maintenance.inventory.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record InventoryOperationRequest(
    @NotNull(message="仓库不能为空") Long warehouseId,
    @NotNull(message="备件不能为空") Long sparePartId,
    @NotNull(message="数量不能为空") @DecimalMin(value="0.01",message="数量必须大于0") @Digits(integer=10,fraction=2,message="数量最多10位整数和2位小数") BigDecimal qty,
    @DecimalMin(value="0",message="单价不能小于0") @Digits(integer=10,fraction=2,message="单价最多10位整数和2位小数") BigDecimal unitPrice,
    @Size(max=500,message="备注不能超过500个字符") String remark
) {}
