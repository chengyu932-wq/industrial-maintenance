package com.cq.maintenance.organization.vo;

public record WorkshopVO(Long id, String workshopNo, String workshopName, Long managerId,
                         String managerName, String status) {}
