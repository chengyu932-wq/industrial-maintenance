package com.cq.maintenance.equipment.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EquipmentStatus {
    PENDING("待启用"), RUNNING("运行"), FAULT("故障"), REPAIRING("维修中"), STOPPED("停用"), SCRAPPED("报废");
    @EnumValue private final String code;
    private final String label;
    EquipmentStatus(String label){this.code=name();this.label=label;}
    @JsonValue public String code(){return code;}
    public String label(){return label;}
}
