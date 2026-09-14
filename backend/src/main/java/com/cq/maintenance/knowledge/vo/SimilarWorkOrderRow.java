package com.cq.maintenance.knowledge.vo;

import java.time.LocalDateTime;

public record SimilarWorkOrderRow(Long workOrderId,String workOrderNo,Long equipmentId,String equipmentNo,
    String equipmentName,Long equipmentTypeId,String equipmentTypeName,String faultDescription,
    String rootCause,String repairAction,String repairResult,String spareSummary,LocalDateTime completedAt) {}
