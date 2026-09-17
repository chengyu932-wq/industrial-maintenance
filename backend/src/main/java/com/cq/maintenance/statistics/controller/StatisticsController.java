package com.cq.maintenance.statistics.controller;

import com.cq.maintenance.common.response.ApiResponse;
import com.cq.maintenance.statistics.dto.StatisticsQuery;
import com.cq.maintenance.statistics.service.StatisticsService;
import com.cq.maintenance.statistics.vo.*;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@ConditionalOnExpression("'${spring.autoconfigure.exclude:}'.indexOf('DataSourceAutoConfiguration') < 0")
@RequestMapping("/api/statistics")
public class StatisticsController {
    private final StatisticsService service;
    public StatisticsController(StatisticsService service) { this.service = service; }

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('statistics:view')")
    public ApiResponse<StatisticsOverviewVO> overview(@Valid StatisticsQuery query) {
        return ApiResponse.success(service.overview(query));
    }

    @GetMapping("/kpis")
    @PreAuthorize("hasAuthority('statistics:view')")
    public ApiResponse<StatisticsKpiVO> kpis(@Valid StatisticsQuery query) {
        return ApiResponse.success(service.kpis(query));
    }

    @GetMapping("/charts")
    @PreAuthorize("hasAuthority('statistics:view')")
    public ApiResponse<StatisticsChartsVO> charts(@Valid StatisticsQuery query) {
        return ApiResponse.success(service.charts(query));
    }
}
