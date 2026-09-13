package com.cq.maintenance.maintenance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cq.maintenance.equipment.entity.EquipmentStatus;
import com.cq.maintenance.equipment.mapper.EquipmentMapper.EquipmentDataScope;
import com.cq.maintenance.maintenance.dto.*;
import com.cq.maintenance.maintenance.entity.*;
import com.cq.maintenance.maintenance.vo.*;
import java.time.*;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface MaintenanceMapper extends BaseMapper<MaintenancePlan> {
    String FROM=" FROM pm_plan p JOIN eqp_equipment e ON e.id=p.equipment_id JOIN org_station st ON st.id=e.station_id JOIN org_line l ON l.id=st.line_id JOIN org_workshop w ON w.id=l.workshop_id LEFT JOIN org_team t ON t.id=e.responsible_team_id ";
    String COLS="p.id,p.plan_no,p.plan_name,p.equipment_id,e.equipment_no,e.equipment_name,w.id workshop_id,w.workshop_name,e.responsible_team_id,t.team_name responsible_team_name,p.cycle_type,p.cycle_value,p.next_execute_date,p.running_hour_threshold,p.status,p.last_generated_at,p.created_at,p.updated_at";
    String FILTER="<if test='q.keyword != null and q.keyword != &quot;&quot;'> AND (p.plan_no LIKE CONCAT('%',#{q.keyword},'%') OR p.plan_name LIKE CONCAT('%',#{q.keyword},'%') OR e.equipment_no LIKE CONCAT('%',#{q.keyword},'%') OR e.equipment_name LIKE CONCAT('%',#{q.keyword},'%'))</if><if test='q.equipmentId != null'> AND p.equipment_id=#{q.equipmentId}</if><if test='q.cycleType != null'> AND p.cycle_type=#{q.cycleType}</if><if test='q.status != null'> AND p.status=#{q.status}</if>";
    String SCOPE="<if test='scope.allData == false'> AND (<choose><when test='scope.mode == &quot;SUPERVISOR&quot;'><if test='scope.workshopId != null'>w.id=#{scope.workshopId}</if><if test='scope.workshopId == null'>1=0</if><if test='scope.teamIds != null and scope.teamIds.size() > 0'> OR e.responsible_team_id IN <foreach collection='scope.teamIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if></when><otherwise>1=0</otherwise></choose>)</if>";

    @Select("<script>SELECT "+COLS+FROM+"<where>1=1 "+FILTER+SCOPE+"</where> ORDER BY p.next_execute_date,p.id LIMIT #{q.size} OFFSET #{q.offset}</script>")
    List<MaintenancePlanVO> findPage(@Param("q") MaintenancePlanQuery q,@Param("scope") EquipmentDataScope scope);
    @Select("<script>SELECT COUNT(*)"+FROM+"<where>1=1 "+FILTER+SCOPE+"</where></script>") long countPage(@Param("q") MaintenancePlanQuery q,@Param("scope") EquipmentDataScope scope);
    @Select("SELECT "+COLS+FROM+" WHERE p.id=#{id}") MaintenancePlanVO findPlanVO(Long id);
    @Select("SELECT id,plan_id,item_name,standard_description,sort_no,required FROM pm_plan_item WHERE plan_id=#{planId} ORDER BY sort_no,id") List<MaintenancePlanItemVO> findPlanItems(Long planId);
    @Select("SELECT id,plan_no,plan_name,equipment_id,cycle_type,cycle_value,next_execute_date,running_hour_threshold,status,last_generated_at,created_at,updated_at FROM pm_plan WHERE id=#{id}") MaintenancePlan findPlan(Long id);
    @Select("SELECT id,plan_no,plan_name,equipment_id,cycle_type,cycle_value,next_execute_date,running_hour_threshold,status,last_generated_at,created_at,updated_at FROM pm_plan WHERE id=#{id} FOR UPDATE") MaintenancePlan lockPlan(Long id);
    @Select("SELECT COUNT(*) FROM pm_plan WHERE plan_no=#{no} AND (#{excludeId} IS NULL OR id != #{excludeId})") long countPlanNo(@Param("no") String no,@Param("excludeId") Long excludeId);
    @Insert("INSERT INTO pm_plan(plan_no,plan_name,equipment_id,cycle_type,cycle_value,next_execute_date,running_hour_threshold,status) VALUES(#{planNo},#{planName},#{equipmentId},#{cycleType},#{cycleValue},#{nextExecuteDate},#{runningHourThreshold},#{status})") @Options(useGeneratedKeys=true,keyProperty="id") int insertPlan(MaintenancePlan plan);
    @Update("UPDATE pm_plan SET plan_no=#{planNo},plan_name=#{planName},equipment_id=#{equipmentId},cycle_type=#{cycleType},cycle_value=#{cycleValue},next_execute_date=#{nextExecuteDate},running_hour_threshold=#{runningHourThreshold} WHERE id=#{id}") int updatePlan(MaintenancePlan plan);
    @Update("UPDATE pm_plan SET status=#{status} WHERE id=#{id} AND status != #{status}") int updatePlanStatus(@Param("id") Long id,@Param("status") MaintenancePlanStatus status);
    @Delete("DELETE FROM pm_plan_item WHERE plan_id=#{planId}") int deletePlanItems(Long planId);
    @Insert("INSERT INTO pm_plan_item(plan_id,item_name,standard_description,sort_no,required) VALUES(#{planId},#{itemName},#{standardDescription},#{sortNo},#{required})") @Options(useGeneratedKeys=true,keyProperty="id") int insertPlanItem(MaintenancePlanItem item);
    @Select("SELECT COUNT(*) FROM mnt_work_order WHERE pm_plan_id=#{planId}") long countGeneratedOrders(Long planId);
    @Select("SELECT id FROM pm_plan WHERE status='ENABLED' AND NOT next_execute_date > #{today} ORDER BY next_execute_date,id") List<Long> findDuePlanIds(LocalDate today);
    @Select("SELECT status FROM eqp_equipment WHERE id=#{id}") EquipmentStatus findEquipmentStatus(Long id);
    @Select("SELECT u.id FROM sys_user u JOIN sys_user_role ur ON ur.user_id=u.id JOIN sys_role r ON r.id=ur.role_id WHERE r.role_code='ADMIN' AND u.status='ENABLED' AND r.status='ENABLED' ORDER BY u.id LIMIT 1") Long findSystemOperatorId();
    @Update("UPDATE pm_plan SET next_execute_date=#{nextDate},last_generated_at=NOW() WHERE id=#{id} AND next_execute_date=#{currentDate}") int advancePlan(@Param("id") Long id,@Param("currentDate") LocalDate currentDate,@Param("nextDate") LocalDate nextDate);
    @Update("UPDATE pm_plan SET status='DISABLED' WHERE id=#{id}") int disablePlan(Long id);

    @Select("SELECT p.id plan_id,p.plan_no,p.plan_name FROM mnt_work_order wo JOIN pm_plan p ON p.id=wo.pm_plan_id WHERE wo.id=#{workOrderId}") MaintenancePlanSourceVO findWorkOrderPlan(Long workOrderId);
    @Select("SELECT i.id plan_item_id,i.item_name,i.standard_description,i.sort_no,i.required,x.result,x.measured_value,x.remark,x.executor_id,u.real_name executor_name,x.executed_at FROM mnt_work_order wo JOIN pm_plan_item i ON i.plan_id=wo.pm_plan_id LEFT JOIN pm_execution_item x ON x.work_order_id=wo.id AND x.plan_item_id=i.id LEFT JOIN sys_user u ON u.id=x.executor_id WHERE wo.id=#{workOrderId} ORDER BY i.sort_no,i.id") List<MaintenanceExecutionItemVO> findExecutionItems(Long workOrderId);
    @Select("SELECT COUNT(*) FROM pm_plan_item WHERE id=#{itemId} AND plan_id=#{planId}") long countPlanItem(@Param("planId") Long planId,@Param("itemId") Long itemId);
    @Insert("INSERT INTO pm_execution_item(work_order_id,plan_item_id,result,measured_value,remark,executor_id) VALUES(#{workOrderId},#{itemId},#{result},#{measuredValue},#{remark},#{executorId}) ON DUPLICATE KEY UPDATE result=VALUES(result),measured_value=VALUES(measured_value),remark=VALUES(remark),executor_id=VALUES(executor_id),executed_at=NOW()") int upsertExecution(@Param("workOrderId") Long workOrderId,@Param("itemId") Long itemId,@Param("result") MaintenanceResult result,@Param("measuredValue") String measuredValue,@Param("remark") String remark,@Param("executorId") Long executorId);
    @Select("SELECT COUNT(*) FROM pm_plan_item i JOIN mnt_work_order wo ON wo.pm_plan_id=i.plan_id LEFT JOIN pm_execution_item x ON x.work_order_id=wo.id AND x.plan_item_id=i.id WHERE wo.id=#{workOrderId} AND i.required=1 AND x.id IS NULL") long countIncompleteRequired(Long workOrderId);
    @Select("SELECT wo.id work_order_id,wo.work_order_no,p.id plan_id,p.plan_no,p.plan_name,wo.status,u.real_name engineer_name,wo.created_at,wo.completed_at,COUNT(x.id) completed_items,COALESCE(SUM(x.result='ABNORMAL'),0) abnormal_items FROM mnt_work_order wo JOIN pm_plan p ON p.id=wo.pm_plan_id LEFT JOIN sys_user u ON u.id=wo.assigned_engineer_id LEFT JOIN pm_execution_item x ON x.work_order_id=wo.id WHERE wo.equipment_id=#{equipmentId} AND wo.work_order_type='MAINTENANCE' GROUP BY wo.id,wo.work_order_no,p.id,p.plan_no,p.plan_name,wo.status,u.real_name,wo.created_at,wo.completed_at ORDER BY wo.created_at DESC,wo.id DESC") List<MaintenanceHistoryVO> findEquipmentHistory(Long equipmentId);
    @Select("SELECT wo.id work_order_id,wo.work_order_no,p.id plan_id,p.plan_no,p.plan_name,wo.status,u.real_name engineer_name,wo.created_at,wo.completed_at,COUNT(x.id) completed_items,COALESCE(SUM(x.result='ABNORMAL'),0) abnormal_items FROM mnt_work_order wo JOIN pm_plan p ON p.id=wo.pm_plan_id LEFT JOIN sys_user u ON u.id=wo.assigned_engineer_id LEFT JOIN pm_execution_item x ON x.work_order_id=wo.id WHERE wo.pm_plan_id=#{planId} GROUP BY wo.id,wo.work_order_no,p.id,p.plan_no,p.plan_name,wo.status,u.real_name,wo.created_at,wo.completed_at ORDER BY wo.created_at DESC,wo.id DESC") List<MaintenanceHistoryVO> findPlanHistory(Long planId);
}
