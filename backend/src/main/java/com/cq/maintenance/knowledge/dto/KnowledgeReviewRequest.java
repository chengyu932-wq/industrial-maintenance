package com.cq.maintenance.knowledge.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record KnowledgeReviewRequest(
    @NotNull(message="审核结论不能为空") Boolean approved,
    @Size(max=500,message="审核意见不能超过500个字符") String remark) {}
