package com.cq.maintenance.organization.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("org_station")
public class Station {
    private Long id;
    private Long lineId;
    private String stationNo;
    private String stationName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
