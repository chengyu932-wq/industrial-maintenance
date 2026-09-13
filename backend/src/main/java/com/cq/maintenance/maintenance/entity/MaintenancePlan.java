package com.cq.maintenance.maintenance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.*;
import lombok.Data;

@Data
@TableName("pm_plan")
public class MaintenancePlan {
    private Long id;
    private String planNo;
    private String planName;
    private Long equipmentId;
    private MaintenanceCycleType cycleType;
    private Integer cycleValue;
    private LocalDate nextExecuteDate;
    private BigDecimal runningHourThreshold;
    private MaintenancePlanStatus status;
    private LocalDateTime lastGeneratedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
