package com.cq.maintenance.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KnowledgeArticleRequest(
    @NotBlank(message="知识标题不能为空") @Size(max=200,message="知识标题不能超过200个字符") String title,
    @NotBlank(message="故障现象不能为空") @Size(max=5000,message="故障现象不能超过5000字") String faultSymptom,
    @NotBlank(message="故障原因不能为空") @Size(max=5000,message="故障原因不能超过5000字") String rootCause,
    @NotBlank(message="解决方法不能为空") @Size(max=5000,message="解决方法不能超过5000字") String solution,
    @NotBlank(message="维修结果不能为空") @Size(max=5000,message="维修结果不能超过5000字") String repairResult) {}
