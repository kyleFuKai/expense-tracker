package com.xingzhewk.service.store.impl;

import com.xingzhewk.service.store.LoginAttemptStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 实现：登录失败计数与窗口锁定。
 *
 * Key 设计：{prefix}:login-fail:{phone}
 * Value：失败次数（字符串）；过期时间 = 窗口期。第一次失败时设置 EXPIRE，
 * 后续 INCR 不重置过期时间——这样 firstFailTime + lockWindowMs 自然就是 TTL 终点。
 *
 * 在 {@code app.session-store=redis} 时启用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.session-store", havingValue = "redis")
public class RedisLoginAttemptStore implements LoginAttemptStore {

    private final StringRedisTemplate redis;

    @Value("${app.login.lock-window-ms:900000}")
    private long lockWindowMs;

    @Value("${app.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.session-store-prefix:expense-tracker}")
    private String prefix;

    private String keyOf(String phone) {
        return prefix + ":login-fail:" + phone;
    }

    @Override
    public long lockedRemainingMs(String key) {
        String k = keyOf(key);
        String val = redis.opsForValue().get(k);
        if (val == null) return 0L;
        int count = parseIntSafe(val);
        if (count < maxAttempts) return 0L;
        Long ttl = redis.getExpire(k, java.util.concurrent.TimeUnit.MILLISECONDS);
        return (ttl == null || ttl < 0) ? 0L : ttl;
    }

    @Override
    public int recordFailure(String key) {
        String k = keyOf(key);
        Long count = redis.opsForValue().increment(k);
        if (count == null) {
            // Redis 异常时返回 1 让外层继续走，但不锁定
            log.warn("Redis INCR returned null for key {}", k);
            return 1;
        }
        if (count == 1L) {
            // 第一次写入，设置窗口期 TTL
            redis.expire(k, Duration.ofMillis(lockWindowMs));
        }
        return count.intValue();
    }

    @Override
    public void clear(String key) {
        redis.delete(keyOf(key));
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }
}
