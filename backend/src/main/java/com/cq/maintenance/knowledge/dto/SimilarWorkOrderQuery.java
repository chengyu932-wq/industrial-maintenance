package com.cq.maintenance.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SimilarWorkOrderQuery(
    @NotBlank(message="故障描述不能为空") @Size(max=2000,message="故障描述不能超过2000个字符") String description) {}
