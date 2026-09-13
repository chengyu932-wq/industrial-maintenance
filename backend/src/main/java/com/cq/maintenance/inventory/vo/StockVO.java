package com.cq.maintenance.inventory.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockVO(Long id,Long warehouseId,String warehouseNo,String warehouseName,Long sparePartId,
    String spareNo,String spareName,String specification,String unit,BigDecimal currentQty,
    BigDecimal suggestedSafetyStock,BigDecimal reorderPoint,String stockStatus,LocalDateTime updatedAt) {}
