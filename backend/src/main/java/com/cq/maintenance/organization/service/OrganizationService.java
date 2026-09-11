package com.cq.maintenance.organization.service;

import com.cq.maintenance.common.exception.BusinessException;
import com.cq.maintenance.common.exception.ErrorCode;
import com.cq.maintenance.organization.dto.LineRequest;
import com.cq.maintenance.organization.dto.StationRequest;
import com.cq.maintenance.organization.dto.WorkshopRequest;
import com.cq.maintenance.organization.entity.Workshop;
import com.cq.maintenance.organization.mapper.OrganizationMapper;
import com.cq.maintenance.organization.vo.LineVO;
import com.cq.maintenance.organization.vo.OrganizationTreeVO;
import com.cq.maintenance.organization.vo.StationVO;
import com.cq.maintenance.organization.vo.WorkshopVO;
import com.cq.maintenance.organization.vo.TeamVO;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {
    private final OrganizationMapper mapper;
    public OrganizationService(OrganizationMapper mapper) { this.mapper = mapper; }

    public List<WorkshopVO> workshops(String status) { return mapper.findWorkshops(status); }
    public List<LineVO> lines(Long workshopId,String status) { return mapper.findLines(workshopId,status); }
    public List<StationVO> stations(Long lineId,String status) { return mapper.findStations(lineId,status); }
    public List<TeamVO> teams(Long workshopId,String status){return mapper.findTeams(workshopId,status);}

    public List<OrganizationTreeVO> tree(String status) {
        List<LineVO> lines=mapper.findLines(null,status); List<StationVO> stations=mapper.findStations(null,status);
        return mapper.findWorkshops(status).stream().map(w -> new OrganizationTreeVO(w.id(),"WORKSHOP",w.workshopNo(),w.workshopName(),w.status(),
            lines.stream().filter(l -> l.workshopId().equals(w.id())).map(l -> new OrganizationTreeVO(l.id(),"LINE",l.lineNo(),l.lineName(),l.status(),
                stations.stream().filter(s -> s.lineId().equals(l.id())).map(s -> new OrganizationTreeVO(s.id(),"STATION",s.stationNo(),s.stationName(),s.status(),List.of())).toList())).toList())).toList();
    }

    @Transactional public void createWorkshop(WorkshopRequest r) {
        validateManager(r.managerId()); unique(mapper.countWorkshopCode(r.workshopNo(),null),"车间编号已存在");
        Workshop w=new Workshop(); w.setWorkshopNo(r.workshopNo().trim()); w.setWorkshopName(r.workshopName().trim());
        w.setManagerId(r.managerId()); w.setStatus(status(r.status())); mapper.insertWorkshop(w);
    }
    @Transactional public void updateWorkshop(Long id,WorkshopRequest r) {
        Workshop w=workshop(id); validateManager(r.managerId()); unique(mapper.countWorkshopCode(r.workshopNo(),id),"车间编号已存在");
        w.setWorkshopNo(r.workshopNo().trim());w.setWorkshopName(r.workshopName().trim());w.setManagerId(r.managerId());w.setStatus(status(r.status()));mapper.updateWorkshop(w);
    }
    @Transactional public void deleteWorkshop(Long id) {
        workshop(id); if(mapper.countLines(id)>0 || mapper.countTeams(id)>0) conflict("车间存在产线或班组，请先处理下级数据或将车间停用"); mapper.deleteWorkshop(id);
    }
    @Transactional public void createLine(LineRequest r) { workshop(r.workshopId()); unique(mapper.countLineCode(r.workshopId(),r.lineNo(),null),"同一车间下产线编号已存在"); mapper.insertLine(r.workshopId(),r.lineNo().trim(),r.lineName().trim(),status(r.status())); }
    @Transactional public void updateLine(Long id,LineRequest r) { line(id); workshop(r.workshopId()); unique(mapper.countLineCode(r.workshopId(),r.lineNo(),id),"同一车间下产线编号已存在"); mapper.updateLine(id,r.workshopId(),r.lineNo().trim(),r.lineName().trim(),status(r.status())); }
    @Transactional public void deleteLine(Long id) { line(id); if(mapper.countStations(id)>0) conflict("产线存在工位，请先处理下级数据或将产线停用"); mapper.deleteLine(id); }
    @Transactional public void createStation(StationRequest r) { line(r.lineId()); unique(mapper.countStationCode(r.lineId(),r.stationNo(),null),"同一产线下工位编号已存在"); mapper.insertStation(r.lineId(),r.stationNo().trim(),r.stationName().trim(),status(r.status())); }
    @Transactional public void updateStation(Long id,StationRequest r) { station(id); line(r.lineId()); unique(mapper.countStationCode(r.lineId(),r.stationNo(),id),"同一产线下工位编号已存在"); mapper.updateStation(id,r.lineId(),r.stationNo().trim(),r.stationName().trim(),status(r.status())); }
    @Transactional public void deleteStation(Long id) { station(id); if(mapper.countEquipment(id)>0) conflict("工位已挂载设备，请先迁移设备或将工位停用"); mapper.deleteStation(id); }

    private Workshop workshop(Long id){ Workshop w=mapper.findWorkshop(id);if(w==null)throw new BusinessException(ErrorCode.NOT_FOUND,"车间不存在");return w; }
    private void line(Long id){if(mapper.findLine(id)==null)throw new BusinessException(ErrorCode.NOT_FOUND,"产线不存在");}
    private void station(Long id){if(mapper.findStation(id)==null)throw new BusinessException(ErrorCode.NOT_FOUND,"工位不存在");}
    private void validateManager(Long id){if(id!=null&&mapper.countEnabledUser(id)==0)conflict("车间负责人不存在或已禁用");}
    private static String status(String value){return value==null||value.isBlank()?"ENABLED":value;}
    private static void unique(long count,String message){if(count>0)conflict(message);}
    private static void conflict(String message){throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,message);}
}
