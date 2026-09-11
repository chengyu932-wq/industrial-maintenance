package com.cq.maintenance.organization.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("org_workshop")
public class Workshop {
    private Long id;
    private String workshopNo;
    private String workshopName;
    private Long managerId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
