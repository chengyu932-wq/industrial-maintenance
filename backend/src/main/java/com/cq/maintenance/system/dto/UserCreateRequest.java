package com.cq.maintenance.system.dto;
import jakarta.validation.constraints.*;
import java.util.List;
public record UserCreateRequest(@NotBlank(message="账号不能为空") @Pattern(regexp="^[A-Za-z0-9_]{3,50}$",message="账号只能包含字母、数字、下划线且长度为3至50") String username,@NotBlank(message="初始密码不能为空") @Size(min=8,max=72,message="密码长度必须为8至72位") String password,@NotBlank(message="姓名不能为空") @Size(max=50,message="姓名不能超过50个字符") String realName,@Size(max=20,message="手机号不能超过20个字符") String phone,@Email(message="邮箱格式不正确") @Size(max=100,message="邮箱不能超过100个字符") String email,@Size(max=50,message="岗位不能超过50个字符") String jobTitle,Long teamId,Long workshopId,@NotEmpty(message="至少分配一个角色") List<Long> roleIds,List<Long> skillIds){}
