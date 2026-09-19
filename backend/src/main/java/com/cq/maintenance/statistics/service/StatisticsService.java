package com.cq.maintenance.statistics.service;

import com.cq.maintenance.common.exception.BusinessException;
import com.cq.maintenance.common.exception.ErrorCode;
import com.cq.maintenance.security.LoginUser;
import com.cq.maintenance.security.SecurityUtils;
import com.cq.maintenance.statistics.dto.*;
import com.cq.maintenance.statistics.mapper.StatisticsMapper;
import com.cq.maintenance.statistics.mapper.StatisticsMapper.*;
import com.cq.maintenance.statistics.vo.*;
import com.cq.maintenance.workorder.mapper.WorkOrderMapper.WorkOrderDataScope;
import com.cq.maintenance.workorder.service.WorkOrderScopeService;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnExpression("'${spring.autoconfigure.exclude:}'.indexOf('DataSourceAutoConfiguration') < 0")
@Transactional(readOnly = true)
public class StatisticsService {
    private static final int SCALE = 2;
    private final StatisticsMapper mapper;
    private final WorkOrderScopeService workOrderScopes;

    public StatisticsService(StatisticsMapper mapper, WorkOrderScopeService workOrderScopes) {
        this.mapper = mapper;
        this.workOrderScopes = workOrderScopes;
    }

    public StatisticsOverviewVO overview(StatisticsQuery query) {
        Period period = period(query);
        WorkOrderDataScope scope = workOrderScopes.current();
        WarehouseScope warehouses = warehouses();
        return new StatisticsOverviewVO(period.vo(), mapper.countEquipment(scope, null),
            mapper.countEquipment(scope, "RUNNING"), mapper.countEquipment(scope, "REPAIRING"),
            mapper.countPendingWorkOrders(scope), mapper.countCompletedRepairs(scope, period.start(), period.end()),
            mapper.countOpenWarnings(warehouses.all(), warehouses.ids()));
    }

    public StatisticsKpiVO kpis(StatisticsQuery query) {
        Period period = period(query);
        WorkOrderDataScope scope = workOrderScopes.current();
        WarehouseScope warehouses = warehouses();
        long faultCount = mapper.countCompletedRepairs(scope, period.start(), period.end());
        BigDecimal runtime = zero(mapper.runtimeHours(scope, period.start(), period.end()));
        RepairAggregate repair = mapper.repairAggregate(scope, period.start(), period.end());
        AvailabilityAggregate availability = mapper.availability(scope, period.start(), period.end());
        InventoryAggregate inventory = mapper.inventoryTurnover(warehouses.all(), warehouses.ids(), period.start(), period.end());

        List<KpiMetricVO> metrics = new ArrayList<>();
        boolean mtbfAvailable = faultCount > 0 && runtime.signum() > 0;
        metrics.add(metric("MTBF", "平均故障间隔时间", mtbfAvailable ? divide(runtime, BigDecimal.valueOf(faultCount)) : null,
            "小时", mtbfAvailable, faultCount, "运行小时增量合计 / 完成维修工单数",
            "eqp_runtime_record + mnt_work_order", mtbfAvailable ? "按真实运行小时记录计算" : faultCount == 0 ? "统计期内无已完成维修故障" : "统计期内缺少运行小时记录"));

        boolean mttrAvailable = repair != null && repair.repairCount() > 0;
        metrics.add(metric("MTTR", "平均修复时间", mttrAvailable ? divide(BigDecimal.valueOf(repair.repairSeconds()), BigDecimal.valueOf(repair.repairCount() * 3600L)) : null,
            "小时", mttrAvailable, repair == null ? 0 : repair.repairCount(), "有效维修总秒数 / 有效维修次数",
            "mnt_work_order.started_at + completed_at", mttrAvailable ? "仅统计起止时间完整且顺序有效的已完成维修工单" : "统计期内无有效维修时长样本"));

        boolean slaAvailable = repair != null && repair.slaCount() > 0;
        metrics.add(metric("ON_TIME_CLOSE_RATE", "工单按时关闭率", slaAvailable ? percent(repair.onTimeCount(), repair.slaCount()) : null,
            "%", slaAvailable, repair == null ? 0 : repair.slaCount(), "按时完成且有 SLA 的维修工单数 / 有 SLA 的已完成维修工单数 × 100%",
            "mnt_work_order.completed_at + sla_resolve_deadline", slaAvailable ? "分母仅包含具有解决截止时间的已完成维修工单" : "统计期内无已完成 SLA 样本"));

        boolean firstTimeAvailable = repair != null && repair.completedCount() > 0;
        metrics.add(metric("FIRST_TIME_FIX_RATE", "一次修复率（首次验收通过）",
            firstTimeAvailable ? percent(repair.firstTimeCount(), repair.completedCount()) : null,
            "%", firstTimeAvailable, repair == null ? 0 : repair.completedCount(),
            "验收退回次数为 0 的已完成维修工单数 / 已完成维修工单数 × 100%",
            "mnt_work_order.acceptance_return_count + completed_at",
            firstTimeAvailable ? "按首次提交是否直接验收通过计算；不等同于 7 天同类故障复发率" : "统计期内无已完成维修工单"));

        boolean availabilityReady = availability != null && availability.observedSeconds() > 0;
        metrics.add(metric("EQUIPMENT_AVAILABILITY", "设备综合可用率",
            availabilityReady ? percent(availability.runningSeconds(), availability.observedSeconds()) : null,
            "%", availabilityReady, availability == null ? 0 : availability.observedSeconds() / 60,
            "RUNNING 状态时长 / 已观测状态时长 × 100%", "eqp_status_log",
            availabilityReady ? "样本数表示状态履历覆盖分钟数；不推算无履历时段" : "统计期内没有可计算的设备状态履历"));

        BigDecimal averageQty = inventory == null ? BigDecimal.ZERO : zero(inventory.averageQty());
        boolean turnoverReady = averageQty.signum() > 0;
        metrics.add(metric("SPARE_PART_TURNOVER", "备件周转率",
            turnoverReady ? divide(zero(inventory.consumedQty()), averageQty) : null,
            "次", turnoverReady, inventory == null ? 0 : inventory.stockItemCount(),
            "净消耗数量 / ((期初库存 + 期末库存) / 2)", "inv_transaction + inv_stock",
            turnoverReady ? "净消耗包含 OUTBOUND、ISSUE 并扣除 RETURN，排除调拨、盘点和报废" : "授权仓库的平均库存为 0 或没有库存样本"));
        return new StatisticsKpiVO(period.vo(), List.copyOf(metrics));
    }

    public StatisticsChartsVO charts(StatisticsQuery query) {
        Period period = period(query);
        WorkOrderDataScope scope = workOrderScopes.current();
        WarehouseScope warehouses = warehouses();
        Map<LocalDate, Long> trend = mapper.repairTrend(scope, period.start(), period.end()).stream()
            .collect(Collectors.toMap(DateValueRow::day, DateValueRow::value));
        List<TrendPointVO> completedTrend = period.start().toLocalDate().datesUntil(period.end().toLocalDate())
            .map(day -> new TrendPointVO(day, trend.getOrDefault(day, 0L))).toList();
        return new StatisticsChartsVO(period.vo(), points(mapper.equipmentStatus(scope)),
            points(mapper.workOrderStatus(scope, period.start(), period.end())),
            points(mapper.faultEquipmentTypes(scope, period.start(), period.end())), completedTrend,
            points(mapper.spareConsumption(warehouses.all(), warehouses.ids(), period.start(), period.end())),
            points(mapper.slaCompletion(scope, period.start(), period.end())));
    }

    private Period period(StatisticsQuery query) {
        StatisticsRange range = query.getRange() == null ? StatisticsRange.LAST_30_DAYS : query.getRange();
        LocalDate today = LocalDate.now();
        LocalDate start;
        LocalDate end;
        if (range == StatisticsRange.CUSTOM) {
            start = query.getStartDate(); end = query.getEndDate();
            if (start == null || end == null) throw new BusinessException(ErrorCode.PARAMETER_ERROR, "自定义统计范围必须同时提供开始和结束日期");
        } else {
            end = today;
            int days = range == StatisticsRange.LAST_7_DAYS ? 7 : range == StatisticsRange.LAST_90_DAYS ? 90 : 30;
            start = end.minusDays(days - 1L);
        }
        if (start.isAfter(end)) throw new BusinessException(ErrorCode.PARAMETER_ERROR, "统计开始日期不能晚于结束日期");
        if (start.plusYears(1).isBefore(end)) throw new BusinessException(ErrorCode.PARAMETER_ERROR, "单次统计范围不能超过一年");
        return new Period(range, start.atStartOfDay(), end.plusDays(1).atStartOfDay());
    }

    private WarehouseScope warehouses() {
        LoginUser user = SecurityUtils.currentUser();
        boolean all = user.roleCodes().contains("ADMIN");
        List<Long> ids = all ? List.of(-1L) : user.authorizedWarehouseIds().isEmpty() ? List.of(-1L) : user.authorizedWarehouseIds();
        return new WarehouseScope(all, ids);
    }

    private static List<ChartPointVO> points(List<NameValueRow> rows) {
        return rows.stream().map(row -> new ChartPointVO(row.name(), zero(row.value()))).toList();
    }
    private static KpiMetricVO metric(String code,String name,BigDecimal value,String unit,boolean available,long sampleSize,String formula,String source,String note) {
        return new KpiMetricVO(code,name,value,unit,available,sampleSize,formula,source,note);
    }
    private static BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private static BigDecimal divide(BigDecimal left, BigDecimal right) { return left.divide(right, SCALE, RoundingMode.HALF_UP); }
    private static BigDecimal percent(long part, long total) { return divide(BigDecimal.valueOf(part * 100L), BigDecimal.valueOf(total)); }
    private record WarehouseScope(boolean all,List<Long> ids) {}
    private record Period(StatisticsRange range,LocalDateTime start,LocalDateTime end) {
        StatisticsPeriodVO vo() { return new StatisticsPeriodVO(range,start.toLocalDate(),end.minusDays(1).toLocalDate()); }
    }
}
