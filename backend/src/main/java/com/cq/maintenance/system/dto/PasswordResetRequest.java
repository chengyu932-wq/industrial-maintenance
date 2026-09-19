package com.cq.maintenance.system.dto;
import jakarta.validation.constraints.*;
public record PasswordResetRequest(@NotBlank(message="新密码不能为空") @Size(min=8,max=72,message="密码长度必须为8至72位") String password){}
