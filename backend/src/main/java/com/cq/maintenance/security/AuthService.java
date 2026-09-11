package com.cq.maintenance.security;

import com.cq.maintenance.common.exception.BusinessException;
import com.cq.maintenance.common.exception.ErrorCode;
import com.cq.maintenance.security.dto.LoginRequest;
import com.cq.maintenance.security.vo.CurrentUserVO;
import com.cq.maintenance.security.vo.LoginVO;
import com.cq.maintenance.security.vo.TokenVO;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final CaptchaService captchaService;
    private final LoginAttemptService attempts;
    private final JwtService jwtService;
    private final TokenStore tokenStore;
    private final CurrentUserService currentUserService;
    private final AuthMapper authMapper;
    private final AuthProperties properties;

    public AuthService(AuthenticationManager authenticationManager, CaptchaService captchaService,
                       LoginAttemptService attempts, JwtService jwtService, TokenStore tokenStore,
                       CurrentUserService currentUserService, AuthMapper authMapper, AuthProperties properties) {
        this.authenticationManager = authenticationManager;
        this.captchaService = captchaService;
        this.attempts = attempts;
        this.jwtService = jwtService;
        this.tokenStore = tokenStore;
        this.currentUserService = currentUserService;
        this.authMapper = authMapper;
        this.properties = properties;
    }

    public LoginVO login(LoginRequest request) {
        captchaService.verify(request.captchaId(), request.captchaCode());
        attempts.ensureNotLocked(request.username());
        try {
            var authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
            LoginUser user = (LoginUser) authentication.getPrincipal();
            attempts.clear(request.username());
            authMapper.markLoginSuccess(user.userId());
            TokenPair pair = issuePair(user);
            return new LoginVO(pair.access().value(), pair.refresh().value(), properties.getAccessTokenTtl().toSeconds(),
                CurrentUserVO.from(user, authMapper.findMenus(user.userId())));
        } catch (DisabledException exception) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        } catch (BadCredentialsException exception) {
            attempts.recordFailure(request.username());
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_INVALID);
        }
    }

    public TokenVO refresh(String refreshToken) {
        JwtService.TokenClaims claims = parse(refreshToken, ErrorCode.REFRESH_TOKEN_INVALID);
        if (!JwtService.REFRESH.equals(claims.tokenType()) || !tokenStore.isRefreshValid(claims, refreshToken)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        LoginUser user = currentUserService.load(claims.userId());
        tokenStore.deleteRefresh(claims.userId(), claims.tokenId());
        TokenPair pair = issuePair(user);
        return new TokenVO(pair.access().value(), pair.refresh().value(), properties.getAccessTokenTtl().toSeconds());
    }

    public void logout(String accessToken, String optionalRefreshToken) {
        JwtService.TokenClaims access = parse(accessToken, ErrorCode.ACCESS_TOKEN_INVALID);
        if (!JwtService.ACCESS.equals(access.tokenType())) throw new BusinessException(ErrorCode.ACCESS_TOKEN_INVALID);
        tokenStore.blacklist(access.tokenId(), access.expiresAt());
        String session = tokenStore.consumeSession(access.tokenId());
        if (session != null && session.contains(":")) {
            int separator = session.indexOf(':');
            tokenStore.deleteRefresh(Long.valueOf(session.substring(0, separator)), session.substring(separator + 1));
        }
        if (optionalRefreshToken != null && !optionalRefreshToken.isBlank()) {
            try {
                JwtService.TokenClaims refresh = jwtService.parse(optionalRefreshToken);
                if (JwtService.REFRESH.equals(refresh.tokenType())) tokenStore.deleteRefresh(refresh.userId(), refresh.tokenId());
            } catch (JwtException | IllegalArgumentException ignored) {
                // Logout remains idempotent when the optional refresh token is already invalid.
            }
        }
    }

    private TokenPair issuePair(LoginUser user) {
        JwtService.IssuedToken access = jwtService.issue(user.userId(), user.username(), JwtService.ACCESS);
        JwtService.IssuedToken refresh = jwtService.issue(user.userId(), user.username(), JwtService.REFRESH);
        tokenStore.savePair(user.userId(), access, refresh);
        return new TokenPair(access, refresh);
    }

    private JwtService.TokenClaims parse(String token, ErrorCode errorCode) {
        try { return jwtService.parse(token); }
        catch (JwtException | IllegalArgumentException exception) { throw new BusinessException(errorCode); }
    }

    private record TokenPair(JwtService.IssuedToken access, JwtService.IssuedToken refresh) {}
}
