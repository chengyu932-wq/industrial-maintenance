package com.cq.maintenance.equipment.service;

import com.cq.maintenance.common.exception.BusinessException;
import com.cq.maintenance.common.exception.ErrorCode;
import com.cq.maintenance.equipment.dto.EquipmentTypeRequest;
import com.cq.maintenance.equipment.entity.EquipmentType;
import com.cq.maintenance.equipment.mapper.EquipmentMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentTypeService {
    private final EquipmentMapper mapper;
    public EquipmentTypeService(EquipmentMapper mapper){this.mapper=mapper;}
    public List<EquipmentType> list(){return mapper.findTypes();}
    @Transactional public void create(EquipmentTypeRequest r){unique(r.typeCode(),null);EquipmentType t=copy(new EquipmentType(),r);mapper.insertType(t);}
    @Transactional public void update(Long id,EquipmentTypeRequest r){EquipmentType t=required(id);unique(r.typeCode(),id);mapper.updateType(copy(t,r));}
    @Transactional public void delete(Long id){required(id);if(mapper.countEquipmentByType(id)>0)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"设备类型已被设备引用，请改为停用");mapper.deleteType(id);}
    private EquipmentType required(Long id){EquipmentType t=mapper.findType(id);if(t==null)throw new BusinessException(ErrorCode.NOT_FOUND,"设备类型不存在");return t;}
    private void unique(String code,Long id){if(mapper.countTypeCode(code,id)>0)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"设备类型编码已存在");}
    private EquipmentType copy(EquipmentType t,EquipmentTypeRequest r){t.setTypeCode(r.typeCode().trim());t.setTypeName(r.typeName().trim());t.setDescription(r.description());t.setStatus(r.status()==null||r.status().isBlank()?"ENABLED":r.status());return t;}
}
