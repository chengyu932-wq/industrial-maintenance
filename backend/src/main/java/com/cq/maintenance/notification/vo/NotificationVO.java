package com.cq.maintenance.notification.vo;
import java.time.LocalDateTime;
public record NotificationVO(Long id,String messageType,String title,String content,String businessType,
    Long businessId,Boolean read,LocalDateTime readAt,LocalDateTime createdAt) {}
