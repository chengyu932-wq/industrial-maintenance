package com.cq.maintenance.knowledge.vo;

public record KnowledgeSourceRow(Long workOrderId,String workOrderNo,Long equipmentId,String equipmentNo,
    String equipmentName,Long equipmentTypeId,String equipmentTypeName,String faultDescription,
    String inspectionProcess,String rootCause,String repairAction,String repairResult,String spareSummary) {}
