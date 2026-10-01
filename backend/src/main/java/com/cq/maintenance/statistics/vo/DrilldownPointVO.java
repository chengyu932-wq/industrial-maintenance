package com.cq.maintenance.statistics.vo;

import java.math.BigDecimal;

public record DrilldownPointVO(Long id, String name, long completedCount, BigDecimal averageRepairHours) {}
