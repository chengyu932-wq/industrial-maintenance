package com.cq.maintenance.repair.dto;

import com.cq.maintenance.repair.entity.RepairPriority;
import com.cq.maintenance.repair.entity.RepairRequestStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class RepairRequestQuery {
    private String requestNo;
    private Long equipmentId;
    private RepairPriority priority;
    private RepairRequestStatus status;
    @Min(value=1,message="页码必须大于0") private long page=1;
    @Min(value=1,message="每页数量必须大于0") @Max(value=100,message="每页最多100条") private long size=20;
    public long getOffset(){return (page-1)*size;}
}
