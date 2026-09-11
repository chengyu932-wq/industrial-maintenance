package com.cq.maintenance.organization.vo;

public record LineVO(Long id, Long workshopId, String workshopNo, String workshopName,
                     String lineNo, String lineName, String status) {}
