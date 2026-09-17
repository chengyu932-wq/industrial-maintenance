package com.cq.maintenance.statistics.vo;

import java.math.BigDecimal;

public record KpiMetricVO(String code, String name, BigDecimal value, String unit,
                          boolean available, long sampleSize, String formula,
                          String dataSource, String note) {}
