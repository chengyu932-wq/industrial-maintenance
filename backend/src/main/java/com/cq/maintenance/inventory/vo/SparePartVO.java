package com.cq.maintenance.inventory.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SparePartVO(Long id,String spareNo,String spareName,String specification,String brand,String unit,
    BigDecimal unitPrice,Long supplierId,String supplierName,String compatibleModel,Integer leadTimeDays,
    Integer safetyDays,String status,LocalDateTime createdAt,LocalDateTime updatedAt) {}
