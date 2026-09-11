package com.cq.maintenance.organization.vo;

import java.util.List;

public record OrganizationTreeVO(Long id, String type, String code, String name, String status,
                                 List<OrganizationTreeVO> children) {}
