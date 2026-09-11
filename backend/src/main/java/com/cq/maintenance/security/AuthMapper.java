package com.cq.maintenance.security;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cq.maintenance.system.entity.SysUser;
import com.cq.maintenance.system.vo.MenuVO;
import com.cq.maintenance.system.vo.UserSummaryVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AuthMapper extends BaseMapper<SysUser> {
    @Select("SELECT * FROM sys_user WHERE username = #{username} LIMIT 1")
    SysUser findByUsername(@Param("username") String username);

    @Select("SELECT role_code FROM sys_role r JOIN sys_user_role ur ON ur.role_id=r.id " +
            "WHERE ur.user_id=#{userId} AND r.status='ENABLED' ORDER BY r.id")
    List<String> findRoleCodes(@Param("userId") Long userId);

    @Select("SELECT DISTINCT m.permission_code FROM sys_menu m " +
            "JOIN sys_role_menu rm ON rm.menu_id=m.id JOIN sys_role r ON r.id=rm.role_id " +
            "JOIN sys_user_role ur ON ur.role_id=r.id " +
            "WHERE ur.user_id=#{userId} AND r.status='ENABLED' AND m.status='ENABLED' " +
            "AND m.permission_code IS NOT NULL ORDER BY m.permission_code")
    List<String> findPermissions(@Param("userId") Long userId);

    @Select("SELECT DISTINCT m.id,m.parent_id,m.menu_type,m.name,m.path,m.component,m.permission_code,m.sort_no " +
            "FROM sys_menu m JOIN sys_role_menu rm ON rm.menu_id=m.id JOIN sys_role r ON r.id=rm.role_id " +
            "JOIN sys_user_role ur ON ur.role_id=r.id WHERE ur.user_id=#{userId} AND r.status='ENABLED' " +
            "AND m.status='ENABLED' AND m.visible=1 AND m.menu_type<>'BUTTON' ORDER BY m.sort_no,m.id")
    List<MenuVO> findMenus(@Param("userId") Long userId);

    @Select("SELECT warehouse_id FROM inv_warehouse_user WHERE user_id=#{userId} ORDER BY warehouse_id")
    List<Long> findWarehouseIds(@Param("userId") Long userId);

    @Select("SELECT id,username,real_name,job_title,team_id,workshop_id,status FROM sys_user ORDER BY id")
    List<UserSummaryVO> findUsers();

    @Update("UPDATE sys_user SET last_login_at=CURRENT_TIMESTAMP, failed_login_count=0 WHERE id=#{userId}")
    int markLoginSuccess(@Param("userId") Long userId);
}
