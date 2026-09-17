package com.cq.maintenance.statistics.vo;

import java.util.List;

public record StatisticsChartsVO(StatisticsPeriodVO period,
        List<ChartPointVO> equipmentStatus,
        List<ChartPointVO> workOrderStatus,
        List<ChartPointVO> faultEquipmentTypes,
        List<TrendPointVO> repairTrend,
        List<ChartPointVO> spareConsumption,
        List<ChartPointVO> slaCompletion) {}
