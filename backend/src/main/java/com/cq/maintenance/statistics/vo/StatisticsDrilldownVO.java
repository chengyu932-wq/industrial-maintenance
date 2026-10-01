package com.cq.maintenance.statistics.vo;

import java.util.List;

public record StatisticsDrilldownVO(StatisticsPeriodVO period, List<DrilldownPointVO> workshops,
    List<DrilldownPointVO> equipmentTypes, List<DrilldownPointVO> engineers) {}
