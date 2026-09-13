package com.cq.maintenance.inventory.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record SparePartRequest(
    @NotBlank(message="备件编号不能为空") @Size(max=50,message="备件编号不能超过50个字符") String spareNo,
    @NotBlank(message="备件名称不能为空") @Size(max=100,message="备件名称不能超过100个字符") String spareName,
    @Size(max=200,message="规格不能超过200个字符") String specification,
    @Size(max=100,message="品牌不能超过100个字符") String brand,
    @NotBlank(message="计量单位不能为空") @Size(max=20,message="计量单位不能超过20个字符") String unit,
    @NotNull(message="单价不能为空") @DecimalMin(value="0",message="单价不能小于0") @Digits(integer=10,fraction=2,message="单价最多10位整数和2位小数") BigDecimal unitPrice,
    Long supplierId,
    @Size(max=200,message="适配型号不能超过200个字符") String compatibleModel,
    @NotNull(message="采购提前期不能为空") @Min(value=0,message="采购提前期不能小于0") Integer leadTimeDays,
    @NotNull(message="安全天数不能为空") @Min(value=0,message="安全天数不能小于0") Integer safetyDays,
    @Pattern(regexp="ENABLED|DISABLED",message="备件状态不合法") String status
) {}
