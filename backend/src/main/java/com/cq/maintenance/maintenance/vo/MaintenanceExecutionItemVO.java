package com.cq.maintenance.maintenance.vo;

import com.cq.maintenance.maintenance.entity.MaintenanceResult;
import java.time.LocalDateTime;

public record MaintenanceExecutionItemVO(Long planItemId,String itemName,String standardDescription,Integer sortNo,
    Boolean required,MaintenanceResult result,String measuredValue,String remark,Long executorId,String executorName,
    LocalDateTime executedAt) {}
