package com.cq.maintenance.maintenance;

import static org.junit.jupiter.api.Assertions.*;import static org.mockito.ArgumentMatchers.*;import static org.mockito.Mockito.*;
import com.cq.maintenance.common.exception.BusinessException;import com.cq.maintenance.equipment.entity.*;import com.cq.maintenance.equipment.service.*;import com.cq.maintenance.maintenance.dto.*;import com.cq.maintenance.maintenance.entity.*;import com.cq.maintenance.maintenance.mapper.MaintenanceMapper;import com.cq.maintenance.maintenance.service.MaintenancePlanService;import java.time.LocalDate;import java.util.List;import org.junit.jupiter.api.*;

class MaintenancePlanServiceTest {
    MaintenanceMapper mapper;EquipmentService equipment;EquipmentScopeService scopes;MaintenancePlanService service;
    @BeforeEach void setUp(){mapper=mock(MaintenanceMapper.class);equipment=mock(EquipmentService.class);scopes=mock(EquipmentScopeService.class);service=new MaintenancePlanService(mapper,equipment,scopes);Equipment e=new Equipment();e.setId(1L);e.setStatus(EquipmentStatus.RUNNING);when(equipment.required(1L)).thenReturn(e);doAnswer(x->{MaintenancePlan p=x.getArgument(0);p.setId(9L);return 1;}).when(mapper).insertPlan(any());}
    MaintenancePlanRequest request(String no,MaintenanceCycleType type){return new MaintenancePlanRequest(no,"数控机床保养",1L,type,1,LocalDate.now().plusDays(1),null);}
    MaintenancePlan plan(MaintenancePlanStatus status){MaintenancePlan p=new MaintenancePlan();p.setId(9L);p.setEquipmentId(1L);p.setStatus(status);when(mapper.findPlan(9L)).thenReturn(p);return p;}
    @Test void createsWeeklyPlan(){assertEquals(9L,service.create(request("PM-W",MaintenanceCycleType.WEEK)));verify(mapper).insertPlan(argThat(p->p.getCycleType()==MaintenanceCycleType.WEEK));}
    @Test void createsMonthlyPlan(){assertEquals(9L,service.create(request("PM-M",MaintenanceCycleType.MONTH)));verify(mapper).insertPlan(argThat(p->p.getCycleType()==MaintenanceCycleType.MONTH));}
    @Test void createsQuarterlyPlan(){assertEquals(9L,service.create(request("PM-Q",MaintenanceCycleType.QUARTER)));verify(mapper).insertPlan(argThat(p->p.getCycleType()==MaintenanceCycleType.QUARTER));}
    @Test void duplicatePlanNumberIsRejected(){when(mapper.countPlanNo("PM-X",null)).thenReturn(1L);assertThrows(BusinessException.class,()->service.create(request("PM-X",MaintenanceCycleType.MONTH)));}
    @Test void scrappedEquipmentIsRejected(){Equipment e=new Equipment();e.setStatus(EquipmentStatus.SCRAPPED);when(equipment.required(1L)).thenReturn(e);assertThrows(BusinessException.class,()->service.create(request("PM-X",MaintenanceCycleType.MONTH)));}
    @Test void pastExecutionDateIsRejected(){var r=new MaintenancePlanRequest("PM-X","计划",1L,MaintenanceCycleType.MONTH,1,LocalDate.now().minusDays(1),null);assertThrows(BusinessException.class,()->service.create(r));}
    @Test void enabledPlanCanBeDisabled(){plan(MaintenancePlanStatus.ENABLED);service.setStatus(9L,MaintenancePlanStatus.DISABLED);verify(mapper).updatePlanStatus(9L,MaintenancePlanStatus.DISABLED);}
    @Test void duplicateStatusChangeIsRejected(){plan(MaintenancePlanStatus.DISABLED);assertThrows(BusinessException.class,()->service.setStatus(9L,MaintenancePlanStatus.DISABLED));}
    @Test void inspectionItemsAreReplacedInOrder(){plan(MaintenancePlanStatus.ENABLED);service.replaceItems(9L,List.of(new MaintenancePlanItemRequest("润滑检查","油位正常",8,true),new MaintenancePlanItemRequest("护罩检查",null,3,false)));verify(mapper).deletePlanItems(9L);verify(mapper,times(2)).insertPlanItem(any());}
    @Test void generatedPlanItemsAreImmutable(){plan(MaintenancePlanStatus.ENABLED);when(mapper.countGeneratedOrders(9L)).thenReturn(1L);assertThrows(BusinessException.class,()->service.replaceItems(9L,List.of(new MaintenancePlanItemRequest("项目",null,1,true))));verify(mapper,never()).deletePlanItems(anyLong());}
}
