package com.cq.maintenance.workorder.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record RepairRecordRequest(
    @Size(max=5000,message="排查过程不能超过5000字") String inspectionProcess,
    @Size(max=5000,message="根因分析不能超过5000字") String rootCause,
    @Size(max=5000,message="处理措施不能超过5000字") String repairAction,
    @Size(max=5000,message="维修结果不能超过5000字") String repairResult,
    @NotNull(message="请填写维修工时") @DecimalMin(value="0.0",message="维修工时不能小于0") BigDecimal laborHours,
    @NotNull(message="请填写停机时长") @Min(value=0,message="停机时长不能小于0") Integer downtimeMinutes,
    Boolean repairable
) {}
