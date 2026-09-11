package com.cq.maintenance.equipment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("eqp_type")
public class EquipmentType {
    private Long id;
    private String typeCode;
    private String typeName;
    private String description;
    private String status;
}
