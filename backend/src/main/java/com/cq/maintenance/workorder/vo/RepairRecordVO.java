package com.cq.maintenance.workorder.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RepairRecordVO(Long id,Long workOrderId,String inspectionProcess,String rootCause,String repairAction,
    String repairResult,BigDecimal laborHours,Integer downtimeMinutes,Boolean repairable,Long createdBy,
    String engineerName,LocalDateTime createdAt,LocalDateTime updatedAt) {}
