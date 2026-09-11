package com.cq.maintenance.equipment;

import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
import com.cq.maintenance.common.exception.BusinessException;import com.cq.maintenance.equipment.mapper.EquipmentMapper;import com.cq.maintenance.equipment.service.EquipmentScopeService;import com.cq.maintenance.equipment.vo.EquipmentDetailRow;import com.cq.maintenance.equipment.entity.EquipmentStatus;import com.cq.maintenance.security.LoginUser;
import java.math.BigDecimal;import java.util.List;import org.junit.jupiter.api.*;import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;import org.springframework.security.core.context.SecurityContextHolder;
class EquipmentScopeServiceTest {
 private EquipmentMapper mapper;private EquipmentScopeService service;
 @BeforeEach void setup(){mapper=mock(EquipmentMapper.class);service=new EquipmentScopeService(mapper);}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 private void login(Long workshop,List<Long> teams){LoginUser u=new LoginUser(8L,"supervisor","主管","ENABLED",List.of("MAINTENANCE_SUPERVISOR"),List.of(),teams,workshop,List.of());SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(u,null,List.of()));}
 private EquipmentDetailRow row(Long workshop,Long team){return new EquipmentDetailRow(1L,"E","设备",1L,"T","类型",null,null,null,null,null,null,null,team,null,workshop,"W","车间",1L,"L","产线",1L,"S","工位",null,EquipmentStatus.RUNNING,BigDecimal.ZERO,"q",null,null);}
 @Test void supervisorCanManageOwnWorkshop(){login(10L,List.of());when(mapper.findDetailBase(1L)).thenReturn(row(10L,null));assertDoesNotThrow(()->service.assertCanManage(1L));}
 @Test void supervisorCannotManageOtherScope(){login(10L,List.of(20L));when(mapper.findDetailBase(1L)).thenReturn(row(11L,21L));assertThrows(BusinessException.class,()->service.assertCanManage(1L));}
}
