package com.cq.maintenance.workorder.vo;

import com.cq.maintenance.repair.vo.RepairRequestVO;
import com.cq.maintenance.inventory.vo.WorkOrderSpareVO;
import java.util.List;

public record WorkOrderDetailVO(WorkOrderListVO workOrder,RepairRequestVO repairRequest,
    RepairRecordVO repairRecord,List<WorkOrderSpareVO> spares,List<WorkOrderFlowVO> flows) {
    public WorkOrderDetailVO { spares=List.copyOf(spares);flows=List.copyOf(flows); }
}
