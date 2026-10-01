package com.cq.maintenance.statistics.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class StatisticsQuery {
    private StatisticsRange range = StatisticsRange.LAST_30_DAYS;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
    @Positive private Long workshopId;
    @Positive private Long equipmentTypeId;
    @Positive private Long engineerId;
}
