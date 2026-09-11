package com.cq.maintenance.organization.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("org_line")
public class ProductionLine {
    private Long id;
    private Long workshopId;
    private String lineNo;
    private String lineName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
