package com.cq.maintenance.maintenance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("pm_plan_item")
public class MaintenancePlanItem {
    private Long id;
    private Long planId;
    private String itemName;
    private String standardDescription;
    private Integer sortNo;
    private Boolean required;
}
