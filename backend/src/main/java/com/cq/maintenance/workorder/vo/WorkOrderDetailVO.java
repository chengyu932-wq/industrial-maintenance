package com.cq.maintenance.workorder.vo;

import com.cq.maintenance.repair.vo.RepairRequestVO;
import java.util.List;

public record WorkOrderDetailVO(WorkOrderListVO workOrder,RepairRequestVO repairRequest,
    RepairRecordVO repairRecord,List<WorkOrderFlowVO> flows) {
    public WorkOrderDetailVO { flows=List.copyOf(flows); }
}
