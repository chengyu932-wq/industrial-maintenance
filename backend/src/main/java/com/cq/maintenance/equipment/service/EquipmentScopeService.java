package com.cq.maintenance.equipment.service;

import com.cq.maintenance.equipment.mapper.EquipmentMapper.EquipmentDataScope;
import com.cq.maintenance.equipment.mapper.EquipmentMapper;
import com.cq.maintenance.equipment.vo.EquipmentDetailRow;
import com.cq.maintenance.common.exception.BusinessException;
import com.cq.maintenance.common.exception.ErrorCode;
import com.cq.maintenance.security.LoginUser;
import com.cq.maintenance.security.SecurityUtils;
import org.springframework.stereotype.Service;

@Service
public class EquipmentScopeService {
    private final EquipmentMapper mapper;
    public EquipmentScopeService(EquipmentMapper mapper){this.mapper=mapper;}
    public EquipmentDataScope current() {
        LoginUser u=SecurityUtils.currentUser();
        if(u.roleCodes().contains("ADMIN")) return new EquipmentDataScope(true,"ALL",u.userId(),u.workshopId(),u.teamIds());
        if(u.roleCodes().contains("MAINTENANCE_SUPERVISOR")) return new EquipmentDataScope(false,"SUPERVISOR",u.userId(),u.workshopId(),u.teamIds());
        if(u.roleCodes().contains("ENGINEER")) return new EquipmentDataScope(false,"ENGINEER",u.userId(),u.workshopId(),u.teamIds());
        return new EquipmentDataScope(false,"REFERENCE",u.userId(),u.workshopId(),u.teamIds());
    }
    public void assertCanManage(Long equipmentId){EquipmentDetailRow d=mapper.findDetailBase(equipmentId);if(d==null)throw new BusinessException(ErrorCode.NOT_FOUND,"设备不存在");assertCanManage(d.workshopId(),d.responsibleTeamId());}
    public void assertCanCreate(Long stationId,Long teamId){assertCanManage(mapper.findWorkshopIdByStation(stationId),teamId);}
    private void assertCanManage(Long workshopId,Long teamId){EquipmentDataScope s=current();if(s.allData())return;boolean allowed="SUPERVISOR".equals(s.mode())&&((s.workshopId()!=null&&s.workshopId().equals(workshopId))||(teamId!=null&&s.teamIds().contains(teamId)));if(!allowed)throw new BusinessException(ErrorCode.DATA_FORBIDDEN,"无权管理该范围内的设备");}
}
