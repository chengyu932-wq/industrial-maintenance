package com.cq.maintenance.system.controller;

import com.cq.maintenance.common.response.ApiResponse;
import com.cq.maintenance.security.AuthMapper;
import com.cq.maintenance.system.vo.UserSummaryVO;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final AuthMapper authMapper;

    public UserController(AuthMapper authMapper) { this.authMapper = authMapper; }

    @GetMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    public ApiResponse<List<UserSummaryVO>> list() {
        return ApiResponse.success(authMapper.findUsers());
    }

    @GetMapping("/options")
    @PreAuthorize("hasAnyAuthority('equipment:add','equipment:update')")
    public ApiResponse<List<UserSummaryVO>> options() {
        return ApiResponse.success(authMapper.findUsers().stream().filter(u -> "ENABLED".equals(u.status())).toList());
    }
}
