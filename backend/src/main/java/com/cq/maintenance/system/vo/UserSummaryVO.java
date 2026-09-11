package com.cq.maintenance.system.vo;

public record UserSummaryVO(Long id, String username, String realName, String jobTitle,
                            Long teamId, Long workshopId, String status) {}
