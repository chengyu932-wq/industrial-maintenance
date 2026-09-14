package com.cq.maintenance.dispatch.mapper;

import com.cq.maintenance.dispatch.vo.*;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface DispatchMapper {
    @Select("SELECT wo.id work_order_id,wo.work_order_no,wo.work_order_type,wo.status,e.type_id equipment_type_id,e.responsible_team_id,w.id workshop_id FROM mnt_work_order wo JOIN eqp_equipment e ON e.id=wo.equipment_id JOIN org_station st ON st.id=e.station_id JOIN org_line l ON l.id=st.line_id JOIN org_workshop w ON w.id=l.workshop_id WHERE wo.id=#{id}")
    DispatchContext findContext(Long id);

    @Select("SELECT DISTINCT u.id engineer_id,u.real_name engineer_name,u.team_id,t.team_name,COALESCE(u.workshop_id,t.workshop_id) workshop_id,(SELECT COUNT(*) FROM mnt_work_order x WHERE x.assigned_engineer_id=u.id AND x.status IN ('ASSIGNED','PROCESSING','SUSPENDED','PENDING_ACCEPT')) active_work_order_count FROM sys_user u JOIN sys_user_role ur ON ur.user_id=u.id JOIN sys_role r ON r.id=ur.role_id LEFT JOIN org_team t ON t.id=u.team_id WHERE u.status='ENABLED' AND r.role_code='ENGINEER' AND r.status='ENABLED' AND (t.id IS NULL OR t.status='ENABLED') ORDER BY u.id")
    List<DispatchEngineerRow> findEligibleEngineers();

    @Select("SELECT s.id skill_id,s.skill_name FROM eqp_type_skill ts JOIN org_skill s ON s.id=ts.skill_id WHERE ts.type_id=#{typeId} AND s.status='ENABLED' ORDER BY s.id")
    List<SkillMatchRow> findRequiredSkills(Long typeId);

    @Select("SELECT s.id skill_id,s.skill_name FROM org_user_skill us JOIN org_skill s ON s.id=us.skill_id WHERE us.user_id=#{userId} AND s.status='ENABLED' ORDER BY s.id")
    List<SkillMatchRow> findEngineerSkills(Long userId);
}
