package com.cq.maintenance.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("kb_article")
public class KnowledgeArticle {
    private Long id;
    private String title;
    private Long sourceWorkOrderId;
    private Long equipmentTypeId;
    private String faultSymptom;
    private String rootCause;
    private String solution;
    private String repairResult;
    private String spareSummary;
    private KnowledgeStatus status;
    private Long submittedBy;
    private Long reviewedBy;
    private String reviewRemark;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
