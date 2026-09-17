package com.cq.maintenance.statistics.vo;

import com.cq.maintenance.statistics.dto.StatisticsRange;
import java.time.LocalDate;

public record StatisticsPeriodVO(StatisticsRange range, LocalDate startDate, LocalDate endDate) {}
