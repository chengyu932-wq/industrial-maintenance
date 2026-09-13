package com.cq.maintenance.inventory.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class WorkOrderSpare {
    private Long id;
    private Long workOrderId;
    private Long warehouseId;
    private Long sparePartId;
    private BigDecimal qty;
    private BigDecimal unitPriceSnapshot;
    private Long transactionId;
    private String status;
    private Long issuedBy;
    private LocalDateTime issuedAt;
}
