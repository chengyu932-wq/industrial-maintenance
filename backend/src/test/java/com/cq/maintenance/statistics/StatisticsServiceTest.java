package com.cq.maintenance.statistics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.cq.maintenance.common.exception.BusinessException;
import com.cq.maintenance.security.LoginUser;
import com.cq.maintenance.statistics.dto.*;
import com.cq.maintenance.statistics.mapper.StatisticsMapper;
import com.cq.maintenance.statistics.mapper.StatisticsMapper.*;
import com.cq.maintenance.statistics.service.StatisticsService;
import com.cq.maintenance.statistics.vo.KpiMetricVO;
import com.cq.maintenance.workorder.mapper.WorkOrderMapper.WorkOrderDataScope;
import com.cq.maintenance.workorder.service.WorkOrderScopeService;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class StatisticsServiceTest {
    StatisticsMapper mapper; WorkOrderScopeService scopes; StatisticsService service;
    WorkOrderDataScope allScope = new WorkOrderDataScope(true,"ALL",1L,null,List.of());

    @BeforeEach void setUp() {
        mapper=mock(StatisticsMapper.class);scopes=mock(WorkOrderScopeService.class);service=new StatisticsService(mapper,scopes);
        when(scopes.current()).thenReturn(allScope);login("ADMIN",List.of());
        when(mapper.runtimeHours(any(),any(),any())).thenReturn(BigDecimal.ZERO);
        when(mapper.repairAggregate(any(),any(),any())).thenReturn(new RepairAggregate(0,0,0,0,0,0));
        when(mapper.availability(any(),any(),any())).thenReturn(new AvailabilityAggregate(0,0));
        when(mapper.inventoryTurnover(anyBoolean(),anyList(),any(),any())).thenReturn(new InventoryAggregate(BigDecimal.ZERO,BigDecimal.ZERO,0));
        when(mapper.equipmentStatus(any())).thenReturn(List.of());when(mapper.workOrderStatus(any(),any(),any())).thenReturn(List.of());
        when(mapper.faultEquipmentTypes(any(),any(),any())).thenReturn(List.of());when(mapper.repairTrend(any(),any(),any())).thenReturn(List.of());
        when(mapper.spareConsumption(anyBoolean(),anyList(),any(),any())).thenReturn(List.of());when(mapper.slaCompletion(any(),any(),any())).thenReturn(List.of());
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    void login(String role,List<Long> warehouses){LoginUser u=new LoginUser(1L,"u","用户","ENABLED",List.of(role),List.of("statistics:view"),List.of(8L),4L,warehouses);SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(u,null,List.of()));}
    StatisticsQuery custom(String start,String end){StatisticsQuery q=new StatisticsQuery();q.setRange(StatisticsRange.CUSTOM);q.setStartDate(LocalDate.parse(start));q.setEndDate(LocalDate.parse(end));return q;}
    KpiMetricVO metric(List<KpiMetricVO> values,String code){return values.stream().filter(v->code.equals(v.code())).findFirst().orElseThrow();}

    @Test void overviewUsesScopedRealCounts(){when(mapper.countEquipment(allScope,null)).thenReturn(12L);when(mapper.countEquipment(allScope,"RUNNING")).thenReturn(8L);when(mapper.countEquipment(allScope,"REPAIRING")).thenReturn(2L);when(mapper.countPendingWorkOrders(allScope)).thenReturn(3L);when(mapper.countCompletedRepairs(eq(allScope),any(),any())).thenReturn(5L);when(mapper.countOpenWarnings(true,List.of(-1L))).thenReturn(1L);var result=service.overview(custom("2026-09-01","2026-09-07"));assertEquals(12,result.equipmentTotal());assertEquals(5,result.completedRepairOrders());assertEquals(1,result.openStockWarnings());}
    @Test void customRangeIsConvertedToInclusiveDatesAndExclusiveEnd(){service.overview(custom("2026-09-01","2026-09-07"));ArgumentCaptor<LocalDateTime> start=ArgumentCaptor.forClass(LocalDateTime.class),end=ArgumentCaptor.forClass(LocalDateTime.class);verify(mapper).countCompletedRepairs(eq(allScope),start.capture(),end.capture());assertEquals(LocalDateTime.of(2026,9,1,0,0),start.getValue());assertEquals(LocalDateTime.of(2026,9,8,0,0),end.getValue());}
    @Test void rejectsIncompleteCustomRange(){StatisticsQuery q=new StatisticsQuery();q.setRange(StatisticsRange.CUSTOM);q.setStartDate(LocalDate.now());assertThrows(BusinessException.class,()->service.overview(q));}
    @Test void rejectsReversedRange(){assertThrows(BusinessException.class,()->service.overview(custom("2026-09-08","2026-09-01")));}
    @Test void rejectsRangeLongerThanOneYear(){assertThrows(BusinessException.class,()->service.overview(custom("2024-01-01","2026-01-02")));}
    @Test void calculatesAvailableKpisFromMapperAggregates(){when(mapper.countCompletedRepairs(eq(allScope),any(),any())).thenReturn(4L);when(mapper.runtimeHours(eq(allScope),any(),any())).thenReturn(new BigDecimal("120"));when(mapper.repairAggregate(eq(allScope),any(),any())).thenReturn(new RepairAggregate(2,14400,4,3,4,3));when(mapper.availability(eq(allScope),any(),any())).thenReturn(new AvailabilityAggregate(80,100));when(mapper.inventoryTurnover(eq(true),anyList(),any(),any())).thenReturn(new InventoryAggregate(new BigDecimal("20"),new BigDecimal("10"),2));var values=service.kpis(custom("2026-09-01","2026-09-07")).metrics();assertEquals(new BigDecimal("30.00"),metric(values,"MTBF").value());assertEquals(new BigDecimal("2.00"),metric(values,"MTTR").value());assertEquals(new BigDecimal("75.00"),metric(values,"ON_TIME_CLOSE_RATE").value());assertEquals(new BigDecimal("75.00"),metric(values,"FIRST_TIME_FIX_RATE").value());assertEquals(new BigDecimal("80.00"),metric(values,"EQUIPMENT_AVAILABILITY").value());assertEquals(new BigDecimal("2.00"),metric(values,"SPARE_PART_TURNOVER").value());}
    @Test void emptyDataReturnsUnavailableInsteadOfInventedZero(){var values=service.kpis(custom("2026-09-01","2026-09-07")).metrics();assertFalse(metric(values,"MTBF").available());assertNull(metric(values,"MTBF").value());assertFalse(metric(values,"MTTR").available());assertFalse(metric(values,"ON_TIME_CLOSE_RATE").available());assertFalse(metric(values,"EQUIPMENT_AVAILABILITY").available());assertFalse(metric(values,"SPARE_PART_TURNOVER").available());}
    @Test void firstTimeFixIsUnavailableWithoutCompletedRepair(){var value=metric(service.kpis(custom("2026-09-01","2026-09-07")).metrics(),"FIRST_TIME_FIX_RATE");assertFalse(value.available());assertNull(value.value());assertTrue(value.note().contains("无已完成维修工单"));}
    @Test void invalidRepairTimestampSamplesAreExcludedByAggregateSample(){when(mapper.repairAggregate(eq(allScope),any(),any())).thenReturn(new RepairAggregate(0,0,0,0,2,1));var values=service.kpis(custom("2026-09-01","2026-09-07")).metrics();assertFalse(metric(values,"MTTR").available());assertEquals(0,metric(values,"MTTR").sampleSize());assertEquals(new BigDecimal("50.00"),metric(values,"ON_TIME_CLOSE_RATE").value());}
    @Test void chartTrendFillsMissingDatesOnlyWithZero(){when(mapper.repairTrend(eq(allScope),any(),any())).thenReturn(List.of(new DateValueRow(LocalDate.of(2026,9,2),2)));var result=service.charts(custom("2026-09-01","2026-09-03"));assertEquals(3,result.repairTrend().size());assertEquals(0,result.repairTrend().get(0).value());assertEquals(2,result.repairTrend().get(1).value());assertEquals(0,result.repairTrend().get(2).value());}
    @Test void warehouseAdministratorOnlyUsesAuthorizedWarehouses(){login("WAREHOUSE_ADMIN",List.of(3L,5L));when(scopes.current()).thenReturn(new WorkOrderDataScope(false,"NONE",1L,null,List.of()));service.kpis(custom("2026-09-01","2026-09-07"));verify(mapper).inventoryTurnover(false,List.of(3L,5L),LocalDateTime.of(2026,9,1,0,0),LocalDateTime.of(2026,9,8,0,0));}
}
