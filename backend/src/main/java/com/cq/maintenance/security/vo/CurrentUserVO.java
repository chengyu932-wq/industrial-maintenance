package com.cq.maintenance.security.vo;

import com.cq.maintenance.security.LoginUser;
import com.cq.maintenance.system.vo.MenuVO;
import java.util.List;

public record CurrentUserVO(Long userId, String username, String realName, List<String> roleCodes,
                            List<String> permissions, List<MenuVO> menus, List<Long> teamIds,
                            Long workshopId, List<Long> authorizedWarehouseIds) {
    public static CurrentUserVO from(LoginUser user, List<MenuVO> menus) {
        return new CurrentUserVO(user.userId(), user.username(), user.realName(), user.roleCodes(),
            user.permissions(), menus, user.teamIds(), user.workshopId(), user.authorizedWarehouseIds());
    }
}
