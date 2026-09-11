package com.cq.maintenance.organization.vo;

public record StationVO(Long id, Long lineId, String lineNo, String lineName, Long workshopId,
                        String workshopNo, String workshopName, String stationNo,
                        String stationName, String status) {}
