package com.cq.maintenance.security;

import com.cq.maintenance.common.exception.BusinessException;
import com.cq.maintenance.common.exception.ErrorCode;
import com.cq.maintenance.common.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final TokenStore tokenStore;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, TokenStore tokenStore,
                                   CurrentUserService currentUserService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.tokenStore = tokenStore;
        this.currentUserService = currentUserService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        try {
            JwtService.TokenClaims claims = jwtService.parse(header.substring(7));
            if (!JwtService.ACCESS.equals(claims.tokenType()) || tokenStore.isBlacklisted(claims.tokenId())) {
                writeError(response, ErrorCode.ACCESS_TOKEN_INVALID);
                return;
            }
            LoginUser user = currentUserService.load(claims.userId());
            var authentication = UsernamePasswordAuthenticationToken.authenticated(user, null, user.authorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } catch (ExpiredJwtException exception) {
            writeError(response, ErrorCode.ACCESS_TOKEN_EXPIRED);
        } catch (BusinessException exception) {
            writeError(response, exception.getErrorCode());
        } catch (JwtException | IllegalArgumentException exception) {
            writeError(response, ErrorCode.ACCESS_TOKEN_INVALID);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void writeError(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.getHttpStatus().value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure(code));
    }
}
