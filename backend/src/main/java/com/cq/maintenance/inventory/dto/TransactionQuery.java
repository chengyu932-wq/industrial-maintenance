package com.cq.maintenance.inventory.dto;

import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper=true)
public class TransactionQuery extends PagedQuery {
    private Long warehouseId;
    private Long sparePartId;
    private String transactionType;
    private Long workOrderId;
    private LocalDate startDate;
    private LocalDate endDate;
}
