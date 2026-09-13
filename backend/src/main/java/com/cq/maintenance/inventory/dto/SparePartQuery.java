package com.cq.maintenance.inventory.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper=true)
public class SparePartQuery extends PagedQuery {
    private String keyword;
    private String status;
}
