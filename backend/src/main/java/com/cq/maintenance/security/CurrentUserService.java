package com.cq.maintenance.security;

import com.cq.maintenance.common.exception.BusinessException;
import com.cq.maintenance.common.exception.ErrorCode;
import com.cq.maintenance.system.entity.SysUser;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final AuthMapper authMapper;

    public CurrentUserService(AuthMapper authMapper) { this.authMapper = authMapper; }

    public LoginUser load(Long userId) {
        SysUser user = authMapper.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        if (!"ENABLED".equals(user.getStatus())) throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        List<Long> teamIds = user.getTeamId() == null ? List.of() : List.of(user.getTeamId());
        return new LoginUser(user.getId(), user.getUsername(), user.getRealName(), user.getStatus(),
            authMapper.findRoleCodes(userId), authMapper.findPermissions(userId), teamIds,
            user.getWorkshopId(), authMapper.findWarehouseIds(userId));
    }
}
