package com.cq.maintenance.maintenance.service;

import com.cq.maintenance.common.exception.*;
import com.cq.maintenance.common.response.PageResult;
import com.cq.maintenance.equipment.entity.*;
import com.cq.maintenance.equipment.service.*;
import com.cq.maintenance.maintenance.dto.*;
import com.cq.maintenance.maintenance.entity.*;
import com.cq.maintenance.maintenance.mapper.MaintenanceMapper;
import com.cq.maintenance.maintenance.vo.*;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenancePlanService {
    private final MaintenanceMapper mapper;private final EquipmentService equipment;private final EquipmentScopeService scopes;
    public MaintenancePlanService(MaintenanceMapper mapper,EquipmentService equipment,EquipmentScopeService scopes){this.mapper=mapper;this.equipment=equipment;this.scopes=scopes;}
    public PageResult<MaintenancePlanVO> page(MaintenancePlanQuery q){var s=scopes.current();return PageResult.of(mapper.findPage(q,s),mapper.countPage(q,s),q.getPage(),q.getSize());}
    public MaintenancePlanVO detail(Long id){assertManage(id);MaintenancePlanVO p=requiredVO(id);return withItems(p);}
    public List<MaintenanceHistoryVO> history(Long id){assertManage(id);required(id);return mapper.findPlanHistory(id);}
    @Transactional public Long create(MaintenancePlanRequest r){validate(r,null);MaintenancePlan p=copy(new MaintenancePlan(),r);p.setStatus(MaintenancePlanStatus.ENABLED);mapper.insertPlan(p);return p.getId();}
    @Transactional public void update(Long id,MaintenancePlanRequest r){assertManage(id);MaintenancePlan current=required(id);validate(r,id);copy(current,r);mapper.updatePlan(current);}
    @Transactional public void setStatus(Long id,MaintenancePlanStatus status){assertManage(id);MaintenancePlan p=required(id);if(status==MaintenancePlanStatus.ENABLED&&equipment.required(p.getEquipmentId()).getStatus()==EquipmentStatus.SCRAPPED)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"报废设备的保养计划不能启用");if(p.getStatus()==status)throw new BusinessException(ErrorCode.DUPLICATE_OPERATION,"计划已处于目标状态");mapper.updatePlanStatus(id,status);}
    @Transactional public void replaceItems(Long id,List<MaintenancePlanItemRequest> items){assertManage(id);required(id);if(items==null||items.isEmpty())throw new BusinessException(ErrorCode.PARAMETER_ERROR,"保养计划至少需要一个检查项目");if(mapper.countGeneratedOrders(id)>0)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"计划已生成保养工单，为保护历史检查标准不能再修改检查项目");mapper.deletePlanItems(id);for(MaintenancePlanItemRequest r:items){MaintenancePlanItem item=new MaintenancePlanItem();item.setPlanId(id);item.setItemName(r.itemName().trim());item.setStandardDescription(trim(r.standardDescription()));item.setSortNo(r.sortNo());item.setRequired(r.required());mapper.insertPlanItem(item);}}
    public List<MaintenancePlanItemVO> items(Long id){assertManage(id);required(id);return mapper.findPlanItems(id);}
    public List<MaintenanceHistoryVO> equipmentHistory(Long equipmentId){equipment.assertVisible(equipmentId);return mapper.findEquipmentHistory(equipmentId);}
    private void validate(MaintenancePlanRequest r,Long id){scopes.assertCanManage(r.equipmentId());Equipment e=equipment.required(r.equipmentId());if(e.getStatus()==EquipmentStatus.SCRAPPED)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"报废设备不能建立保养计划");if(r.nextExecuteDate().isBefore(LocalDate.now()))throw new BusinessException(ErrorCode.PARAMETER_ERROR,"下次执行日期不能早于今天");if(mapper.countPlanNo(r.planNo().trim(),id)>0)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"保养计划编号已存在");}
    private MaintenancePlan copy(MaintenancePlan p,MaintenancePlanRequest r){p.setPlanNo(r.planNo().trim());p.setPlanName(r.planName().trim());p.setEquipmentId(r.equipmentId());p.setCycleType(r.cycleType());p.setCycleValue(r.cycleValue());p.setNextExecuteDate(r.nextExecuteDate());p.setRunningHourThreshold(r.runningHourThreshold());return p;}
    private void assertManage(Long id){MaintenancePlan p=required(id);scopes.assertCanManage(p.getEquipmentId());}
    private MaintenancePlan required(Long id){MaintenancePlan p=mapper.findPlan(id);if(p==null)throw new BusinessException(ErrorCode.NOT_FOUND,"保养计划不存在");return p;}
    private MaintenancePlanVO requiredVO(Long id){MaintenancePlanVO p=mapper.findPlanVO(id);if(p==null)throw new BusinessException(ErrorCode.NOT_FOUND,"保养计划不存在");return p;}
    private MaintenancePlanVO withItems(MaintenancePlanVO p){return new MaintenancePlanVO(p.id(),p.planNo(),p.planName(),p.equipmentId(),p.equipmentNo(),p.equipmentName(),p.workshopId(),p.workshopName(),p.responsibleTeamId(),p.responsibleTeamName(),p.cycleType(),p.cycleValue(),p.nextExecuteDate(),p.runningHourThreshold(),p.status(),p.lastGeneratedAt(),p.createdAt(),p.updatedAt(),mapper.findPlanItems(p.id()));}
    private String trim(String value){return value==null||value.isBlank()?null:value.trim();}
}
