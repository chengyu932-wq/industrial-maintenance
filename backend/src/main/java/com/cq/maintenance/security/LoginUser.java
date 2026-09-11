package com.cq.maintenance.security;

import java.io.Serializable;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record LoginUser(Long userId, String username, String realName, String status,
                        List<String> roleCodes, List<String> permissions, List<Long> teamIds,
                        Long workshopId, List<Long> authorizedWarehouseIds) implements Serializable {
    public List<SimpleGrantedAuthority> authorities() {
        return permissions.stream().map(SimpleGrantedAuthority::new).toList();
    }
}
