package com.xingzhewk.service.store.impl;

import com.xingzhewk.service.store.SmsCodeStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 实现：短信验证码 + 发送频控。
 *
 * Key 设计：
 *   {prefix}:sms-code:{phone}      → 验证码本体，TTL = 5min
 *   {prefix}:sms-throttle:{phone}  → 占位，TTL = 60s，用 SETNX 实现「最近 60s 内已发过」
 *
 * 频控独立 key 是因为验证码本体 TTL（5min）比间隔（60s）长，混用会让发送间隔失效。
 *
 * 在 {@code app.session-store=redis} 时启用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.session-store", havingValue = "redis")
public class RedisSmsCodeStore implements SmsCodeStore {

    private final StringRedisTemplate redis;

    @Value("${app.session-store-prefix:expense-tracker}")
    private String prefix;

    private String codeKey(String phone)     { return prefix + ":sms-code:" + phone; }
    private String throttleKey(String phone) { return prefix + ":sms-throttle:" + phone; }

    @Override
    public boolean putIfNotThrottled(String key, String code, long ttlSeconds, long minIntervalMs) {
        // SETNX 占用频控 key：成功 = 没人在窗口内，可以发；失败 = 频控未过期
        Boolean acquired = redis.opsForValue().setIfAbsent(
                throttleKey(key), "1", Duration.ofMillis(minIntervalMs));
        if (acquired == null || !acquired) {
            return false;
        }
        redis.opsForValue().set(codeKey(key), code, Duration.ofSeconds(ttlSeconds));
        return true;
    }

    @Override
    public String get(String key) {
        return redis.opsForValue().get(codeKey(key));
    }

    @Override
    public void remove(String key) {
        redis.delete(codeKey(key));
        // 频控 key 不主动删；让它自然到期，防止刚校验完又秒发
    }
}
