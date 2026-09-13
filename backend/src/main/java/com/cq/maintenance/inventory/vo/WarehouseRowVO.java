package com.cq.maintenance.inventory.vo;

import java.time.LocalDateTime;

public record WarehouseRowVO(Long id,String warehouseNo,String warehouseName,String location,Long managerId,
    String managerName,String status,LocalDateTime createdAt,LocalDateTime updatedAt) {}
