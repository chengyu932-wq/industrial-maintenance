package com.cq.maintenance.notification.entity;
import java.time.LocalDateTime;
import lombok.Data;
@Data
public class Notification {
    private Long id;private Long receiverId;private String messageType;private String title;private String content;
    private String businessType;private Long businessId;private Boolean read;private LocalDateTime readAt;private LocalDateTime createdAt;
}
