package com.cq.maintenance.statistics.mapper;

import com.cq.maintenance.workorder.mapper.WorkOrderMapper.WorkOrderDataScope;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface StatisticsMapper {
    String EQUIPMENT_JOIN = " FROM eqp_equipment e JOIN org_station st ON st.id=e.station_id JOIN org_line l ON l.id=st.line_id JOIN org_workshop w ON w.id=l.workshop_id ";
    String EQUIPMENT_SCOPE = "<if test='scope.allData == false'> AND (<choose>"+
        "<when test='scope.mode == &quot;SUPERVISOR&quot;'><if test='scope.workshopId != null'>w.id=#{scope.workshopId}</if><if test='scope.workshopId == null'>1=0</if>"+
        "<if test='scope.teamIds != null and scope.teamIds.size() > 0'> OR e.responsible_team_id IN <foreach collection='scope.teamIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if></when>"+
        "<when test='scope.mode == &quot;ENGINEER&quot;'>e.responsible_user_id=#{scope.userId}<if test='scope.teamIds != null and scope.teamIds.size() > 0'> OR e.responsible_team_id IN <foreach collection='scope.teamIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if></when>"+
        "<otherwise>1=0</otherwise></choose>)</if>";
    String WORK_ORDER_JOIN = " FROM mnt_work_order wo JOIN eqp_equipment e ON e.id=wo.equipment_id JOIN org_station st ON st.id=e.station_id JOIN org_line l ON l.id=st.line_id JOIN org_workshop w ON w.id=l.workshop_id LEFT JOIN mnt_repair_request rr ON rr.id=wo.repair_request_id ";
    String WORK_ORDER_SCOPE = "<if test='scope.allData == false'> AND (<choose>"+
        "<when test='scope.mode == &quot;SUPERVISOR&quot;'><if test='scope.workshopId != null'>w.id=#{scope.workshopId}</if><if test='scope.workshopId == null'>1=0</if>"+
        "<if test='scope.teamIds != null and scope.teamIds.size() > 0'> OR wo.assigned_team_id IN <foreach collection='scope.teamIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> OR e.responsible_team_id IN <foreach collection='scope.teamIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if></when>"+
        "<when test='scope.mode == &quot;ENGINEER&quot;'>wo.assigned_engineer_id=#{scope.userId}<if test='scope.teamIds != null and scope.teamIds.size() > 0'> OR wo.assigned_team_id IN <foreach collection='scope.teamIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if></when>"+
        "<when test='scope.mode == &quot;REPORTER&quot;'>rr.reporter_id=#{scope.userId}</when><otherwise>1=0</otherwise></choose>)</if>";
    String WAREHOUSE_SCOPE = " AND (#{allWarehouses}=true OR warehouse_id IN <foreach collection='warehouseIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>) ";

    @Select("<script>SELECT COUNT(*)"+EQUIPMENT_JOIN+" WHERE 1=1 "+
        "<if test='status != null'> AND e.status=#{status}</if>"+EQUIPMENT_SCOPE+"</script>")
    long countEquipment(@Param("scope") WorkOrderDataScope scope,@Param("status") String status);

    @Select("<script>SELECT COUNT(*)"+WORK_ORDER_JOIN+" WHERE wo.status NOT IN ('COMPLETED','CANCELLED') "+WORK_ORDER_SCOPE+"</script>")
    long countPendingWorkOrders(@Param("scope") WorkOrderDataScope scope);

    @Select("<script>SELECT COUNT(*)"+WORK_ORDER_JOIN+" WHERE wo.work_order_type='REPAIR' AND wo.status='COMPLETED' AND wo.completed_at&gt;=#{start} AND wo.completed_at&lt;#{end} "+WORK_ORDER_SCOPE+"</script>")
    long countCompletedRepairs(@Param("scope") WorkOrderDataScope scope,@Param("start") LocalDateTime start,@Param("end") LocalDateTime end);

    @Select("<script>SELECT COUNT(*) FROM inv_warning WHERE status='OPEN' "+WAREHOUSE_SCOPE+"</script>")
    long countOpenWarnings(@Param("allWarehouses") boolean allWarehouses,@Param("warehouseIds") List<Long> warehouseIds);

    @Select("<script>SELECT e.status name,COUNT(*) value"+EQUIPMENT_JOIN+" WHERE 1=1 "+EQUIPMENT_SCOPE+" GROUP BY e.status ORDER BY e.status</script>")
    List<NameValueRow> equipmentStatus(@Param("scope") WorkOrderDataScope scope);

    @Select("<script>SELECT wo.status name,COUNT(*) value"+WORK_ORDER_JOIN+" WHERE wo.created_at&gt;=#{start} AND wo.created_at&lt;#{end} "+WORK_ORDER_SCOPE+" GROUP BY wo.status ORDER BY wo.status</script>")
    List<NameValueRow> workOrderStatus(@Param("scope") WorkOrderDataScope scope,@Param("start") LocalDateTime start,@Param("end") LocalDateTime end);

    @Select("<script>SELECT t.type_name name,COUNT(*) value"+WORK_ORDER_JOIN+" JOIN eqp_type t ON t.id=e.type_id WHERE wo.work_order_type='REPAIR' AND wo.status='COMPLETED' AND wo.completed_at&gt;=#{start} AND wo.completed_at&lt;#{end} "+WORK_ORDER_SCOPE+" GROUP BY t.id,t.type_name ORDER BY value DESC,t.id LIMIT 5</script>")
    List<NameValueRow> faultEquipmentTypes(@Param("scope") WorkOrderDataScope scope,@Param("start") LocalDateTime start,@Param("end") LocalDateTime end);

    @Select("<script>SELECT DATE(wo.completed_at) day,COUNT(*) value"+WORK_ORDER_JOIN+" WHERE wo.work_order_type='REPAIR' AND wo.status='COMPLETED' AND wo.completed_at&gt;=#{start} AND wo.completed_at&lt;#{end} "+WORK_ORDER_SCOPE+" GROUP BY DATE(wo.completed_at) ORDER BY day</script>")
    List<DateValueRow> repairTrend(@Param("scope") WorkOrderDataScope scope,@Param("start") LocalDateTime start,@Param("end") LocalDateTime end);

    @Select("<script>SELECT p.spare_name name,GREATEST(SUM(CASE WHEN t.transaction_type IN ('OUTBOUND','ISSUE') THEN -t.qty_change WHEN t.transaction_type='RETURN' THEN -t.qty_change ELSE 0 END),0) value FROM inv_transaction t JOIN inv_spare_part p ON p.id=t.spare_part_id WHERE t.created_at&gt;=#{start} AND t.created_at&lt;#{end} "+
        " AND (#{allWarehouses}=true OR t.warehouse_id IN <foreach collection='warehouseIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>) GROUP BY p.id,p.spare_name HAVING value&gt;0 ORDER BY value DESC,p.id LIMIT 5</script>")
    List<NameValueRow> spareConsumption(@Param("allWarehouses") boolean allWarehouses,@Param("warehouseIds") List<Long> warehouseIds,@Param("start") LocalDateTime start,@Param("end") LocalDateTime end);

    @Select("<script>SELECT CASE WHEN wo.completed_at&lt;=wo.sla_resolve_deadline THEN 'ON_TIME' ELSE 'OVERDUE' END name,COUNT(*) value"+WORK_ORDER_JOIN+" WHERE wo.work_order_type='REPAIR' AND wo.status='COMPLETED' AND wo.sla_resolve_deadline IS NOT NULL AND wo.completed_at&gt;=#{start} AND wo.completed_at&lt;#{end} "+WORK_ORDER_SCOPE+" GROUP BY name ORDER BY name</script>")
    List<NameValueRow> slaCompletion(@Param("scope") WorkOrderDataScope scope,@Param("start") LocalDateTime start,@Param("end") LocalDateTime end);

    @Select("<script>SELECT COALESCE(SUM(r.running_hours_increment),0) FROM eqp_runtime_record r JOIN eqp_equipment e ON e.id=r.equipment_id JOIN org_station st ON st.id=e.station_id JOIN org_line l ON l.id=st.line_id JOIN org_workshop w ON w.id=l.workshop_id WHERE r.record_date&gt;=DATE(#{start}) AND r.record_date&lt;DATE(#{end}) "+EQUIPMENT_SCOPE+"</script>")
    BigDecimal runtimeHours(@Param("scope") WorkOrderDataScope scope,@Param("start") LocalDateTime start,@Param("end") LocalDateTime end);

    @Select("<script>SELECT COALESCE(SUM(CASE WHEN wo.started_at IS NOT NULL AND wo.completed_at&gt;=wo.started_at THEN 1 ELSE 0 END),0) repair_count,COALESCE(SUM(CASE WHEN wo.started_at IS NOT NULL AND wo.completed_at&gt;=wo.started_at THEN TIMESTAMPDIFF(SECOND,wo.started_at,wo.completed_at) ELSE 0 END),0) repair_seconds,COUNT(*) completed_count,COALESCE(SUM(CASE WHEN wo.acceptance_return_count=0 THEN 1 ELSE 0 END),0) first_time_count,COALESCE(SUM(CASE WHEN wo.sla_resolve_deadline IS NOT NULL THEN 1 ELSE 0 END),0) sla_count,COALESCE(SUM(CASE WHEN wo.sla_resolve_deadline IS NOT NULL AND wo.completed_at&lt;=wo.sla_resolve_deadline THEN 1 ELSE 0 END),0) on_time_count"+WORK_ORDER_JOIN+" WHERE wo.work_order_type='REPAIR' AND wo.status='COMPLETED' AND wo.completed_at&gt;=#{start} AND wo.completed_at&lt;#{end} "+WORK_ORDER_SCOPE+"</script>")
    RepairAggregate repairAggregate(@Param("scope") WorkOrderDataScope scope,@Param("start") LocalDateTime start,@Param("end") LocalDateTime end);

    @Select("<script>WITH scoped AS (SELECT e.id"+EQUIPMENT_JOIN+" WHERE 1=1 "+EQUIPMENT_SCOPE+"), ordered AS (SELECT l.equipment_id,l.to_status,l.changed_at,LEAD(l.changed_at) OVER(PARTITION BY l.equipment_id ORDER BY l.changed_at,l.id) next_at FROM eqp_status_log l JOIN scoped s ON s.id=l.equipment_id WHERE l.changed_at&lt;#{end}), clipped AS (SELECT to_status,GREATEST(changed_at,#{start}) interval_start,LEAST(COALESCE(next_at,#{end}),#{end}) interval_end FROM ordered WHERE COALESCE(next_at,#{end})&gt;#{start} AND to_status IN ('RUNNING','FAULT','REPAIRING','STOPPED')) SELECT COALESCE(SUM(CASE WHEN to_status='RUNNING' THEN TIMESTAMPDIFF(SECOND,interval_start,interval_end) ELSE 0 END),0) running_seconds,COALESCE(SUM(TIMESTAMPDIFF(SECOND,interval_start,interval_end)),0) observed_seconds FROM clipped WHERE interval_end&gt;interval_start</script>")
    AvailabilityAggregate availability(@Param("scope") WorkOrderDataScope scope,@Param("start") LocalDateTime start,@Param("end") LocalDateTime end);

    @Select("<script>WITH scoped_stock AS (SELECT s.warehouse_id,s.spare_part_id,s.current_qty FROM inv_stock s WHERE (#{allWarehouses}=true OR s.warehouse_id IN <foreach collection='warehouseIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>)), before_start AS (SELECT t.warehouse_id,t.spare_part_id,t.qty_after,ROW_NUMBER() OVER(PARTITION BY t.warehouse_id,t.spare_part_id ORDER BY t.created_at DESC,t.id DESC) rn FROM inv_transaction t JOIN scoped_stock s ON s.warehouse_id=t.warehouse_id AND s.spare_part_id=t.spare_part_id WHERE t.created_at&lt;#{start}), before_end AS (SELECT t.warehouse_id,t.spare_part_id,t.qty_after,ROW_NUMBER() OVER(PARTITION BY t.warehouse_id,t.spare_part_id ORDER BY t.created_at DESC,t.id DESC) rn FROM inv_transaction t JOIN scoped_stock s ON s.warehouse_id=t.warehouse_id AND s.spare_part_id=t.spare_part_id WHERE t.created_at&lt;#{end}), first_period AS (SELECT t.warehouse_id,t.spare_part_id,t.qty_before,ROW_NUMBER() OVER(PARTITION BY t.warehouse_id,t.spare_part_id ORDER BY t.created_at,t.id) rn FROM inv_transaction t JOIN scoped_stock s ON s.warehouse_id=t.warehouse_id AND s.spare_part_id=t.spare_part_id WHERE t.created_at&gt;=#{start} AND t.created_at&lt;#{end}), consumption AS (SELECT t.warehouse_id,t.spare_part_id,SUM(CASE WHEN t.transaction_type IN ('OUTBOUND','ISSUE') THEN -t.qty_change WHEN t.transaction_type='RETURN' THEN -t.qty_change ELSE 0 END) consumed FROM inv_transaction t JOIN scoped_stock s ON s.warehouse_id=t.warehouse_id AND s.spare_part_id=t.spare_part_id WHERE t.created_at&gt;=#{start} AND t.created_at&lt;#{end} GROUP BY t.warehouse_id,t.spare_part_id), boundaries AS (SELECT s.warehouse_id,s.spare_part_id,COALESCE(bs.qty_after,fp.qty_before,s.current_qty) opening_qty,COALESCE(be.qty_after,s.current_qty) closing_qty,COALESCE(c.consumed,0) consumed FROM scoped_stock s LEFT JOIN before_start bs ON bs.warehouse_id=s.warehouse_id AND bs.spare_part_id=s.spare_part_id AND bs.rn=1 LEFT JOIN before_end be ON be.warehouse_id=s.warehouse_id AND be.spare_part_id=s.spare_part_id AND be.rn=1 LEFT JOIN first_period fp ON fp.warehouse_id=s.warehouse_id AND fp.spare_part_id=s.spare_part_id AND fp.rn=1 LEFT JOIN consumption c ON c.warehouse_id=s.warehouse_id AND c.spare_part_id=s.spare_part_id) SELECT GREATEST(COALESCE(SUM(consumed),0),0) consumed_qty,COALESCE(SUM((opening_qty+closing_qty)/2),0) average_qty,COUNT(*) stock_item_count FROM boundaries</script>")
    InventoryAggregate inventoryTurnover(@Param("allWarehouses") boolean allWarehouses,@Param("warehouseIds") List<Long> warehouseIds,@Param("start") LocalDateTime start,@Param("end") LocalDateTime end);

    record NameValueRow(String name, BigDecimal value) {}
    record DateValueRow(java.time.LocalDate day, long value) {}
    record RepairAggregate(long repairCount, long repairSeconds, long completedCount, long firstTimeCount, long slaCount, long onTimeCount) {}
    record AvailabilityAggregate(long runningSeconds, long observedSeconds) {}
    record InventoryAggregate(BigDecimal consumedQty, BigDecimal averageQty, long stockItemCount) {}
}
