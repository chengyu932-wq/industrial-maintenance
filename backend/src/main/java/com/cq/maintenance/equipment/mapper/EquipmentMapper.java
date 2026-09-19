package com.cq.maintenance.equipment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cq.maintenance.equipment.dto.EquipmentQuery;
import com.cq.maintenance.equipment.entity.Equipment;
import com.cq.maintenance.equipment.entity.EquipmentStatus;
import com.cq.maintenance.equipment.entity.EquipmentType;
import com.cq.maintenance.equipment.vo.EquipmentDetailRow;
import com.cq.maintenance.equipment.vo.EquipmentListVO;
import com.cq.maintenance.equipment.vo.EquipmentStatusLogVO;
import com.cq.maintenance.equipment.vo.RuntimeHoursVO;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface EquipmentMapper extends BaseMapper<Equipment> {
    String JOIN_SQL=" FROM eqp_equipment e JOIN eqp_type t ON t.id=e.type_id JOIN org_station s ON s.id=e.station_id " +
        "JOIN org_line l ON l.id=s.line_id JOIN org_workshop w ON w.id=l.workshop_id " +
        "LEFT JOIN sys_user u ON u.id=e.responsible_user_id LEFT JOIN org_team tm ON tm.id=e.responsible_team_id ";
    String WHERE_SQL="<where>"+
        "<if test='q.keyword != null and q.keyword != &quot;&quot;'> AND (e.equipment_no LIKE CONCAT('%',#{q.keyword},'%') OR e.equipment_name LIKE CONCAT('%',#{q.keyword},'%'))</if>"+
        "<if test='q.typeId != null'> AND e.type_id=#{q.typeId}</if><if test='q.workshopId != null'> AND w.id=#{q.workshopId}</if>"+
        "<if test='q.lineId != null'> AND l.id=#{q.lineId}</if><if test='q.stationId != null'> AND s.id=#{q.stationId}</if>"+
        "<if test='q.status != null'> AND e.status=#{q.status}</if><if test='q.responsibleUserId != null'> AND e.responsible_user_id=#{q.responsibleUserId}</if>"+
        "<if test='scope.allData == false'> AND ("+
          "<choose><when test='scope.mode == &quot;SUPERVISOR&quot;'>"+
            "<if test='scope.workshopId != null'>w.id=#{scope.workshopId}</if><if test='scope.workshopId == null'>1=0</if>"+
            "<if test='scope.teamIds != null and scope.teamIds.size() > 0'> OR e.responsible_team_id IN <foreach collection='scope.teamIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>"+
          "</when><when test='scope.mode == &quot;ENGINEER&quot;'>e.responsible_user_id=#{scope.userId}"+
            "<if test='scope.teamIds != null and scope.teamIds.size() > 0'> OR e.responsible_team_id IN <foreach collection='scope.teamIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>"+
          "</when><otherwise>e.status&lt;&gt;'SCRAPPED'</otherwise></choose>)</if>"+
        "</where>";

    @Select("<script>SELECT e.id,e.equipment_no,e.equipment_name,e.type_id,t.type_code,t.type_name,e.model,e.manufacturer,e.specifications,e.manufacture_date,"+
        "w.id workshop_id,w.workshop_no,w.workshop_name,l.id line_id,l.line_no,l.line_name,s.id station_id,s.station_no,s.station_name,e.responsible_user_id,u.username responsible_username,u.real_name responsible_user_name,"+
        "e.responsible_team_id,tm.team_no responsible_team_no,tm.team_name responsible_team_name,e.commissioning_date,e.warranty_expire_date,e.status,e.qr_code"+JOIN_SQL+WHERE_SQL+
        " ORDER BY e.id DESC LIMIT #{q.size} OFFSET #{q.offset}</script>")
    List<EquipmentListVO> findPage(@Param("q") EquipmentQuery query,@Param("scope") EquipmentDataScope scope);
    @Select("<script>SELECT COUNT(*)"+JOIN_SQL+WHERE_SQL+"</script>") long countPage(@Param("q") EquipmentQuery query,@Param("scope") EquipmentDataScope scope);

    @Select("SELECT e.id,e.equipment_no,e.equipment_name,e.type_id,t.type_code,t.type_name,e.model,e.manufacturer,e.specifications,"+
        "e.manufacture_date,e.commissioning_date,e.responsible_user_id,u.real_name responsible_user_name,e.responsible_team_id,tm.team_name responsible_team_name,"+
        "w.id workshop_id,w.workshop_no,w.workshop_name,l.id line_id,l.line_no,l.line_name,s.id station_id,s.station_no,s.station_name,"+
        "e.warranty_expire_date,e.status,e.running_hours,e.qr_code,e.created_at,e.updated_at"+JOIN_SQL+" WHERE e.id=#{id}")
    EquipmentDetailRow findDetailBase(Long id);
    @Select("SELECT id,equipment_no,equipment_name,type_id,model,manufacturer,specifications,manufacture_date,commissioning_date,"+
        "responsible_user_id,responsible_team_id,station_id,warranty_expire_date,status,running_hours,qr_code,created_at,updated_at FROM eqp_equipment WHERE id=#{id}")
    Equipment findEquipment(Long id);
    @Select("SELECT id,equipment_no,equipment_name,type_id,model,manufacturer,specifications,manufacture_date,commissioning_date,responsible_user_id,responsible_team_id,station_id,warranty_expire_date,status,running_hours,qr_code,created_at,updated_at FROM eqp_equipment WHERE id=#{id} FOR UPDATE") Equipment lockEquipment(Long id);
    @Update("UPDATE eqp_equipment SET running_hours=#{total} WHERE id=#{id}") int updateRunningHours(@Param("id") Long id,@Param("total") java.math.BigDecimal total);
    @Insert("INSERT INTO eqp_runtime_record(equipment_id,record_date,running_hours_increment,total_running_hours,source,recorded_by,remark) VALUES(#{equipmentId},#{recordDate},#{increment},#{total},'MANUAL',#{recordedBy},#{remark})") int insertRuntimeRecord(@Param("equipmentId") Long equipmentId,@Param("recordDate") java.time.LocalDate recordDate,@Param("increment") java.math.BigDecimal increment,@Param("total") java.math.BigDecimal total,@Param("recordedBy") Long recordedBy,@Param("remark") String remark);
    @Select("SELECT r.id,r.equipment_id,r.record_date,r.running_hours_increment,r.total_running_hours,r.source,r.recorded_by,u.real_name recorder_name,r.remark,r.created_at FROM eqp_runtime_record r LEFT JOIN sys_user u ON u.id=r.recorded_by WHERE r.equipment_id=#{equipmentId} ORDER BY r.record_date DESC,r.id DESC") List<RuntimeHoursVO> findRuntimeRecords(Long equipmentId);
    @Select("SELECT COUNT(*) FROM eqp_equipment WHERE equipment_no=#{equipmentNo} AND (#{excludeId} IS NULL OR id<>#{excludeId})")
    long countEquipmentNo(@Param("equipmentNo") String equipmentNo,@Param("excludeId") Long excludeId);
    @Insert("INSERT INTO eqp_equipment(equipment_no,equipment_name,type_id,model,manufacturer,specifications,manufacture_date,commissioning_date,"+
        "responsible_user_id,responsible_team_id,station_id,warranty_expire_date,status,running_hours,qr_code) VALUES(#{equipmentNo},#{equipmentName},#{typeId},"+
        "#{model},#{manufacturer},#{specifications},#{manufactureDate},#{commissioningDate},#{responsibleUserId},#{responsibleTeamId},#{stationId},#{warrantyExpireDate},#{status},0,#{qrCode})")
    @Options(useGeneratedKeys=true,keyProperty="id") int insertEquipment(Equipment e);
    @Update("UPDATE eqp_equipment SET equipment_no=#{equipmentNo},equipment_name=#{equipmentName},type_id=#{typeId},model=#{model},manufacturer=#{manufacturer},"+
        "specifications=#{specifications},manufacture_date=#{manufactureDate},commissioning_date=#{commissioningDate},responsible_user_id=#{responsibleUserId},"+
        "responsible_team_id=#{responsibleTeamId},station_id=#{stationId},warranty_expire_date=#{warrantyExpireDate} WHERE id=#{id}") int updateEquipment(Equipment e);
    @Update("UPDATE eqp_equipment SET status=#{target} WHERE id=#{id} AND status=#{current}") int updateStatus(@Param("id") Long id,@Param("current") EquipmentStatus current,@Param("target") EquipmentStatus target);
    @Insert("INSERT INTO eqp_status_log(equipment_id,from_status,to_status,source_type,source_id,reason,operator_id) VALUES(#{equipmentId},#{fromStatus},#{toStatus},#{sourceType},#{sourceId},#{reason},#{operatorId})")
    int insertStatusLog(@Param("equipmentId") Long equipmentId,@Param("fromStatus") EquipmentStatus fromStatus,@Param("toStatus") EquipmentStatus toStatus,
        @Param("sourceType") String sourceType,@Param("sourceId") Long sourceId,@Param("reason") String reason,@Param("operatorId") Long operatorId);
    @Select("SELECT l.id,l.equipment_id,l.from_status,l.to_status,l.source_type,l.source_id,l.reason,l.operator_id,u.real_name operator_name,l.changed_at " +
        "FROM eqp_status_log l LEFT JOIN sys_user u ON u.id=l.operator_id WHERE l.equipment_id=#{equipmentId} ORDER BY l.changed_at DESC,l.id DESC")
    List<EquipmentStatusLogVO> findStatusHistory(Long equipmentId);

    @Select("SELECT id,type_code,type_name,description,status FROM eqp_type ORDER BY type_code") List<EquipmentType> findTypes();
    @Select("SELECT id,type_code,type_name,description,status FROM eqp_type WHERE id=#{id}") EquipmentType findType(Long id);
    @Select("SELECT id,type_code,type_name,description,status FROM eqp_type WHERE type_code=#{code} AND status='ENABLED'") EquipmentType findTypeByCode(String code);
    @Select("SELECT COUNT(*) FROM eqp_type WHERE type_code=#{code} AND (#{excludeId} IS NULL OR id<>#{excludeId})") long countTypeCode(@Param("code") String code,@Param("excludeId") Long excludeId);
    @Insert("INSERT INTO eqp_type(type_code,type_name,description,status) VALUES(#{typeCode},#{typeName},#{description},#{status})") int insertType(EquipmentType type);
    @Update("UPDATE eqp_type SET type_code=#{typeCode},type_name=#{typeName},description=#{description},status=#{status} WHERE id=#{id}") int updateType(EquipmentType type);
    @Delete("DELETE FROM eqp_type WHERE id=#{id}") int deleteType(Long id);
    @Select("SELECT COUNT(*) FROM eqp_equipment WHERE type_id=#{id}") long countEquipmentByType(Long id);
    @Select("SELECT COUNT(*) FROM org_station WHERE id=#{id} AND status='ENABLED'") long countEnabledStation(Long id);
    @Select("SELECT COUNT(*) FROM sys_user WHERE id=#{id} AND status='ENABLED'") long countEnabledUser(Long id);
    @Select("SELECT COUNT(*) FROM org_team WHERE id=#{id} AND status='ENABLED'") long countEnabledTeam(Long id);
    @Select("SELECT l.workshop_id FROM org_station s JOIN org_line l ON l.id=s.line_id WHERE s.id=#{stationId}") Long findWorkshopIdByStation(Long stationId);
    @Select("SELECT s.id FROM org_station s JOIN org_line l ON l.id=s.line_id JOIN org_workshop w ON w.id=l.workshop_id " +
        "WHERE w.workshop_no=#{workshopNo} AND l.line_no=#{lineNo} AND s.station_no=#{stationNo} AND w.status='ENABLED' AND l.status='ENABLED' AND s.status='ENABLED'")
    Long findStationIdByCodes(@Param("workshopNo") String workshopNo,@Param("lineNo") String lineNo,@Param("stationNo") String stationNo);
    @Select("SELECT id FROM sys_user WHERE username=#{username} AND status='ENABLED'") Long findUserIdByUsername(String username);
    @Select("SELECT id FROM org_team WHERE team_no=#{teamNo} AND status='ENABLED'") Long findTeamIdByCode(String teamNo);

    record EquipmentDataScope(boolean allData,String mode,Long userId,Long workshopId,List<Long> teamIds) {}
}
