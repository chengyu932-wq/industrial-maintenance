package com.cq.maintenance.statistics.vo;

public record StatisticsOverviewVO(StatisticsPeriodVO period, long equipmentTotal,
        long runningEquipment, long repairingEquipment, long pendingWorkOrders,
        long completedRepairOrders, long openStockWarnings) {}
