package com.xingzhewk.service.store.impl;

import com.xingzhewk.service.store.LoginAttemptStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内登录失败计数存储。仅适用于单实例部署。
 *
 * 在 {@code app.session-store=memory}（默认）时启用。
 * 通过 {@link Scheduled} 定时清理已过窗口期的记录，避免 Map 长期堆积导致内存泄漏。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.session-store", havingValue = "memory", matchIfMissing = true)
public class InMemoryLoginAttemptStore implements LoginAttemptStore {

    private final long lockWindowMs;
    private final int maxAttempts;
    private final ConcurrentHashMap<String, Record> store = new ConcurrentHashMap<>();

    public InMemoryLoginAttemptStore(
            @Value("${app.login.lock-window-ms:900000}") long lockWindowMs,
            @Value("${app.login.max-attempts:5}") int maxAttempts) {
        this.lockWindowMs = lockWindowMs;
        this.maxAttempts = maxAttempts;
    }

    @Override
    public long lockedRemainingMs(String key) {
        Record r = store.get(key);
        if (r == null || r.failedCount < maxAttempts) {
            return 0L;
        }
        long elapsed = System.currentTimeMillis() - r.firstFailTimeMs;
        if (elapsed >= lockWindowMs) {
            // 窗口期已过，顺手清理
            store.remove(key);
            return 0L;
        }
        return lockWindowMs - elapsed;
    }

    @Override
    public int recordFailure(String key) {
        Record r = store.compute(key, (k, prev) -> {
            long now = System.currentTimeMillis();
            if (prev == null || now - prev.firstFailTimeMs >= lockWindowMs) {
                return new Record(1, now);
            }
            return new Record(prev.failedCount + 1, prev.firstFailTimeMs);
        });
        return r.failedCount;
    }

    @Override
    public void clear(String key) {
        store.remove(key);
    }

    /**
     * 兜底清理：每分钟扫一遍，剔除已过窗口期的条目。
     * 即使有些 key 永远不再登录，也不会无限堆积。
     */
    @Scheduled(fixedDelayString = "${app.login.cleanup-interval-ms:60000}")
    void cleanupExpired() {
        long now = System.currentTimeMillis();
        int sizeBefore = store.size();
        store.entrySet().removeIf(e -> now - e.getValue().firstFailTimeMs >= lockWindowMs);
        int removed = sizeBefore - store.size();
        if (removed > 0) {
            log.debug("LoginAttemptStore 清理过期条目 {} 个, 剩余 {}", removed, store.size());
        }
    }

    // 供测试观察
    int size() {
        return store.size();
    }

    /** 不可变记录，compute 内部用 new 替换避免共享可变状态。 */
    private record Record(int failedCount, long firstFailTimeMs) { }
}
