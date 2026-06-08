package com.xingzhewk.service.store.impl;

import com.xingzhewk.service.store.SmsCodeStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内短信验证码存储。仅适用于单实例部署。
 *
 * 在 {@code app.session-store=memory}（默认）时启用。
 * 兜底清理：定时剔除已超过 ttl 的条目，避免内存泄漏。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.session-store", havingValue = "memory", matchIfMissing = true)
public class InMemorySmsCodeStore implements SmsCodeStore {

    private final ConcurrentHashMap<String, Record> store = new ConcurrentHashMap<>();

    @Override
    public boolean putIfNotThrottled(String key, String code, long ttlSeconds, long minIntervalMs) {
        long now = System.currentTimeMillis();
        // compute 保证「检查 + 写入」原子化，避免并发下双发
        Record updated = store.compute(key, (k, prev) -> {
            if (prev != null && now - prev.writeTimeMs < minIntervalMs) {
                return prev; // 触发频控，原样返回（外层用引用比较判定）
            }
            return new Record(code, now, now + ttlSeconds * 1000L);
        });
        return updated.code.equals(code) && updated.writeTimeMs == now;
    }

    @Override
    public String get(String key) {
        Record r = store.get(key);
        if (r == null) return null;
        if (System.currentTimeMillis() > r.expireAtMs) {
            store.remove(key);
            return null;
        }
        return r.code;
    }

    @Override
    public void remove(String key) {
        store.remove(key);
    }

    @Scheduled(fixedDelayString = "${app.sms.cleanup-interval-ms:60000}")
    void cleanupExpired() {
        long now = System.currentTimeMillis();
        int sizeBefore = store.size();
        store.entrySet().removeIf(e -> now > e.getValue().expireAtMs);
        int removed = sizeBefore - store.size();
        if (removed > 0) {
            log.debug("SmsCodeStore 清理过期条目 {} 个, 剩余 {}", removed, store.size());
        }
    }

    int size() {
        return store.size();
    }

    private record Record(String code, long writeTimeMs, long expireAtMs) { }
}
