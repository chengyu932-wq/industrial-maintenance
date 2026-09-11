package com.cq.maintenance.equipment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.cq.maintenance.common.exception.BusinessException;
import com.cq.maintenance.equipment.dto.*;
import com.cq.maintenance.equipment.entity.*;
import com.cq.maintenance.equipment.mapper.EquipmentMapper;
import com.cq.maintenance.equipment.mapper.EquipmentMapper.EquipmentDataScope;
import com.cq.maintenance.equipment.service.*;
import com.cq.maintenance.equipment.vo.*;
import com.cq.maintenance.security.LoginUser;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class EquipmentServiceTest {
    private EquipmentMapper mapper;private EquipmentScopeService scopes;private EquipmentService service;
    @BeforeEach void setUp(){mapper=mock(EquipmentMapper.class);scopes=mock(EquipmentScopeService.class);service=new EquipmentService(mapper,scopes);LoginUser u=new LoginUser(1L,"admin","管理员","ENABLED",List.of("ADMIN"),List.of(),List.of(),null,List.of());SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(u,null,List.of()));}
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    private EquipmentCreateRequest request(){return new EquipmentCreateRequest("EQ-1","设备一",2L,"M1","厂商","参数",null,null,null,null,3L,null);}
    private void valid(){EquipmentType t=new EquipmentType();t.setId(2L);when(mapper.findType(2L)).thenReturn(t);when(mapper.countEnabledStation(3L)).thenReturn(1L);doAnswer(i->{((Equipment)i.getArgument(0)).setId(10L);return 1;}).when(mapper).insertEquipment(any());}
    @Test void shouldCreateEquipmentWithPendingStateAndHistory(){valid();assertEquals(10L,service.create(request()));verify(mapper).insertStatusLog(10L,null,EquipmentStatus.PENDING,"MANUAL",null,"设备建档",1L);}
    @Test void duplicateEquipmentNumberShouldFailClearly(){when(mapper.countEquipmentNo("EQ-1",null)).thenReturn(1L);BusinessException e=assertThrows(BusinessException.class,()->service.create(request()));assertEquals("设备编号已存在",e.getMessage());}
    @Test void invalidTypeShouldFail(){when(mapper.findType(2L)).thenReturn(null);assertThrows(BusinessException.class,()->service.create(request()));}
    @Test void invalidStationShouldFail(){EquipmentType t=new EquipmentType();when(mapper.findType(2L)).thenReturn(t);assertThrows(BusinessException.class,()->service.create(request()));}
    @Test void shouldReturnBackendPage(){EquipmentQuery q=new EquipmentQuery();EquipmentDataScope s=new EquipmentDataScope(true,"ALL",1L,null,List.of());when(scopes.current()).thenReturn(s);when(mapper.findPage(q,s)).thenReturn(List.of());when(mapper.countPage(q,s)).thenReturn(21L);assertEquals(21L,service.page(q).total());}
    @Test void shouldAggregateDetailHistory(){EquipmentDataScope s=new EquipmentDataScope(true,"ALL",1L,null,List.of());when(scopes.current()).thenReturn(s);Equipment e=new Equipment();e.setId(1L);when(mapper.findEquipment(1L)).thenReturn(e);EquipmentDetailRow row=new EquipmentDetailRow(1L,"E1","设备",2L,"T","类型",null,null,null,null,null,null,null,null,null,1L,"W","车间",2L,"L","产线",3L,"S","工位",null,EquipmentStatus.PENDING,BigDecimal.ZERO,"qr",null,null);when(mapper.findDetailBase(1L)).thenReturn(row);when(mapper.findStatusHistory(1L)).thenReturn(List.of());assertEquals("车间",service.detail(1L).workshopName());}
    @Test void shouldUpdateBasicInfoWithoutStatusMutation(){valid();Equipment e=new Equipment();e.setId(1L);e.setStatus(EquipmentStatus.RUNNING);when(mapper.findEquipment(1L)).thenReturn(e);EquipmentUpdateRequest r=new EquipmentUpdateRequest("EQ-1","新名称",2L,null,null,null,null,null,null,null,3L,null);service.update(1L,r);verify(mapper).updateEquipment(e);verify(mapper,never()).updateStatus(anyLong(),any(),any());}
}
