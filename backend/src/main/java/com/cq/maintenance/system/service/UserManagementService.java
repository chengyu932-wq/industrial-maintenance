package com.cq.maintenance.system.service;
import com.cq.maintenance.common.exception.*;
import com.cq.maintenance.security.SecurityUtils;
import com.cq.maintenance.system.dto.*;
import com.cq.maintenance.system.entity.SysUser;
import com.cq.maintenance.system.mapper.UserManagementMapper;
import com.cq.maintenance.system.vo.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service public class UserManagementService {private UserManagementMapper mapper;private PasswordEncoder encoder;public UserManagementService(){}public UserManagementService(UserManagementMapper mapper,PasswordEncoder encoder){this.mapper=mapper;this.encoder=encoder;}@Autowired(required=false)void setMapper(UserManagementMapper mapper){this.mapper=mapper;}@Autowired(required=false)void setEncoder(PasswordEncoder encoder){this.encoder=encoder;}
 public UserDetailVO detail(Long id){SysUser u=required(id);return new UserDetailVO(u.getId(),u.getUsername(),u.getRealName(),u.getPhone(),u.getEmail(),u.getJobTitle(),u.getTeamId(),u.getWorkshopId(),u.getStatus(),mapper.roleIds(id),mapper.skillIds(id));}
 public UserMetadataVO metadata(){return new UserMetadataVO(mapper.roles(),mapper.skills(),mapper.teams(),mapper.workshops());}
 @Transactional public Long create(UserCreateRequest r){String username=r.username().trim();if(mapper.countUsername(username)>0)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"账号已存在");validateRefs(r.teamId(),r.workshopId(),r.roleIds(),r.skillIds());SysUser u=new SysUser();u.setUsername(username);u.setPasswordHash(encoder.encode(r.password()));copy(u,r.realName(),r.phone(),r.email(),r.jobTitle(),r.teamId(),r.workshopId());u.setStatus("ENABLED");mapper.insert(u);replaceRelations(u.getId(),r.roleIds(),r.skillIds());return u.getId();}
 @Transactional public void update(Long id,UserUpdateRequest r){SysUser u=required(id);validateRefs(r.teamId(),r.workshopId(),r.roleIds(),r.skillIds());copy(u,r.realName(),r.phone(),r.email(),r.jobTitle(),r.teamId(),r.workshopId());mapper.update(u);replaceRelations(id,r.roleIds(),r.skillIds());}
 @Transactional public void status(Long id,UserStatusRequest r){SysUser u=required(id);if(id.equals(SecurityUtils.currentUser().userId())&&"DISABLED".equals(r.status()))throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"不能禁用当前登录账号");if(!Objects.equals(u.getStatus(),r.status()))mapper.updateStatus(id,r.status());}
 @Transactional public void resetPassword(Long id,PasswordResetRequest r){required(id);mapper.resetPassword(id,encoder.encode(r.password()));}
 private SysUser required(Long id){SysUser u=mapper.find(id);if(u==null)throw new BusinessException(ErrorCode.NOT_FOUND,"用户不存在");return u;}
 private void validateRefs(Long team,Long workshop,List<Long> roles,List<Long> skills){if(team!=null&&mapper.countTeam(team)==0)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"班组不存在或已停用");if(workshop!=null&&mapper.countWorkshop(workshop)==0)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"车间不存在或已停用");for(Long id:new LinkedHashSet<>(roles))if(id==null||mapper.countRole(id)==0)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"角色不存在或已停用");for(Long id:new LinkedHashSet<>(skills==null?List.of():skills))if(id==null||mapper.countSkill(id)==0)throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,"技能不存在或已停用");}
 private void replaceRelations(Long id,List<Long> roles,List<Long> skills){List<Long> uniqueRoles=new ArrayList<>(new LinkedHashSet<>(roles));List<Long> uniqueSkills=new ArrayList<>(new LinkedHashSet<>(skills==null?List.of():skills));mapper.deleteRoles(id);mapper.insertRoles(id,uniqueRoles);mapper.deleteSkills(id);if(!uniqueSkills.isEmpty())mapper.insertSkills(id,uniqueSkills);}
 private void copy(SysUser u,String name,String phone,String email,String job,Long team,Long workshop){u.setRealName(name.trim());u.setPhone(trim(phone));u.setEmail(trim(email));u.setJobTitle(trim(job));u.setTeamId(team);u.setWorkshopId(workshop);}private String trim(String s){return s==null||s.isBlank()?null:s.trim();}
}
