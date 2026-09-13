package com.cq.maintenance.maintenance;

import static org.junit.jupiter.api.Assertions.*;import static org.mockito.ArgumentMatchers.*;import static org.mockito.Mockito.*;
import com.cq.maintenance.common.exception.BusinessException;import com.cq.maintenance.maintenance.dto.MaintenanceExecutionItemRequest;import com.cq.maintenance.maintenance.entity.MaintenanceResult;import com.cq.maintenance.maintenance.mapper.MaintenanceMapper;import com.cq.maintenance.maintenance.service.MaintenanceExecutionService;import com.cq.maintenance.security.LoginUser;import com.cq.maintenance.workorder.entity.*;import com.cq.maintenance.workorder.mapper.WorkOrderMapper;import com.cq.maintenance.workorder.service.WorkOrderScopeService;import java.util.List;import org.junit.jupiter.api.*;import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;import org.springframework.security.core.context.SecurityContextHolder;

class MaintenanceExecutionServiceTest {
    MaintenanceMapper mapper;WorkOrderMapper orders;WorkOrderScopeService scopes;MaintenanceExecutionService service;
    @BeforeEach void setUp(){mapper=mock(MaintenanceMapper.class);orders=mock(WorkOrderMapper.class);scopes=mock(WorkOrderScopeService.class);service=new MaintenanceExecutionService(mapper,orders,scopes);LoginUser u=new LoginUser(7L,"engineer","工程师","ENABLED",List.of("ENGINEER"),List.of(),List.of(3L),4L,List.of());SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(u,null,List.of()));}
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    WorkOrder order(WorkOrderType type,WorkOrderStatus status){WorkOrder o=new WorkOrder();o.setId(1L);o.setPmPlanId(2L);o.setWorkOrderType(type);o.setStatus(status);o.setAssignedEngineerId(7L);when(orders.lockWorkOrder(1L)).thenReturn(o);when(orders.findWorkOrder(1L)).thenReturn(o);return o;}
    MaintenanceExecutionItemRequest result(Long id){return new MaintenanceExecutionItemRequest(id,MaintenanceResult.NORMAL,"12.4","正常");}
    @Test void assignedEngineerCanSaveResult(){order(WorkOrderType.MAINTENANCE,WorkOrderStatus.PROCESSING);when(mapper.countPlanItem(2L,3L)).thenReturn(1L);service.save(1L,List.of(result(3L)));verify(mapper).upsertExecution(1L,3L,MaintenanceResult.NORMAL,"12.4","正常",7L);}
    @Test void nonMaintenanceOrderIsRejected(){order(WorkOrderType.REPAIR,WorkOrderStatus.PROCESSING);assertThrows(BusinessException.class,()->service.save(1L,List.of(result(3L))));}
    @Test void nonProcessingOrderIsRejected(){order(WorkOrderType.MAINTENANCE,WorkOrderStatus.ASSIGNED);assertThrows(BusinessException.class,()->service.save(1L,List.of(result(3L))));}
    @Test void foreignPlanItemIsRejected(){order(WorkOrderType.MAINTENANCE,WorkOrderStatus.PROCESSING);assertThrows(BusinessException.class,()->service.save(1L,List.of(result(99L))));}
    @Test void duplicateItemsInRequestAreRejected(){order(WorkOrderType.MAINTENANCE,WorkOrderStatus.PROCESSING);when(mapper.countPlanItem(2L,3L)).thenReturn(1L);assertThrows(BusinessException.class,()->service.save(1L,List.of(result(3L),result(3L))));}
    @Test void missingRequiredItemBlocksCompletion(){when(mapper.countIncompleteRequired(1L)).thenReturn(1L);assertThrows(BusinessException.class,()->service.assertComplete(1L));}
}
