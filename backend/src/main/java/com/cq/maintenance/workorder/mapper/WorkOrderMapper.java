package com.cq.maintenance.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cq.maintenance.equipment.entity.Equipment;
import com.cq.maintenance.repair.dto.RepairRequestQuery;
import com.cq.maintenance.repair.entity.RepairRequest;
import com.cq.maintenance.repair.vo.RepairRequestVO;
import com.cq.maintenance.workorder.dto.*;
import com.cq.maintenance.workorder.entity.*;
import com.cq.maintenance.workorder.vo.*;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface WorkOrderMapper extends BaseMapper<WorkOrder> {
    String LIST_FROM=" FROM mnt_work_order wo JOIN eqp_equipment e ON e.id=wo.equipment_id "+
        "JOIN org_station st ON st.id=e.station_id JOIN org_line l ON l.id=st.line_id JOIN org_workshop w ON w.id=l.workshop_id "+
        "LEFT JOIN mnt_repair_request rr ON rr.id=wo.repair_request_id LEFT JOIN sys_user reporter ON reporter.id=rr.reporter_id "+
        "LEFT JOIN sys_user engineer ON engineer.id=wo.assigned_engineer_id LEFT JOIN org_team team ON team.id=wo.assigned_team_id ";
    String LIST_COLUMNS="wo.id,wo.work_order_no,wo.work_order_type,wo.repair_request_id,rr.request_no,wo.equipment_id,e.equipment_no,e.equipment_name,"+
        "w.id workshop_id,w.workshop_name,wo.priority,wo.status,rr.reporter_id,reporter.real_name reporter_name,wo.assigned_engineer_id,"+
        "engineer.real_name assigned_engineer_name,wo.assigned_team_id,team.team_name assigned_team_name,wo.created_at,wo.assigned_at,wo.accepted_at,"+
        "wo.started_at,wo.submitted_at,wo.completed_at,wo.acceptance_return_count,wo.pm_plan_id,wo.sla_rule_id,wo.sla_response_deadline,wo.sla_resolve_deadline";
    String SCOPE_SQL="<if test='scope.allData == false'> AND (<choose>"+
        "<when test='scope.mode == &quot;SUPERVISOR&quot;'><if test='scope.workshopId != null'>w.id=#{scope.workshopId}</if><if test='scope.workshopId == null'>1=0</if>"+
        "<if test='scope.teamIds != null and scope.teamIds.size() > 0'> OR wo.assigned_team_id IN <foreach collection='scope.teamIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> OR e.responsible_team_id IN <foreach collection='scope.teamIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if></when>"+
        "<when test='scope.mode == &quot;ENGINEER&quot;'>wo.assigned_engineer_id=#{scope.userId}<if test='scope.teamIds != null and scope.teamIds.size() > 0'> OR wo.assigned_team_id IN <foreach collection='scope.teamIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if></when>"+
        "<when test='scope.mode == &quot;REPORTER&quot;'>rr.reporter_id=#{scope.userId}</when><otherwise>1=0</otherwise></choose>)</if>";
    String QUERY_SQL="<if test='q.workOrderNo != null and q.workOrderNo != &quot;&quot;'> AND wo.work_order_no LIKE CONCAT('%',#{q.workOrderNo},'%')</if>"+
        "<if test='q.status != null'> AND wo.status=#{q.status}</if><if test='q.type != null'> AND wo.work_order_type=#{q.type}</if>"+
        "<if test='q.equipmentId != null'> AND wo.equipment_id=#{q.equipmentId}</if><if test='q.equipmentKeyword != null and q.equipmentKeyword != &quot;&quot;'> AND (e.equipment_no LIKE CONCAT('%',#{q.equipmentKeyword},'%') OR e.equipment_name LIKE CONCAT('%',#{q.equipmentKeyword},'%'))</if>"+
        "<if test='q.priority != null'> AND wo.priority=#{q.priority}</if><if test='q.reporterId != null'> AND rr.reporter_id=#{q.reporterId}</if>"+
        "<if test='q.engineerId != null'> AND wo.assigned_engineer_id=#{q.engineerId}</if><if test='q.teamId != null'> AND wo.assigned_team_id=#{q.teamId}</if>"+
        "<if test='q.workshopId != null'> AND w.id=#{q.workshopId}</if><if test='q.startDate != null'> AND DATE(wo.created_at)&gt;=#{q.startDate}</if>"+
        "<if test='q.endDate != null'> AND DATE(wo.created_at)&lt;=#{q.endDate}</if>";

    @Select("SELECT id,equipment_no,equipment_name,type_id,model,manufacturer,specifications,manufacture_date,commissioning_date,responsible_user_id,responsible_team_id,station_id,warranty_expire_date,status,running_hours,qr_code,created_at,updated_at FROM eqp_equipment WHERE id=#{id} FOR UPDATE")
    Equipment lockEquipment(Long id);

    @Select("SELECT COUNT(*) FROM mnt_work_order WHERE equipment_id=#{equipmentId} AND work_order_type='REPAIR' AND status IN ('PENDING_ASSIGN','ASSIGNED','PROCESSING','SUSPENDED','PENDING_ACCEPT') AND (#{excludeId} IS NULL OR id != #{excludeId})")
    long countActiveRepairOrders(@Param("equipmentId") Long equipmentId,@Param("excludeId") Long excludeId);

    @Insert("INSERT INTO mnt_repair_request(request_no,equipment_id,reporter_id,source,priority,fault_description,status) VALUES(#{requestNo},#{equipmentId},#{reporterId},#{source},#{priority},#{faultDescription},#{status})")
    @Options(useGeneratedKeys=true,keyProperty="id") int insertRepairRequest(RepairRequest request);
    @Update("UPDATE mnt_repair_request SET status='CONVERTED' WHERE id=#{id} AND status='SUBMITTED'") int markRepairConverted(Long id);
    @Update("UPDATE mnt_repair_request SET status='CANCELLED',cancelled_at=NOW(),cancel_reason=#{reason} WHERE id=#{id} AND status != 'CANCELLED'") int cancelRepairRequest(@Param("id") Long id,@Param("reason") String reason);

    @Insert("INSERT INTO mnt_work_order(work_order_no,work_order_type,repair_request_id,pm_plan_id,equipment_id,priority,status,created_by) VALUES(#{workOrderNo},#{workOrderType},#{repairRequestId},#{pmPlanId},#{equipmentId},#{priority},#{status},#{createdBy})")
    @Options(useGeneratedKeys=true,keyProperty="id") int insertWorkOrder(WorkOrder order);

    @Select("SELECT id,work_order_no,work_order_type,repair_request_id,pm_plan_id,equipment_id,priority,status,assigned_engineer_id,assigned_team_id,created_at,assigned_at,accepted_at,started_at,submitted_at,completed_at,cancelled_at,cancel_reason,sla_rule_id,sla_response_deadline,sla_resolve_deadline,acceptance_return_count,created_by,updated_at FROM mnt_work_order WHERE id=#{id} FOR UPDATE")
    WorkOrder lockWorkOrder(Long id);
    @Select("SELECT id,work_order_no,work_order_type,repair_request_id,pm_plan_id,equipment_id,priority,status,assigned_engineer_id,assigned_team_id,created_at,assigned_at,accepted_at,started_at,submitted_at,completed_at,cancelled_at,cancel_reason,sla_rule_id,sla_response_deadline,sla_resolve_deadline,acceptance_return_count,created_by,updated_at FROM mnt_work_order WHERE id=#{id}")
    WorkOrder findWorkOrder(Long id);

    @Select("<script>SELECT "+LIST_COLUMNS+LIST_FROM+"<where>1=1 "+QUERY_SQL+SCOPE_SQL+"</where> ORDER BY wo.created_at DESC,wo.id DESC LIMIT #{q.size} OFFSET #{q.offset}</script>")
    List<WorkOrderListVO> findWorkOrderPage(@Param("q") WorkOrderQuery query,@Param("scope") WorkOrderDataScope scope);
    @Select("<script>SELECT COUNT(*)"+LIST_FROM+"<where>1=1 "+QUERY_SQL+SCOPE_SQL+"</where></script>")
    long countWorkOrderPage(@Param("q") WorkOrderQuery query,@Param("scope") WorkOrderDataScope scope);
    @Select("SELECT "+LIST_COLUMNS+LIST_FROM+" WHERE wo.id=#{id}") WorkOrderListVO findWorkOrderSummary(Long id);
    @Select("<script>SELECT COUNT(*)"+LIST_FROM+" WHERE wo.id=#{id} "+SCOPE_SQL+"</script>") long countVisible(@Param("id") Long id,@Param("scope") WorkOrderDataScope scope);

    @Select("<script>SELECT rr.id,rr.request_no,rr.equipment_id,e.equipment_no,e.equipment_name,rr.reporter_id,u.real_name reporter_name,rr.source,rr.priority,rr.fault_description,rr.reported_at,rr.status,wo.id work_order_id,wo.work_order_no,wo.status work_order_status FROM mnt_repair_request rr JOIN eqp_equipment e ON e.id=rr.equipment_id JOIN org_station st ON st.id=e.station_id JOIN org_line l ON l.id=st.line_id JOIN org_workshop w ON w.id=l.workshop_id LEFT JOIN sys_user u ON u.id=rr.reporter_id LEFT JOIN mnt_work_order wo ON wo.repair_request_id=rr.id WHERE rr.id=#{id} <if test='scope.allData == false'> AND (<choose><when test='scope.mode == &quot;SUPERVISOR&quot;'><if test='scope.workshopId != null'>w.id=#{scope.workshopId}</if><if test='scope.workshopId == null'>1=0</if><if test='scope.teamIds != null and scope.teamIds.size() > 0'> OR e.responsible_team_id IN <foreach collection='scope.teamIds' item='tid' open='(' separator=',' close=')'>#{tid}</foreach></if></when><when test='scope.mode == &quot;ENGINEER&quot;'>wo.assigned_engineer_id=#{scope.userId}<if test='scope.teamIds != null and scope.teamIds.size() > 0'> OR wo.assigned_team_id IN <foreach collection='scope.teamIds' item='tid' open='(' separator=',' close=')'>#{tid}</foreach></if></when><when test='scope.mode == &quot;REPORTER&quot;'>rr.reporter_id=#{scope.userId}</when><otherwise>1=0</otherwise></choose>)</if></script>")
    RepairRequestVO findRepairRequest(@Param("id") Long id,@Param("scope") WorkOrderDataScope scope);
    @Select("<script>SELECT rr.id,rr.request_no,rr.equipment_id,e.equipment_no,e.equipment_name,rr.reporter_id,u.real_name reporter_name,rr.source,rr.priority,rr.fault_description,rr.reported_at,rr.status,wo.id work_order_id,wo.work_order_no,wo.status work_order_status FROM mnt_repair_request rr JOIN eqp_equipment e ON e.id=rr.equipment_id JOIN org_station st ON st.id=e.station_id JOIN org_line l ON l.id=st.line_id JOIN org_workshop w ON w.id=l.workshop_id LEFT JOIN sys_user u ON u.id=rr.reporter_id LEFT JOIN mnt_work_order wo ON wo.repair_request_id=rr.id WHERE 1=1 <if test='q.requestNo != null and q.requestNo != &quot;&quot;'> AND rr.request_no LIKE CONCAT('%',#{q.requestNo},'%')</if><if test='q.equipmentId != null'> AND rr.equipment_id=#{q.equipmentId}</if><if test='q.priority != null'> AND rr.priority=#{q.priority}</if><if test='q.status != null'> AND rr.status=#{q.status}</if> "+SCOPE_SQL+" ORDER BY rr.reported_at DESC,rr.id DESC LIMIT #{q.size} OFFSET #{q.offset}</script>")
    List<RepairRequestVO> findRepairPage(@Param("q") RepairRequestQuery query,@Param("scope") WorkOrderDataScope scope);
    @Select("<script>SELECT COUNT(*) FROM mnt_repair_request rr JOIN eqp_equipment e ON e.id=rr.equipment_id JOIN org_station st ON st.id=e.station_id JOIN org_line l ON l.id=st.line_id JOIN org_workshop w ON w.id=l.workshop_id LEFT JOIN mnt_work_order wo ON wo.repair_request_id=rr.id WHERE 1=1 <if test='q.requestNo != null and q.requestNo != &quot;&quot;'> AND rr.request_no LIKE CONCAT('%',#{q.requestNo},'%')</if><if test='q.equipmentId != null'> AND rr.equipment_id=#{q.equipmentId}</if><if test='q.priority != null'> AND rr.priority=#{q.priority}</if><if test='q.status != null'> AND rr.status=#{q.status}</if> "+SCOPE_SQL+"</script>")
    long countRepairPage(@Param("q") RepairRequestQuery query,@Param("scope") WorkOrderDataScope scope);

    @Update("UPDATE mnt_work_order SET status='ASSIGNED',assigned_engineer_id=#{engineerId},assigned_team_id=#{teamId},assigned_at=NOW() WHERE id=#{id} AND status='PENDING_ASSIGN'")
    int assign(@Param("id") Long id,@Param("engineerId") Long engineerId,@Param("teamId") Long teamId);
    @Update("UPDATE mnt_work_order SET accepted_at=NOW() WHERE id=#{id} AND status='ASSIGNED' AND accepted_at IS NULL") int acceptResponse(Long id);
    @Update("<script>UPDATE mnt_work_order SET status=#{target}<if test='action == &quot;START&quot;'>,started_at=NOW()</if><if test='action == &quot;SUBMIT&quot;'>,submitted_at=NOW()</if><if test='action == &quot;ACCEPT_PASS&quot;'>,completed_at=NOW()</if><if test='action == &quot;ACCEPT_RETURN&quot;'>,acceptance_return_count=acceptance_return_count+1</if> WHERE id=#{id} AND status=#{current}</script>")
    int updateState(@Param("id") Long id,@Param("current") WorkOrderStatus current,@Param("target") WorkOrderStatus target,@Param("action") String action);
    @Update("UPDATE mnt_work_order SET status='CANCELLED',cancelled_at=NOW(),cancel_reason=#{reason} WHERE id=#{id} AND status=#{current}")
    int cancel(@Param("id") Long id,@Param("current") WorkOrderStatus current,@Param("reason") String reason);
    @Insert("INSERT INTO mnt_work_order_flow(work_order_id,from_status,to_status,action,operator_id,remark) VALUES(#{workOrderId},#{fromStatus},#{toStatus},#{action},#{operatorId},#{remark})")
    int insertFlow(@Param("workOrderId") Long workOrderId,@Param("fromStatus") WorkOrderStatus fromStatus,@Param("toStatus") WorkOrderStatus toStatus,@Param("action") String action,@Param("operatorId") Long operatorId,@Param("remark") String remark);
    @Insert("INSERT INTO mnt_assignment_log(work_order_id,old_engineer_id,new_engineer_id,old_team_id,new_team_id,assignment_type,reason,operator_id) VALUES(#{workOrderId},NULL,#{engineerId},NULL,#{teamId},'MANUAL',#{reason},#{operatorId})")
    int insertAssignment(@Param("workOrderId") Long workOrderId,@Param("engineerId") Long engineerId,@Param("teamId") Long teamId,@Param("reason") String reason,@Param("operatorId") Long operatorId);

    @Select("SELECT u.id,u.username,u.real_name,u.team_id,t.team_name,u.workshop_id FROM sys_user u JOIN sys_user_role ur ON ur.user_id=u.id JOIN sys_role r ON r.id=ur.role_id LEFT JOIN org_team t ON t.id=u.team_id WHERE r.role_code='ENGINEER' AND r.status='ENABLED' AND u.status='ENABLED' ORDER BY u.real_name")
    List<EngineerOptionVO> findEngineers();
    @Select("SELECT u.id,u.username,u.real_name,u.team_id,t.team_name,u.workshop_id FROM sys_user u JOIN sys_user_role ur ON ur.user_id=u.id JOIN sys_role r ON r.id=ur.role_id LEFT JOIN org_team t ON t.id=u.team_id WHERE u.id=#{id} AND r.role_code='ENGINEER' AND r.status='ENABLED' AND u.status='ENABLED'")
    EngineerOptionVO findEngineer(Long id);
    @Select("SELECT COUNT(*) FROM org_team WHERE id=#{id} AND status='ENABLED'") long countEnabledTeam(Long id);

    @Select("SELECT id,work_order_id,inspection_process,root_cause,repair_action,repair_result,labor_hours,downtime_minutes,repairable,created_by,created_at,updated_at FROM mnt_repair_record WHERE work_order_id=#{workOrderId}") RepairRecord findRepairRecord(Long workOrderId);
    @Select("SELECT r.id,r.work_order_id,r.inspection_process,r.root_cause,r.repair_action,r.repair_result,r.labor_hours,r.downtime_minutes,r.repairable,r.created_by,u.real_name engineer_name,r.created_at,r.updated_at FROM mnt_repair_record r LEFT JOIN sys_user u ON u.id=r.created_by WHERE r.work_order_id=#{workOrderId}") RepairRecordVO findRepairRecordVO(Long workOrderId);
    @Insert("INSERT INTO mnt_repair_record(work_order_id,inspection_process,root_cause,repair_action,repair_result,labor_hours,downtime_minutes,repairable,created_by) VALUES(#{workOrderId},#{inspectionProcess},#{rootCause},#{repairAction},#{repairResult},#{laborHours},#{downtimeMinutes},#{repairable},#{createdBy})")
    @Options(useGeneratedKeys=true,keyProperty="id") int insertRepairRecord(RepairRecord record);
    @Update("UPDATE mnt_repair_record SET inspection_process=#{inspectionProcess},root_cause=#{rootCause},repair_action=#{repairAction},repair_result=#{repairResult},labor_hours=#{laborHours},downtime_minutes=#{downtimeMinutes},repairable=#{repairable} WHERE work_order_id=#{workOrderId}") int updateRepairRecord(RepairRecord record);
    @Select("SELECT f.id,f.work_order_id,f.from_status,f.to_status,f.action,f.operator_id,u.real_name operator_name,f.remark,f.created_at FROM mnt_work_order_flow f LEFT JOIN sys_user u ON u.id=f.operator_id WHERE f.work_order_id=#{workOrderId} ORDER BY f.created_at,f.id") List<WorkOrderFlowVO> findFlows(Long workOrderId);

    record WorkOrderDataScope(boolean allData,String mode,Long userId,Long workshopId,List<Long> teamIds) {}
}
