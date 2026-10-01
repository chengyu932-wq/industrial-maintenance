package com.cq.maintenance.workorder.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EquipmentSpareUsageVO(Long id,Long workOrderId,String workOrderNo,String spareNo,String spareName,
    String unit,BigDecimal issuedQty,BigDecimal returnedQty,BigDecimal usedQty,LocalDateTime issuedAt) {}
