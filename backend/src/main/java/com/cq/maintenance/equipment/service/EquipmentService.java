package com.cq.maintenance.equipment.service;

import com.cq.maintenance.common.exception.BusinessException;
import com.cq.maintenance.common.exception.ErrorCode;
import com.cq.maintenance.common.response.PageResult;
import com.cq.maintenance.equipment.dto.EquipmentCreateRequest;
import com.cq.maintenance.equipment.dto.EquipmentQuery;
import com.cq.maintenance.equipment.dto.EquipmentUpdateRequest;
import com.cq.maintenance.equipment.entity.Equipment;
import com.cq.maintenance.equipment.entity.EquipmentStatus;
import com.cq.maintenance.equipment.mapper.EquipmentMapper;
import com.cq.maintenance.equipment.mapper.EquipmentMapper.EquipmentDataScope;
import com.cq.maintenance.equipment.vo.EquipmentDetailVO;
import com.cq.maintenance.equipment.vo.EquipmentDetailRow;
import com.cq.maintenance.equipment.vo.EquipmentListVO;
import com.cq.maintenance.equipment.vo.EquipmentStatusLogVO;
import com.cq.maintenance.security.SecurityUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentService {
    private final EquipmentMapper mapper; private final EquipmentScopeService scopes;
    public EquipmentService(EquipmentMapper mapper,EquipmentScopeService scopes){this.mapper=mapper;this.scopes=scopes;}
    public PageResult<EquipmentListVO> page(EquipmentQuery q){EquipmentDataScope s=scopes.current();return PageResult.of(mapper.findPage(q,s),mapper.countPage(q,s),q.getPage(),q.getSize());}
    public EquipmentDetailVO detail(Long id){
        assertVisible(id); EquipmentDetailRow d=mapper.findDetailBase(id);if(d==null)throw new BusinessException(ErrorCode.NOT_FOUND,"设备不存在");
        return new EquipmentDetailVO(d.id(),d.equipmentNo(),d.equipmentName(),d.typeId(),d.typeCode(),d.typeName(),d.model(),d.manufacturer(),d.specifications(),
            d.manufactureDate(),d.commissioningDate(),d.responsibleUserId(),d.responsibleUserName(),d.responsibleTeamId(),d.responsibleTeamName(),d.workshopId(),d.workshopNo(),
            d.workshopName(),d.lineId(),d.lineNo(),d.lineName(),d.stationId(),d.stationNo(),d.stationName(),d.warrantyExpireDate(),d.status(),d.runningHours(),d.qrCode(),d.createdAt(),d.updatedAt(),mapper.findStatusHistory(id));
    }
    public List<EquipmentStatusLogVO> history(Long id){assertVisible(id);return mapper.findStatusHistory(id);}
    @Transactional public Long create(EquipmentCreateRequest r){
        validate(r.equipmentNo(),null,r.typeId(),r.stationId(),r.responsibleUserId(),r.responsibleTeamId());
        scopes.assertCanCreate(r.stationId(),r.responsibleTeamId());
        Equipment e=copy(new Equipment(),r.equipmentNo(),r.equipmentName(),r.typeId(),r.model(),r.manufacturer(),r.specifications(),r.manufactureDate(),r.commissioningDate(),r.responsibleUserId(),r.responsibleTeamId(),r.stationId(),r.warrantyExpireDate());
        e.setStatus(EquipmentStatus.PENDING);e.setRunningHours(BigDecimal.ZERO);e.setQrCode(UUID.randomUUID().toString());mapper.insertEquipment(e);
        mapper.insertStatusLog(e.getId(),null,EquipmentStatus.PENDING,"MANUAL",null,"设备建档",SecurityUtils.currentUser().userId());return e.getId();
    }
    @Transactional public void update(Long id,EquipmentUpdateRequest r){Equipment e=required(id);scopes.assertCanManage(id);if(e.getStatus()==EquipmentStatus.SCRAPPED)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"报废设备不能修改基础资料");
        validate(r.equipmentNo(),id,r.typeId(),r.stationId(),r.responsibleUserId(),r.responsibleTeamId());scopes.assertCanCreate(r.stationId(),r.responsibleTeamId());copy(e,r.equipmentNo(),r.equipmentName(),r.typeId(),r.model(),r.manufacturer(),r.specifications(),r.manufactureDate(),r.commissioningDate(),r.responsibleUserId(),r.responsibleTeamId(),r.stationId(),r.warrantyExpireDate());mapper.updateEquipment(e);}
    public Equipment required(Long id){Equipment e=mapper.findEquipment(id);if(e==null)throw new BusinessException(ErrorCode.NOT_FOUND,"设备不存在");return e;}
    public void assertVisible(Long id){EquipmentDataScope s=scopes.current();
        if(s.allData()){required(id);return;} EquipmentDetailRow d=mapper.findDetailBase(id);if(d==null)throw new BusinessException(ErrorCode.NOT_FOUND,"设备不存在");
        boolean visible=s.mode().equals("REFERENCE")?d.status()!=EquipmentStatus.SCRAPPED:s.mode().equals("SUPERVISOR")&&((s.workshopId()!=null&&s.workshopId().equals(d.workshopId()))||(d.responsibleTeamId()!=null&&s.teamIds().contains(d.responsibleTeamId())))||s.mode().equals("ENGINEER")&&((d.responsibleUserId()!=null&&d.responsibleUserId().equals(s.userId()))||(d.responsibleTeamId()!=null&&s.teamIds().contains(d.responsibleTeamId())));
        if(!visible)throw new BusinessException(ErrorCode.DATA_FORBIDDEN,"无权访问该设备");}
    private void validate(String no,Long id,Long typeId,Long stationId,Long userId,Long teamId){
        if(mapper.countEquipmentNo(no.trim(),id)>0)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"设备编号已存在");
        if(mapper.findType(typeId)==null)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"设备类型不存在");
        if(mapper.countEnabledStation(stationId)==0)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"工位不存在或已停用");
        if(userId!=null&&mapper.countEnabledUser(userId)==0)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"责任人不存在或已禁用");
        if(teamId!=null&&mapper.countEnabledTeam(teamId)==0)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"负责班组不存在或已停用");}
    private Equipment copy(Equipment e,String no,String name,Long typeId,String model,String manufacturer,String specs,java.time.LocalDate manufacture,java.time.LocalDate commissioning,Long userId,Long teamId,Long stationId,java.time.LocalDate warranty){
        e.setEquipmentNo(no.trim());e.setEquipmentName(name.trim());e.setTypeId(typeId);e.setModel(model);e.setManufacturer(manufacturer);e.setSpecifications(specs);e.setManufactureDate(manufacture);e.setCommissioningDate(commissioning);e.setResponsibleUserId(userId);e.setResponsibleTeamId(teamId);e.setStationId(stationId);e.setWarrantyExpireDate(warranty);return e;}
}
