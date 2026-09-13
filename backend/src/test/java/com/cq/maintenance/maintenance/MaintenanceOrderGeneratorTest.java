package com.cq.maintenance.maintenance;

import static org.junit.jupiter.api.Assertions.*;import static org.mockito.ArgumentMatchers.*;import static org.mockito.Mockito.*;
import com.cq.maintenance.common.exception.BusinessException;import com.cq.maintenance.equipment.entity.EquipmentStatus;import com.cq.maintenance.maintenance.entity.*;import com.cq.maintenance.maintenance.mapper.MaintenanceMapper;import com.cq.maintenance.maintenance.service.*;import com.cq.maintenance.maintenance.vo.MaintenancePlanItemVO;import com.cq.maintenance.workorder.entity.*;import com.cq.maintenance.workorder.mapper.WorkOrderMapper;import com.cq.maintenance.workorder.service.*;import java.time.LocalDate;import java.util.List;import org.junit.jupiter.api.*;

class MaintenanceOrderGeneratorTest {
    MaintenanceMapper mapper;WorkOrderMapper orders;WorkOrderStateService states;MaintenanceOrderGenerator service;LocalDate today=LocalDate.of(2026,9,13);
    @BeforeEach void setUp(){mapper=mock(MaintenanceMapper.class);orders=mock(WorkOrderMapper.class);states=mock(WorkOrderStateService.class);BusinessNumberService numbers=mock(BusinessNumberService.class);when(numbers.next("PM")).thenReturn("PM20260913");service=new MaintenanceOrderGenerator(mapper,orders,states,numbers,new MaintenanceScheduleService());doAnswer(x->{WorkOrder o=x.getArgument(0);o.setId(88L);return 1;}).when(orders).insertWorkOrder(any());}
    MaintenancePlan due(){MaintenancePlan p=new MaintenancePlan();p.setId(1L);p.setPlanNo("PM-1");p.setEquipmentId(5L);p.setCycleType(MaintenanceCycleType.MONTH);p.setCycleValue(1);p.setNextExecuteDate(today);p.setStatus(MaintenancePlanStatus.ENABLED);when(mapper.lockPlan(1L)).thenReturn(p);when(mapper.findEquipmentStatus(5L)).thenReturn(EquipmentStatus.RUNNING);when(mapper.findPlanItems(1L)).thenReturn(List.of(new MaintenancePlanItemVO(2L,1L,"润滑",null,1,true)));when(mapper.findSystemOperatorId()).thenReturn(9L);when(mapper.advancePlan(eq(1L),eq(today),any())).thenReturn(1);return p;}
    @Test void duePlanGeneratesMaintenanceOrder(){due();assertTrue(service.generateIfDue(1L,today));verify(orders).insertWorkOrder(argThat(o->o.getWorkOrderType()==WorkOrderType.MAINTENANCE&&o.getPmPlanId().equals(1L)));}
    @Test void generationWritesInitialFlow(){due();service.generateIfDue(1L,today);verify(states).created(argThat(o->o.getId().equals(88L)),eq(9L),contains("PM-1"));}
    @Test void generationAdvancesNextDate(){due();service.generateIfDue(1L,today);verify(mapper).advancePlan(1L,today,LocalDate.of(2026,10,13));}
    @Test void futurePlanDoesNotGenerate(){MaintenancePlan p=due();p.setNextExecuteDate(today.plusDays(1));assertFalse(service.generateIfDue(1L,today));verifyNoInteractions(orders);}
    @Test void disabledPlanDoesNotGenerate(){MaintenancePlan p=due();p.setStatus(MaintenancePlanStatus.DISABLED);assertFalse(service.generateIfDue(1L,today));verifyNoInteractions(orders);}
    @Test void scrappedEquipmentDisablesPlan(){due();when(mapper.findEquipmentStatus(5L)).thenReturn(EquipmentStatus.SCRAPPED);assertFalse(service.generateIfDue(1L,today));verify(mapper).disablePlan(1L);verifyNoInteractions(orders);}
    @Test void planWithoutItemsFailsWithoutOrder(){due();when(mapper.findPlanItems(1L)).thenReturn(List.of());assertThrows(BusinessException.class,()->service.generateIfDue(1L,today));verifyNoInteractions(orders);}
}
