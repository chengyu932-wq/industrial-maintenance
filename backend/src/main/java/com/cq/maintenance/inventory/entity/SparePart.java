package com.cq.maintenance.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("inv_spare_part")
public class SparePart {
    private Long id;
    private String spareNo;
    private String spareName;
    private String specification;
    private String brand;
    private String unit;
    private BigDecimal unitPrice;
    private Long supplierId;
    private String compatibleModel;
    private Integer leadTimeDays;
    private Integer safetyDays;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
