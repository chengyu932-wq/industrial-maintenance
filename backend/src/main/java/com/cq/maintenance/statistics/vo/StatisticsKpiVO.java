package com.cq.maintenance.statistics.vo;

import java.util.List;

public record StatisticsKpiVO(StatisticsPeriodVO period, List<KpiMetricVO> metrics) {}
