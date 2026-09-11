package com.cq.maintenance.organization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cq.maintenance.organization.entity.Workshop;
import com.cq.maintenance.organization.vo.LineVO;
import com.cq.maintenance.organization.vo.StationVO;
import com.cq.maintenance.organization.vo.WorkshopVO;
import com.cq.maintenance.organization.vo.TeamVO;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrganizationMapper extends BaseMapper<Workshop> {
    @Select("<script>SELECT w.id,w.workshop_no,w.workshop_name,w.manager_id,u.real_name manager_name,w.status " +
        "FROM org_workshop w LEFT JOIN sys_user u ON u.id=w.manager_id " +
        "<where><if test='status != null and status != &quot;&quot;'>w.status=#{status}</if></where> ORDER BY w.workshop_no</script>")
    List<WorkshopVO> findWorkshops(@Param("status") String status);

    @Select("SELECT id,workshop_no,workshop_name,manager_id,status,created_at,updated_at FROM org_workshop WHERE id=#{id}")
    Workshop findWorkshop(Long id);

    @Select("SELECT COUNT(*) FROM org_workshop WHERE workshop_no=#{code} AND (#{excludeId} IS NULL OR id<>#{excludeId})")
    long countWorkshopCode(@Param("code") String code, @Param("excludeId") Long excludeId);

    @Insert("INSERT INTO org_workshop(workshop_no,workshop_name,manager_id,status) VALUES(#{workshopNo},#{workshopName},#{managerId},#{status})")
    int insertWorkshop(Workshop workshop);

    @Update("UPDATE org_workshop SET workshop_no=#{workshopNo},workshop_name=#{workshopName},manager_id=#{managerId},status=#{status} WHERE id=#{id}")
    int updateWorkshop(Workshop workshop);

    @Delete("DELETE FROM org_workshop WHERE id=#{id}") int deleteWorkshop(Long id);
    @Select("SELECT COUNT(*) FROM org_line WHERE workshop_id=#{id}") long countLines(Long id);
    @Select("SELECT COUNT(*) FROM org_team WHERE workshop_id=#{id}") long countTeams(Long id);

    @Select("<script>SELECT l.id,l.workshop_id,w.workshop_no,w.workshop_name,l.line_no,l.line_name,l.status " +
        "FROM org_line l JOIN org_workshop w ON w.id=l.workshop_id <where>" +
        "<if test='workshopId != null'>l.workshop_id=#{workshopId}</if>" +
        "<if test='status != null and status != &quot;&quot;'> AND l.status=#{status}</if>" +
        "</where> ORDER BY w.workshop_no,l.line_no</script>")
    List<LineVO> findLines(@Param("workshopId") Long workshopId, @Param("status") String status);

    @Select("SELECT id,workshop_id workshopId,line_no lineNo,line_name lineName,status FROM org_line WHERE id=#{id}")
    java.util.Map<String,Object> findLine(Long id);
    @Select("SELECT COUNT(*) FROM org_line WHERE workshop_id=#{workshopId} AND line_no=#{code} AND (#{excludeId} IS NULL OR id<>#{excludeId})")
    long countLineCode(@Param("workshopId") Long workshopId,@Param("code") String code,@Param("excludeId") Long excludeId);
    @Insert("INSERT INTO org_line(workshop_id,line_no,line_name,status) VALUES(#{workshopId},#{lineNo},#{lineName},#{status})")
    int insertLine(@Param("workshopId") Long workshopId,@Param("lineNo") String lineNo,@Param("lineName") String lineName,@Param("status") String status);
    @Update("UPDATE org_line SET workshop_id=#{workshopId},line_no=#{lineNo},line_name=#{lineName},status=#{status} WHERE id=#{id}")
    int updateLine(@Param("id") Long id,@Param("workshopId") Long workshopId,@Param("lineNo") String lineNo,@Param("lineName") String lineName,@Param("status") String status);
    @Delete("DELETE FROM org_line WHERE id=#{id}") int deleteLine(Long id);
    @Select("SELECT COUNT(*) FROM org_station WHERE line_id=#{id}") long countStations(Long id);

    @Select("<script>SELECT s.id,s.line_id,l.line_no,l.line_name,w.id workshop_id,w.workshop_no,w.workshop_name," +
        "s.station_no,s.station_name,s.status FROM org_station s JOIN org_line l ON l.id=s.line_id " +
        "JOIN org_workshop w ON w.id=l.workshop_id <where>" +
        "<if test='lineId != null'>s.line_id=#{lineId}</if><if test='status != null and status != &quot;&quot;'> AND s.status=#{status}</if>" +
        "</where> ORDER BY w.workshop_no,l.line_no,s.station_no</script>")
    List<StationVO> findStations(@Param("lineId") Long lineId,@Param("status") String status);
    @Select("SELECT id,line_id lineId,station_no stationNo,station_name stationName,status FROM org_station WHERE id=#{id}")
    java.util.Map<String,Object> findStation(Long id);
    @Select("SELECT COUNT(*) FROM org_station WHERE line_id=#{lineId} AND station_no=#{code} AND (#{excludeId} IS NULL OR id<>#{excludeId})")
    long countStationCode(@Param("lineId") Long lineId,@Param("code") String code,@Param("excludeId") Long excludeId);
    @Insert("INSERT INTO org_station(line_id,station_no,station_name,status) VALUES(#{lineId},#{stationNo},#{stationName},#{status})")
    int insertStation(@Param("lineId") Long lineId,@Param("stationNo") String stationNo,@Param("stationName") String stationName,@Param("status") String status);
    @Update("UPDATE org_station SET line_id=#{lineId},station_no=#{stationNo},station_name=#{stationName},status=#{status} WHERE id=#{id}")
    int updateStation(@Param("id") Long id,@Param("lineId") Long lineId,@Param("stationNo") String stationNo,@Param("stationName") String stationName,@Param("status") String status);
    @Delete("DELETE FROM org_station WHERE id=#{id}") int deleteStation(Long id);
    @Select("SELECT COUNT(*) FROM eqp_equipment WHERE station_id=#{id}") long countEquipment(Long id);

    @Select("SELECT COUNT(*) FROM sys_user WHERE id=#{id} AND status='ENABLED'") long countEnabledUser(Long id);
    @Select("<script>SELECT t.id,t.team_no,t.team_name,t.workshop_id,w.workshop_name,t.leader_id,u.real_name leader_name,t.status " +
        "FROM org_team t JOIN org_workshop w ON w.id=t.workshop_id LEFT JOIN sys_user u ON u.id=t.leader_id <where>"+
        "<if test='workshopId != null'>t.workshop_id=#{workshopId}</if><if test='status != null and status != &quot;&quot;'> AND t.status=#{status}</if>"+
        "</where> ORDER BY t.team_no</script>") List<TeamVO> findTeams(@Param("workshopId") Long workshopId,@Param("status") String status);
}
