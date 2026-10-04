package com.cq.maintenance.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    public static final String ACCESS = "access";
    public static final String REFRESH = "refresh";
    private final AuthProperties properties;
    private final SecretKey key;

    public JwtService(AuthProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public IssuedToken issue(Long userId, String username, String type) {
        return issue(userId,username,type,0L);
    }
    public IssuedToken issue(Long userId, String username, String type, Long passwordVersion) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ACCESS.equals(type) ? properties.getAccessTokenTtl() : properties.getRefreshTokenTtl());
        String tokenId = UUID.randomUUID().toString();
        String value = Jwts.builder().id(tokenId).subject(userId.toString()).claim("username", username)
            .claim("passwordVersion", passwordVersion == null ? 0L : passwordVersion).claim("tokenType", type).issuedAt(Date.from(now)).expiration(Date.from(expiresAt))
            .signWith(key).compact();
        return new IssuedToken(value, tokenId, expiresAt);
    }

    public TokenClaims parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new TokenClaims(Long.valueOf(claims.getSubject()), claims.get("username", String.class),
            claims.get("tokenType", String.class), claims.getId(), claims.getExpiration().toInstant(), claims.get("passwordVersion", Long.class) == null ? 0L : claims.get("passwordVersion", Long.class));
    }

    public record IssuedToken(String value, String tokenId, Instant expiresAt) {}
    public record TokenClaims(Long userId, String username, String tokenType, String tokenId, Instant expiresAt, Long passwordVersion) {
        public TokenClaims(Long userId,String username,String tokenType,String tokenId,Instant expiresAt){this(userId,username,tokenType,tokenId,expiresAt,0L);}
    }
}
