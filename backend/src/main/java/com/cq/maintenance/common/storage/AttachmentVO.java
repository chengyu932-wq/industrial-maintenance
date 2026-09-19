package com.cq.maintenance.common.storage;
import java.time.LocalDateTime;
public record AttachmentVO(Long id,String category,String fileName,String fileType,Long uploadedBy,String uploaderName,LocalDateTime createdAt) {}
