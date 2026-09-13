package com.cq.maintenance.inventory.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class InventoryTransaction {
    private Long id;
    private String transactionNo;
    private Long warehouseId;
    private Long sparePartId;
    private String transactionType;
    private BigDecimal qtyChange;
    private BigDecimal qtyBefore;
    private BigDecimal qtyAfter;
    private BigDecimal unitPrice;
    private Long workOrderId;
    private Long equipmentId;
    private Long relatedTransactionId;
    private Long operatorId;
    private String remark;
    private LocalDateTime createdAt;
}
