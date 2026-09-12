package com.cq.maintenance.workorder;

import static org.junit.jupiter.api.Assertions.*;import static org.mockito.ArgumentMatchers.*;import static org.mockito.Mockito.*;
import com.cq.maintenance.common.exception.BusinessException;import com.cq.maintenance.workorder.entity.*;import com.cq.maintenance.workorder.mapper.WorkOrderMapper;import com.cq.maintenance.workorder.service.WorkOrderStateService;import org.junit.jupiter.api.*;

class WorkOrderStateServiceTest {
    WorkOrderMapper mapper;WorkOrderStateService service;
    @BeforeEach void setUp(){mapper=mock(WorkOrderMapper.class);service=new WorkOrderStateService(mapper);when(mapper.assign(anyLong(),anyLong(),anyLong())).thenReturn(1);when(mapper.acceptResponse(anyLong())).thenReturn(1);when(mapper.updateState(anyLong(),any(),any(),anyString())).thenReturn(1);when(mapper.cancel(anyLong(),any(),anyString())).thenReturn(1);}
    WorkOrder order(WorkOrderStatus status){WorkOrder o=new WorkOrder();o.setId(1L);o.setStatus(status);return o;}
    @Test void pendingCanBeAssigned(){service.assign(order(WorkOrderStatus.PENDING_ASSIGN),2L,3L,"人工派单",9L);verify(mapper).insertFlow(1L,WorkOrderStatus.PENDING_ASSIGN,WorkOrderStatus.ASSIGNED,"ASSIGN",9L,"人工派单");}
    @Test void assignedCannotBeAssignedAgain(){assertThrows(BusinessException.class,()->service.assign(order(WorkOrderStatus.ASSIGNED),2L,3L,null,9L));}
    @Test void engineerCanAcceptResponseWithoutNewStatus(){service.acceptResponse(order(WorkOrderStatus.ASSIGNED),2L);verify(mapper).insertFlow(1L,WorkOrderStatus.ASSIGNED,WorkOrderStatus.ASSIGNED,"ACCEPT",2L,"工程师确认接单");}
    @Test void repeatedAcceptIsRejected(){WorkOrder o=order(WorkOrderStatus.ASSIGNED);o.setAcceptedAt(java.time.LocalDateTime.now());assertThrows(BusinessException.class,()->service.acceptResponse(o,2L));}
    @Test void assignedCanStart(){service.transition(order(WorkOrderStatus.ASSIGNED),WorkOrderStatus.PROCESSING,"START",null,2L);}
    @Test void processingCanSuspend(){service.transition(order(WorkOrderStatus.PROCESSING),WorkOrderStatus.SUSPENDED,"SUSPEND","等待备件",2L);}
    @Test void suspendedCanResume(){service.transition(order(WorkOrderStatus.SUSPENDED),WorkOrderStatus.PROCESSING,"RESUME","备件到货",2L);}
    @Test void processingCanSubmit(){service.transition(order(WorkOrderStatus.PROCESSING),WorkOrderStatus.PENDING_ACCEPT,"SUBMIT",null,2L);}
    @Test void acceptanceCanPass(){service.transition(order(WorkOrderStatus.PENDING_ACCEPT),WorkOrderStatus.COMPLETED,"ACCEPT_PASS","正常",4L);}
    @Test void acceptanceCanReturn(){service.transition(order(WorkOrderStatus.PENDING_ACCEPT),WorkOrderStatus.PROCESSING,"ACCEPT_RETURN","仍有异响",4L);}
    @Test void pendingCanCancel(){service.cancel(order(WorkOrderStatus.PENDING_ASSIGN),"误报",4L);}
    @Test void completedCannotChange(){assertThrows(BusinessException.class,()->service.transition(order(WorkOrderStatus.COMPLETED),WorkOrderStatus.PROCESSING,"START",null,2L));}
    @Test void concurrentUpdateIsRejected(){when(mapper.updateState(anyLong(),any(),any(),anyString())).thenReturn(0);assertThrows(BusinessException.class,()->service.transition(order(WorkOrderStatus.ASSIGNED),WorkOrderStatus.PROCESSING,"START",null,2L));verify(mapper,never()).insertFlow(anyLong(),any(),any(),anyString(),anyLong(),any());}
}
