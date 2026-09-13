package com.cq.maintenance.inventory.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper=true)
public class WarehouseQuery extends PagedQuery {
    private String keyword;
    private String status;
}
