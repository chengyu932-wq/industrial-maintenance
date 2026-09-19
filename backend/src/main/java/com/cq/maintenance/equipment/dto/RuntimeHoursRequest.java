package com.cq.maintenance.equipment.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public record RuntimeHoursRequest(@NotNull(message="记录日期不能为空") LocalDate recordDate,@NotNull(message="新增运行小时不能为空") @DecimalMin(value="0.01",message="新增运行小时必须大于0") @Digits(integer=8,fraction=2,message="运行小时最多保留2位小数") BigDecimal increment,@Size(max=500,message="备注不能超过500个字符") String remark){}
