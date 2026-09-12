package com.cq.maintenance.equipment.service;

import com.cq.maintenance.common.exception.BusinessException;
import com.cq.maintenance.common.exception.ErrorCode;
import com.cq.maintenance.equipment.entity.Equipment;
import com.cq.maintenance.equipment.entity.EquipmentStatus;
import com.cq.maintenance.equipment.mapper.EquipmentMapper;
import com.cq.maintenance.security.SecurityUtils;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentStateService {
    private static final Map<EquipmentStatus,Set<EquipmentStatus>> TRANSITIONS=Map.of(
        EquipmentStatus.PENDING,Set.of(EquipmentStatus.RUNNING),
        EquipmentStatus.RUNNING,Set.of(EquipmentStatus.FAULT,EquipmentStatus.STOPPED),
        EquipmentStatus.FAULT,Set.of(EquipmentStatus.REPAIRING,EquipmentStatus.RUNNING,EquipmentStatus.STOPPED,EquipmentStatus.SCRAPPED),
        EquipmentStatus.REPAIRING,Set.of(EquipmentStatus.RUNNING,EquipmentStatus.SCRAPPED),
        EquipmentStatus.STOPPED,Set.of(EquipmentStatus.RUNNING,EquipmentStatus.SCRAPPED),
        EquipmentStatus.SCRAPPED,Set.of());
    private static final Map<EquipmentStatus,Set<EquipmentStatus>> MANUAL_TRANSITIONS=Map.of(
        EquipmentStatus.PENDING,Set.of(EquipmentStatus.RUNNING),
        EquipmentStatus.RUNNING,Set.of(EquipmentStatus.STOPPED),
        EquipmentStatus.STOPPED,Set.of(EquipmentStatus.RUNNING));
    private final EquipmentMapper mapper;
    private final EquipmentScopeService scopes;
    public EquipmentStateService(EquipmentMapper mapper,EquipmentScopeService scopes){this.mapper=mapper;this.scopes=scopes;}

    @Transactional public void manual(Long equipmentId,EquipmentStatus target,String reason){
        scopes.assertCanManage(equipmentId);
        Equipment equipment=mapper.findEquipment(equipmentId);
        if(equipment==null)throw new BusinessException(ErrorCode.NOT_FOUND,"设备不存在");
        if(!MANUAL_TRANSITIONS.getOrDefault(equipment.getStatus(),Set.of()).contains(target))throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION,"故障与维修状态须由后续报修/工单流程触发，报废请使用独立接口");
        transition(equipmentId,target,reason,"MANUAL",null,SecurityUtils.currentUser().userId());
    }
    @Transactional public void scrap(Long equipmentId,String reason){scopes.assertCanManage(equipmentId);transition(equipmentId,EquipmentStatus.SCRAPPED,reason,"SCRAP",null,SecurityUtils.currentUser().userId());}

    @Transactional public void cancelWorkOrder(Long equipmentId,EquipmentStatus target,String reason,Long workOrderId,Long operatorId){
        if(target!=EquipmentStatus.RUNNING&&target!=EquipmentStatus.STOPPED)throw new BusinessException(ErrorCode.PARAMETER_ERROR,"取消工单后的设备状态只能为运行或停用");
        Equipment equipment=mapper.findEquipment(equipmentId);
        if(equipment==null)throw new BusinessException(ErrorCode.NOT_FOUND,"设备不存在");
        if(equipment.getStatus()!=EquipmentStatus.FAULT)throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION,"仅故障设备可随最后一张维修工单取消进行状态处置");
        transition(equipmentId,target,reason,"WORK_ORDER",workOrderId,operatorId);
    }

    @Transactional public void transition(Long equipmentId,EquipmentStatus target,String reason,String sourceType,Long sourceId,Long operatorId){
        Equipment e=mapper.findEquipment(equipmentId);if(e==null)throw new BusinessException(ErrorCode.NOT_FOUND,"设备不存在");
        EquipmentStatus current=e.getStatus();
        if(current==target)throw new BusinessException(ErrorCode.DUPLICATE_OPERATION,"设备已处于目标状态");
        if(!TRANSITIONS.getOrDefault(current,Set.of()).contains(target))throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION,"设备状态不能从"+current.label()+"变更为"+target.label());
        if(mapper.updateStatus(equipmentId,current,target)!=1)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"设备状态已变化，请刷新后重试");
        mapper.insertStatusLog(equipmentId,current,target,sourceType,sourceId,reason.trim(),operatorId);
    }
}
