package com.cq.maintenance.knowledge.vo;

import com.cq.maintenance.knowledge.entity.KnowledgeStatus;
import java.time.LocalDateTime;

public record KnowledgeArticleVO(Long id,String title,Long sourceWorkOrderId,String sourceWorkOrderNo,
    Long equipmentId,String equipmentNo,String equipmentName,Long equipmentTypeId,String equipmentTypeName,
    String faultSymptom,String rootCause,String solution,String repairResult,String spareSummary,
    KnowledgeStatus status,Long submittedBy,String submitterName,Long reviewedBy,String reviewerName,
    String reviewRemark,LocalDateTime reviewedAt,LocalDateTime createdAt,LocalDateTime updatedAt) {}
