package com.cq.maintenance.dispatch.vo;

import com.cq.maintenance.workorder.entity.WorkOrderStatus;
import com.cq.maintenance.workorder.entity.WorkOrderType;

public record DispatchContext(Long workOrderId,String workOrderNo,WorkOrderType workOrderType,
    WorkOrderStatus status,Long equipmentTypeId,Long responsibleTeamId,Long workshopId) {}
