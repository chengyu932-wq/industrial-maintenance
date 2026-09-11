package com.cq.maintenance.equipment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.cq.maintenance.common.exception.BusinessException;
import com.cq.maintenance.equipment.entity.*;
import com.cq.maintenance.equipment.mapper.EquipmentMapper;
import com.cq.maintenance.equipment.service.*;
import com.cq.maintenance.security.LoginUser;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class EquipmentStateServiceTest {
    private EquipmentMapper mapper;private EquipmentStateService service;
    @BeforeEach void setUp(){mapper=mock(EquipmentMapper.class);service=new EquipmentStateService(mapper,mock(EquipmentScopeService.class));LoginUser u=new LoginUser(7L,"admin","管理员","ENABLED",List.of("ADMIN"),List.of(),List.of(),null,List.of());SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(u,null,List.of()));}
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    private void state(EquipmentStatus s){Equipment e=new Equipment();e.setId(1L);e.setStatus(s);when(mapper.findEquipment(1L)).thenReturn(e);when(mapper.updateStatus(anyLong(),any(),any())).thenReturn(1);}
    @Test void pendingToRunningIsLegalAndWritesHistory(){state(EquipmentStatus.PENDING);service.manual(1L,EquipmentStatus.RUNNING,"正式启用");verify(mapper).updateStatus(1L,EquipmentStatus.PENDING,EquipmentStatus.RUNNING);verify(mapper).insertStatusLog(1L,EquipmentStatus.PENDING,EquipmentStatus.RUNNING,"MANUAL",null,"正式启用",7L);}
    @Test void pendingToStoppedIsIllegal(){state(EquipmentStatus.PENDING);assertThrows(BusinessException.class,()->service.manual(1L,EquipmentStatus.STOPPED,"跳过启用"));verify(mapper,never()).insertStatusLog(anyLong(),any(),any(),anyString(),any(),anyString(),anyLong());}
    @Test void manualFaultTransitionIsForbiddenUntilWorkOrderStage(){state(EquipmentStatus.RUNNING);assertThrows(BusinessException.class,()->service.manual(1L,EquipmentStatus.FAULT,"模拟故障"));}
    @Test void manualRepairCompletionIsForbiddenUntilWorkOrderStage(){state(EquipmentStatus.REPAIRING);assertThrows(BusinessException.class,()->service.manual(1L,EquipmentStatus.RUNNING,"绕过工单完成维修"));verify(mapper,never()).updateStatus(anyLong(),any(),any());}
    @Test void stoppedEquipmentCanBeScrapped(){state(EquipmentStatus.STOPPED);service.scrap(1L,"达到使用年限");verify(mapper).insertStatusLog(1L,EquipmentStatus.STOPPED,EquipmentStatus.SCRAPPED,"SCRAP",null,"达到使用年限",7L);}
    @Test void scrappedEquipmentCannotReturnToRunning(){state(EquipmentStatus.SCRAPPED);assertThrows(BusinessException.class,()->service.manual(1L,EquipmentStatus.RUNNING,"恢复"));}
    @Test void optimisticConflictDoesNotWriteHistory(){state(EquipmentStatus.RUNNING);when(mapper.updateStatus(anyLong(),any(),any())).thenReturn(0);assertThrows(BusinessException.class,()->service.manual(1L,EquipmentStatus.STOPPED,"停机"));verify(mapper,never()).insertStatusLog(anyLong(),any(),any(),anyString(),any(),anyString(),anyLong());}
}
