package com.cq.maintenance.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("inv_warehouse")
public class Warehouse {
    private Long id;
    private String warehouseNo;
    private String warehouseName;
    private String location;
    private Long managerId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
