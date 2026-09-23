package com.thread.Igniter.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class RedisTokenService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public void revokeToken(String jti, Date expiration) {

        long remainingMillis =
                expiration.getTime() - System.currentTimeMillis();

        if (remainingMillis <= 0) {
            return;
        }

        String key = BLACKLIST_PREFIX + jti;

        redisTemplate.opsForValue().set(
                key,
                "revoked",
                Duration.ofMillis(remainingMillis)
        );
    }

    public boolean isTokenRevoked(String jti) {

        String key = BLACKLIST_PREFIX + jti;

        return Boolean.TRUE.equals(
                redisTemplate.hasKey(key)
        );
    }
}