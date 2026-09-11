package com.cq.maintenance.security;

import com.cq.maintenance.common.response.ApiResponse;
import com.cq.maintenance.security.dto.LoginRequest;
import com.cq.maintenance.security.dto.RefreshTokenRequest;
import com.cq.maintenance.security.vo.CaptchaVO;
import com.cq.maintenance.security.vo.CurrentUserVO;
import com.cq.maintenance.security.vo.LoginVO;
import com.cq.maintenance.security.vo.TokenVO;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final CaptchaService captchaService;
    private final AuthMapper authMapper;

    public AuthController(AuthService authService, CaptchaService captchaService, AuthMapper authMapper) {
        this.authService = authService;
        this.captchaService = captchaService;
        this.authMapper = authMapper;
    }

    @GetMapping("/captcha")
    public ApiResponse<CaptchaVO> captcha() { return ApiResponse.success(captchaService.create()); }

    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenVO> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                    @RequestBody(required = false) RefreshTokenRequest request) {
        authService.logout(authorization.substring(7), request == null ? null : request.refreshToken());
        return ApiResponse.success(null);
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserVO> me() {
        LoginUser user = SecurityUtils.currentUser();
        return ApiResponse.success(CurrentUserVO.from(user, authMapper.findMenus(user.userId())));
    }

    @GetMapping("/permissions")
    public ApiResponse<java.util.List<String>> permissions() {
        return ApiResponse.success(SecurityUtils.currentUser().permissions());
    }
}
