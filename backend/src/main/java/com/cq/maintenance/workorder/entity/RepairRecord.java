package com.cq.maintenance.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("mnt_repair_record")
public class RepairRecord {
    private Long id;
    private Long workOrderId;
    private String inspectionProcess;
    private String rootCause;
    private String repairAction;
    private String repairResult;
    private BigDecimal laborHours;
    private Integer downtimeMinutes;
    private Boolean repairable;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
