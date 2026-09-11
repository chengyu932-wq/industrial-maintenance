package com.cq.maintenance.security;

import java.time.Duration;
import java.time.Instant;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenStore {
    private static final String REFRESH_PREFIX = "auth:refresh:";
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";
    private static final String SESSION_PREFIX = "auth:session:";
    private final StringRedisTemplate redis;

    public TokenStore(StringRedisTemplate redis) { this.redis = redis; }

    public void savePair(Long userId, JwtService.IssuedToken access, JwtService.IssuedToken refresh) {
        redis.opsForValue().set(refreshKey(userId, refresh.tokenId()), refresh.value(), remaining(refresh.expiresAt()));
        Duration accessTtl = remaining(access.expiresAt());
        if (!accessTtl.isZero()) {
            redis.opsForValue().set(SESSION_PREFIX + access.tokenId(), userId + ":" + refresh.tokenId(), accessTtl);
        }
    }

    public boolean isRefreshValid(JwtService.TokenClaims claims, String rawToken) {
        String stored = redis.opsForValue().get(refreshKey(claims.userId(), claims.tokenId()));
        return rawToken.equals(stored);
    }

    public void deleteRefresh(Long userId, String tokenId) { redis.delete(refreshKey(userId, tokenId)); }
    public boolean isBlacklisted(String tokenId) { return Boolean.TRUE.equals(redis.hasKey(BLACKLIST_PREFIX + tokenId)); }

    public void blacklist(String tokenId, Instant expiresAt) {
        Duration ttl = remaining(expiresAt);
        if (!ttl.isZero()) redis.opsForValue().set(BLACKLIST_PREFIX + tokenId, "1", ttl);
    }

    public String consumeSession(String accessTokenId) {
        return redis.opsForValue().getAndDelete(SESSION_PREFIX + accessTokenId);
    }

    private String refreshKey(Long userId, String tokenId) { return REFRESH_PREFIX + userId + ":" + tokenId; }
    private Duration remaining(Instant expiresAt) {
        Duration duration = Duration.between(Instant.now(), expiresAt);
        return duration.isNegative() ? Duration.ZERO : duration;
    }
}
