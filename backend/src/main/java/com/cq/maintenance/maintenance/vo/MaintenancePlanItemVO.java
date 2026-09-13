package com.cq.maintenance.maintenance.vo;

public record MaintenancePlanItemVO(Long id,Long planId,String itemName,String standardDescription,Integer sortNo,Boolean required) {}
