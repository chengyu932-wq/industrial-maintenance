package com.cq.maintenance.dispatch.vo;

public record DispatchEngineerRow(Long engineerId,String engineerName,Long teamId,String teamName,
    Long workshopId,long activeWorkOrderCount) {}
