package com.cq.maintenance.inventory.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionVO(Long id,String transactionNo,Long warehouseId,String warehouseName,Long sparePartId,
    String spareNo,String spareName,String transactionType,BigDecimal qtyChange,BigDecimal qtyBefore,
    BigDecimal qtyAfter,BigDecimal unitPrice,Long workOrderId,String workOrderNo,Long equipmentId,
    String equipmentNo,Long relatedTransactionId,Long operatorId,String operatorName,String remark,
    LocalDateTime createdAt) {}
