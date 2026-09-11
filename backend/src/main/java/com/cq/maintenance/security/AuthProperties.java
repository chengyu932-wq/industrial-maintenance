package com.cq.maintenance.security;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {
    private String jwtSecret;
    private Duration accessTokenTtl = Duration.ofMinutes(30);
    private Duration refreshTokenTtl = Duration.ofDays(7);
    private Duration captchaTtl = Duration.ofMinutes(2);
    private Duration loginFailureWindow = Duration.ofMinutes(10);
    private Duration loginLockTtl = Duration.ofMinutes(10);
    private int maxLoginFailures = 5;

    @PostConstruct
    void validate() {
        if (jwtSecret == null || jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
    }

    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }
    public Duration getAccessTokenTtl() { return accessTokenTtl; }
    public void setAccessTokenTtl(Duration value) { this.accessTokenTtl = value; }
    public Duration getRefreshTokenTtl() { return refreshTokenTtl; }
    public void setRefreshTokenTtl(Duration value) { this.refreshTokenTtl = value; }
    public Duration getCaptchaTtl() { return captchaTtl; }
    public void setCaptchaTtl(Duration value) { this.captchaTtl = value; }
    public Duration getLoginFailureWindow() { return loginFailureWindow; }
    public void setLoginFailureWindow(Duration value) { this.loginFailureWindow = value; }
    public Duration getLoginLockTtl() { return loginLockTtl; }
    public void setLoginLockTtl(Duration value) { this.loginLockTtl = value; }
    public int getMaxLoginFailures() { return maxLoginFailures; }
    public void setMaxLoginFailures(int value) { this.maxLoginFailures = value; }
}
