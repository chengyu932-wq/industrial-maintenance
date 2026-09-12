package com.cq.maintenance.workorder.dto;

import com.cq.maintenance.repair.entity.RepairPriority;
import com.cq.maintenance.workorder.entity.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import lombok.Data;

@Data
public class WorkOrderQuery {
    private String workOrderNo;
    private WorkOrderStatus status;
    private WorkOrderType type;
    private Long equipmentId;
    private String equipmentKeyword;
    private RepairPriority priority;
    private Long reporterId;
    private Long engineerId;
    private Long teamId;
    private Long workshopId;
    private LocalDate startDate;
    private LocalDate endDate;
    @Min(value=1,message="页码必须大于0") private long page=1;
    @Min(value=1,message="每页数量必须大于0") @Max(value=100,message="每页最多100条") private long size=20;
    public long getOffset(){return (page-1)*size;}
}
