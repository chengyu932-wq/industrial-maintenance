package com.cq.maintenance.inventory.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WorkOrderSpareVO(Long id,Long workOrderId,Long warehouseId,String warehouseNo,String warehouseName,
    Long sparePartId,String spareNo,String spareName,String specification,String unit,BigDecimal issuedQty,
    BigDecimal returnedQty,BigDecimal usedQty,BigDecimal unitPriceSnapshot,String status,Long issuedBy,
    String issuedByName,LocalDateTime issuedAt) {}
