package com.cq.maintenance.system.dto;
import jakarta.validation.constraints.*;
public record UserStatusRequest(@NotBlank(message="用户状态不能为空") @Pattern(regexp="ENABLED|DISABLED",message="用户状态不合法") String status){}
