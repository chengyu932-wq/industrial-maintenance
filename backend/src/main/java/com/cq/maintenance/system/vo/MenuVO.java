package com.cq.maintenance.system.vo;

public record MenuVO(Long id, Long parentId, String menuType, String name, String path,
                     String component, String permissionCode, Integer sortNo) {}
