package com.cq.maintenance.workorder;

import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
import com.cq.maintenance.common.exception.BusinessException;import com.cq.maintenance.security.LoginUser;import com.cq.maintenance.workorder.entity.*;import com.cq.maintenance.workorder.mapper.WorkOrderMapper;import com.cq.maintenance.workorder.service.WorkOrderScopeService;import com.cq.maintenance.workorder.vo.WorkOrderListVO;import java.util.List;import org.junit.jupiter.api.*;import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;import org.springframework.security.core.context.SecurityContextHolder;

class WorkOrderScopeServiceTest {
    WorkOrderMapper mapper;WorkOrderScopeService service;
    @BeforeEach void setUp(){mapper=mock(WorkOrderMapper.class);service=new WorkOrderScopeService(mapper);}
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    void login(Long id,String role,Long workshop,List<Long> teams){LoginUser u=new LoginUser(id,"u","用户","ENABLED",List.of(role),List.of(),teams,workshop,List.of());SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(u,null,List.of()));}
    WorkOrderListVO summary(Long reporter){return new WorkOrderListVO(1L,"WO",WorkOrderType.REPAIR,2L,"RR",3L,"E","设备",4L,"车间",null,WorkOrderStatus.PENDING_ACCEPT,reporter,"报修人",7L,"工程师",8L,"班组",null,null,null,null,null,null,0,null);}
    @Test void adminGetsAllScope(){login(1L,"ADMIN",null,List.of());assertTrue(service.current().allData());}
    @Test void supervisorGetsWorkshopScope(){login(2L,"MAINTENANCE_SUPERVISOR",4L,List.of(8L));assertEquals("SUPERVISOR",service.current().mode());}
    @Test void engineerGetsSelfOrTeamScope(){login(3L,"ENGINEER",4L,List.of(8L));assertEquals("ENGINEER",service.current().mode());}
    @Test void reporterGetsSelfScope(){login(5L,"REPORTER",null,List.of());assertEquals("REPORTER",service.current().mode());}
    @Test void invisibleOrderIsRejected(){login(5L,"REPORTER",null,List.of());when(mapper.countVisible(anyLong(),any())).thenReturn(0L);assertThrows(BusinessException.class,()->service.assertVisible(1L));}
    @Test void nonEngineerCannotProcessAssignedOrder(){login(2L,"MAINTENANCE_SUPERVISOR",4L,List.of());WorkOrder o=new WorkOrder();o.setAssignedEngineerId(2L);assertThrows(BusinessException.class,()->service.assertAssignedEngineer(o));}
    @Test void otherEngineerCannotProcessOrder(){login(3L,"ENGINEER",4L,List.of());WorkOrder o=new WorkOrder();o.setAssignedEngineerId(9L);assertThrows(BusinessException.class,()->service.assertAssignedEngineer(o));}
    @Test void assignedEngineerCanProcess(){login(3L,"ENGINEER",4L,List.of());WorkOrder o=new WorkOrder();o.setAssignedEngineerId(3L);assertDoesNotThrow(()->service.assertAssignedEngineer(o));}
    @Test void reporterCanAcceptOwnOrder(){login(5L,"REPORTER",null,List.of());assertDoesNotThrow(()->service.assertCanAccept(summary(5L)));}
    @Test void reporterCannotAcceptOthersOrder(){login(5L,"REPORTER",null,List.of());assertThrows(BusinessException.class,()->service.assertCanAccept(summary(6L)));}
}
