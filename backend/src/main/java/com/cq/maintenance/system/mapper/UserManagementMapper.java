package com.cq.maintenance.system.mapper;
import com.cq.maintenance.system.entity.SysUser;
import com.cq.maintenance.system.vo.*;
import java.util.List;
import org.apache.ibatis.annotations.*;
@Mapper public interface UserManagementMapper {
 @Select("SELECT id,username,password_hash,real_name,phone,email,job_title,team_id,workshop_id,status,failed_login_count,last_login_at,created_at,updated_at FROM sys_user WHERE id=#{id}") SysUser find(Long id);
 @Select("SELECT COUNT(*) FROM sys_user WHERE username=#{username}") long countUsername(String username);
 @Insert("INSERT INTO sys_user(username,password_hash,real_name,phone,email,job_title,team_id,workshop_id,status) VALUES(#{username},#{passwordHash},#{realName},#{phone},#{email},#{jobTitle},#{teamId},#{workshopId},#{status})") @Options(useGeneratedKeys=true,keyProperty="id") int insert(SysUser user);
 @Update("UPDATE sys_user SET real_name=#{realName},phone=#{phone},email=#{email},job_title=#{jobTitle},team_id=#{teamId},workshop_id=#{workshopId} WHERE id=#{id}") int update(SysUser user);
 @Update("UPDATE sys_user SET status=#{status},failed_login_count=0 WHERE id=#{id}") int updateStatus(@Param("id")Long id,@Param("status")String status);
 @Update("UPDATE sys_user SET password_hash=#{hash},failed_login_count=0 WHERE id=#{id}") int resetPassword(@Param("id")Long id,@Param("hash")String hash);
 @Select("SELECT COUNT(*) FROM sys_role WHERE id=#{id} AND status='ENABLED'") long countRole(Long id);
 @Select("SELECT COUNT(*) FROM org_skill WHERE id=#{id} AND status='ENABLED'") long countSkill(Long id);
 @Select("SELECT COUNT(*) FROM org_team WHERE id=#{id} AND status='ENABLED'") long countTeam(Long id);
 @Select("SELECT COUNT(*) FROM org_workshop WHERE id=#{id} AND status='ENABLED'") long countWorkshop(Long id);
 @Delete("DELETE FROM sys_user_role WHERE user_id=#{userId}") int deleteRoles(Long userId);
 @Insert("<script>INSERT INTO sys_user_role(user_id,role_id) VALUES <foreach collection='ids' item='id' separator=','>(#{userId},#{id})</foreach></script>") int insertRoles(@Param("userId")Long userId,@Param("ids")List<Long> ids);
 @Delete("DELETE FROM org_user_skill WHERE user_id=#{userId}") int deleteSkills(Long userId);
 @Insert("<script>INSERT INTO org_user_skill(user_id,skill_id,skill_level) VALUES <foreach collection='ids' item='id' separator=','>(#{userId},#{id},1)</foreach></script>") int insertSkills(@Param("userId")Long userId,@Param("ids")List<Long> ids);
 @Select("SELECT role_id FROM sys_user_role WHERE user_id=#{userId} ORDER BY role_id") List<Long> roleIds(Long userId);
 @Select("SELECT skill_id FROM org_user_skill WHERE user_id=#{userId} ORDER BY skill_id") List<Long> skillIds(Long userId);
 @Select("SELECT id,role_code code,role_name name FROM sys_role WHERE status='ENABLED' ORDER BY id") List<UserMetadataVO.Option> roles();
 @Select("SELECT id,skill_code code,skill_name name FROM org_skill WHERE status='ENABLED' ORDER BY id") List<UserMetadataVO.Option> skills();
 @Select("SELECT id,team_no code,team_name name FROM org_team WHERE status='ENABLED' ORDER BY id") List<UserMetadataVO.Option> teams();
 @Select("SELECT id,workshop_no code,workshop_name name FROM org_workshop WHERE status='ENABLED' ORDER BY id") List<UserMetadataVO.Option> workshops();
}
