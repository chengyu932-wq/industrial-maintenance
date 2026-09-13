package com.cq.maintenance.inventory.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WarningVO(Long id,Long warehouseId,String warehouseName,Long sparePartId,String spareNo,
    String spareName,BigDecimal thresholdQty,BigDecimal currentQty,String status,LocalDateTime triggeredAt,
    LocalDateTime closedAt) {}
