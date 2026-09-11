package com.cq.maintenance.organization;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.cq.maintenance.common.exception.BusinessException;
import com.cq.maintenance.organization.dto.LineRequest;
import com.cq.maintenance.organization.dto.StationRequest;
import com.cq.maintenance.organization.entity.Workshop;
import com.cq.maintenance.organization.mapper.OrganizationMapper;
import com.cq.maintenance.organization.service.OrganizationService;
import com.cq.maintenance.organization.vo.WorkshopVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrganizationServiceTest {
    private OrganizationMapper mapper;private OrganizationService service;
    @BeforeEach void setUp(){mapper=mock(OrganizationMapper.class);service=new OrganizationService(mapper);}
    @Test void shouldQueryWorkshops(){when(mapper.findWorkshops(null)).thenReturn(List.of(new WorkshopVO(1L,"W1","一车间",null,null,"ENABLED")));assertEquals(1,service.workshops(null).size());}
    @Test void lineMustReferenceExistingWorkshop(){when(mapper.findWorkshop(9L)).thenReturn(null);BusinessException e=assertThrows(BusinessException.class,()->service.createLine(new LineRequest(9L,"L1","一线","ENABLED")));assertEquals("车间不存在",e.getMessage());}
    @Test void stationMustReferenceExistingLine(){when(mapper.findLine(9L)).thenReturn(null);assertThrows(BusinessException.class,()->service.createStation(new StationRequest(9L,"S1","一工位","ENABLED")));}
    @Test void shouldRejectDeletingWorkshopWithChildren(){Workshop w=new Workshop();w.setId(1L);when(mapper.findWorkshop(1L)).thenReturn(w);when(mapper.countLines(1L)).thenReturn(1L);assertThrows(BusinessException.class,()->service.deleteWorkshop(1L));verify(mapper,never()).deleteWorkshop(1L);}
    @Test void shouldCreateValidLine(){Workshop w=new Workshop();w.setId(1L);when(mapper.findWorkshop(1L)).thenReturn(w);service.createLine(new LineRequest(1L,"L1","一线",null));verify(mapper).insertLine(1L,"L1","一线","ENABLED");}
}
