package com.cq.maintenance.inventory.vo;

import java.math.BigDecimal;

public record SafetyStockVO(Long warehouseId,Long sparePartId,BigDecimal dailyConsumption,
    BigDecimal suggestedSafetyStock,BigDecimal reorderPoint,int periodDays) {}
