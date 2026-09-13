package com.cq.maintenance.maintenance.vo;

import java.util.List;

public record MaintenanceWorkOrderVO(Long planId,String planNo,String planName,List<MaintenanceExecutionItemVO> items) {
    public MaintenanceWorkOrderVO { items=List.copyOf(items); }
}
