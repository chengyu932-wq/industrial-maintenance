package com.cq.maintenance.inventory.vo;

import java.time.LocalDateTime;
import java.util.List;

public record WarehouseVO(Long id,String warehouseNo,String warehouseName,String location,Long managerId,
    String managerName,String status,LocalDateTime createdAt,LocalDateTime updatedAt,List<Long> authorizedUserIds) {
    public WarehouseVO { authorizedUserIds=authorizedUserIds==null?List.of():List.copyOf(authorizedUserIds); }
}
