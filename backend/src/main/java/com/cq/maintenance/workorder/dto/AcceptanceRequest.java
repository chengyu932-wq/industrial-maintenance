package com.cq.maintenance.workorder.dto;

import jakarta.validation.constraints.Size;

public record AcceptanceRequest(@Size(max=500,message="验收意见不能超过500字") String remark) {}
