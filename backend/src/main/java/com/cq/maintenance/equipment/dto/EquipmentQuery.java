package com.cq.maintenance.equipment.dto;

import com.cq.maintenance.equipment.entity.EquipmentStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class EquipmentQuery {
    private String keyword;
    private Long typeId;
    private Long workshopId;
    private Long lineId;
    private Long stationId;
    private EquipmentStatus status;
    private Long responsibleUserId;
    @Min(value=1,message="页码必须大于0") private long page=1;
    @Min(value=1,message="每页数量必须大于0") @Max(value=100,message="每页最多100条") private long size=20;
    public long offset(){return (page-1)*size;}
    public long getOffset(){return offset();}
}
