package com.cq.maintenance.inventory.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper=true)
public class StockQuery extends PagedQuery {
    private Long warehouseId;
    private Long sparePartId;
    private String keyword;
    private Boolean lowStockOnly;
}
