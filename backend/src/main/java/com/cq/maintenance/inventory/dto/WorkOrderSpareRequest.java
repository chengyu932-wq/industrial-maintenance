package com.cq.maintenance.inventory.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record WorkOrderSpareRequest(
    @NotNull(message="仓库不能为空") Long warehouseId,
    @NotNull(message="备件不能为空") Long sparePartId,
    @NotNull(message="领用数量不能为空") @DecimalMin(value="0.01",message="领用数量必须大于0") @Digits(integer=10,fraction=2,message="数量最多10位整数和2位小数") BigDecimal qty,
    @Size(max=500,message="领用说明不能超过500个字符") String remark
) {}
