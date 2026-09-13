package com.cq.maintenance.inventory.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record WarehouseAuthorizationRequest(@NotNull(message="授权用户列表不能为空") List<Long> userIds) {
    public WarehouseAuthorizationRequest { userIds=List.copyOf(userIds); }
}
