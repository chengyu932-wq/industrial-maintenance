package com.cq.maintenance.inventory.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ReturnSpareRequest(
    @NotNull(message="退库数量不能为空") @DecimalMin(value="0.01",message="退库数量必须大于0") @Digits(integer=10,fraction=2,message="数量最多10位整数和2位小数") BigDecimal qty,
    @NotBlank(message="退库原因不能为空") @Size(max=500,message="退库原因不能超过500个字符") String remark
) {}
