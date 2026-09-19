package com.cq.maintenance.audit.entity;
import java.time.LocalDateTime;
public record OperationLog(Long id,Long userId,String username,String module,String operation,String requestUri,String httpMethod,String ipAddress,String requestSummary,String result,String errorMessage,Long durationMs,LocalDateTime createdAt){}
