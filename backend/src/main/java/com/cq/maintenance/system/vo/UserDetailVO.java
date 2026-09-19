package com.cq.maintenance.system.vo;
import java.util.List;
public record UserDetailVO(Long id,String username,String realName,String phone,String email,String jobTitle,Long teamId,Long workshopId,String status,List<Long> roleIds,List<Long> skillIds){}
