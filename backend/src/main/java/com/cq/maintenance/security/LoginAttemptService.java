package com.cq.maintenance.security;

import com.cq.maintenance.common.exception.BusinessException;
import com.cq.maintenance.common.exception.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {
    private static final String FAIL_PREFIX = "auth:login:fail:";
    private static final String LOCK_PREFIX = "auth:login:lock:";
    private final StringRedisTemplate redis;
    private final AuthProperties properties;

    public LoginAttemptService(StringRedisTemplate redis, AuthProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public void ensureNotLocked(String username) {
        if (Boolean.TRUE.equals(redis.hasKey(LOCK_PREFIX + normalize(username)))) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
    }

    public void recordFailure(String username) {
        String normalized = normalize(username);
        String key = FAIL_PREFIX + normalized;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) redis.expire(key, properties.getLoginFailureWindow());
        if (count != null && count >= properties.getMaxLoginFailures()) {
            redis.opsForValue().set(LOCK_PREFIX + normalized, "1", properties.getLoginLockTtl());
            redis.delete(key);
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
    }

    public void clear(String username) {
        String normalized = normalize(username);
        redis.delete(FAIL_PREFIX + normalized);
        redis.delete(LOCK_PREFIX + normalized);
    }

    private String normalize(String username) { return username.trim().toLowerCase(java.util.Locale.ROOT); }
}
