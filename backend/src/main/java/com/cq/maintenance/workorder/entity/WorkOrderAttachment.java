package com.cq.maintenance.workorder.entity;
import lombok.Data;
@Data public class WorkOrderAttachment {private Long id;private Long workOrderId;private String attachmentType;private String fileName;private String filePath;private Long uploadedBy;}
