package com.cq.maintenance.sla.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cq.maintenance.repair.entity.RepairPriority;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("mnt_sla_rule")
public class SlaRule {
    private Long id; private RepairPriority priority; private Integer responseMinutes;
    private Integer resolveMinutes; private Boolean enabled; private LocalDateTime updatedAt;
}
