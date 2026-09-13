package com.cq.maintenance.maintenance.service;

import com.cq.maintenance.common.exception.*;
import com.cq.maintenance.equipment.entity.EquipmentStatus;
import com.cq.maintenance.maintenance.entity.*;
import com.cq.maintenance.maintenance.mapper.MaintenanceMapper;
import com.cq.maintenance.repair.entity.RepairPriority;
import com.cq.maintenance.workorder.entity.*;
import com.cq.maintenance.workorder.mapper.WorkOrderMapper;
import com.cq.maintenance.workorder.service.*;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenanceOrderGenerator {
    private final MaintenanceMapper mapper;private final WorkOrderMapper orders;private final WorkOrderStateService states;private final BusinessNumberService numbers;private final MaintenanceScheduleService schedules;
    public MaintenanceOrderGenerator(MaintenanceMapper mapper,WorkOrderMapper orders,WorkOrderStateService states,BusinessNumberService numbers,MaintenanceScheduleService schedules){this.mapper=mapper;this.orders=orders;this.states=states;this.numbers=numbers;this.schedules=schedules;}
    @Transactional public boolean generateIfDue(Long planId,LocalDate today){MaintenancePlan p=mapper.lockPlan(planId);if(p==null||p.getStatus()!=MaintenancePlanStatus.ENABLED||p.getNextExecuteDate().isAfter(today))return false;EquipmentStatus equipmentStatus=mapper.findEquipmentStatus(p.getEquipmentId());if(equipmentStatus==null)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"保养计划关联设备不存在");if(equipmentStatus==EquipmentStatus.SCRAPPED){mapper.disablePlan(planId);return false;}if(mapper.findPlanItems(planId).isEmpty())throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"保养计划没有检查项目");Long operator=mapper.findSystemOperatorId();if(operator==null)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"系统缺少可用管理员，无法生成保养工单");WorkOrder order=new WorkOrder();order.setWorkOrderNo(numbers.next("PM"));order.setWorkOrderType(WorkOrderType.MAINTENANCE);order.setPmPlanId(planId);order.setEquipmentId(p.getEquipmentId());order.setPriority(RepairPriority.NORMAL);order.setStatus(WorkOrderStatus.PENDING_ASSIGN);order.setCreatedBy(operator);orders.insertWorkOrder(order);states.created(order,operator,"保养计划 "+p.getPlanNo()+" 到期自动生成");LocalDate next=schedules.calculateNextFutureDate(p.getNextExecuteDate(),p.getCycleType(),p.getCycleValue(),today);if(mapper.advancePlan(planId,p.getNextExecuteDate(),next)!=1)throw new BusinessException(ErrorCode.DUPLICATE_OPERATION,"保养计划执行日期已变化");return true;}
}
