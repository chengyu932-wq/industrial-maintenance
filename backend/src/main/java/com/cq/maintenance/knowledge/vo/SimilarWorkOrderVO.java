package com.cq.maintenance.knowledge.vo;

import java.time.LocalDateTime;

public record SimilarWorkOrderVO(Long workOrderId,String workOrderNo,Long equipmentId,String equipmentNo,
    String equipmentName,Long equipmentTypeId,String equipmentTypeName,String faultDescription,double similarity,
    String rootCause,String repairAction,String repairResult,String spareSummary,LocalDateTime completedAt) {}
