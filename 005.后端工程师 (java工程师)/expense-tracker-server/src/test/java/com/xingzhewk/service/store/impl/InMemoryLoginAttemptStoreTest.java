package com.xingzhewk.service.store.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 不依赖 Spring 上下文，构造器手动注入参数。
 */
class InMemoryLoginAttemptStoreTest {

    private InMemoryLoginAttemptStore newStore(long windowMs, int maxAttempts) {
        return new InMemoryLoginAttemptStore(windowMs, maxAttempts);
    }

    @Test
    void 初始状态_未锁定() {
        InMemoryLoginAttemptStore s = newStore(900_000, 5);
        assertEquals(0, s.lockedRemainingMs("13800000000"));
    }

    @Test
    void 失败计数递增() {
        InMemoryLoginAttemptStore s = newStore(900_000, 5);
        assertEquals(1, s.recordFailure("a"));
        assertEquals(2, s.recordFailure("a"));
        assertEquals(3, s.recordFailure("a"));
    }

    @Test
    void 达到阈值后进入锁定状态() {
        InMemoryLoginAttemptStore s = newStore(900_000, 3);
        s.recordFailure("a");
        s.recordFailure("a");
        assertEquals(0, s.lockedRemainingMs("a"), "第 2 次未达阈值不应锁定");
        s.recordFailure("a");
        long remaining = s.lockedRemainingMs("a");
        assertTrue(remaining > 0 && remaining <= 900_000, "应进入锁定: " + remaining);
    }

    @Test
    void clear后重置() {
        InMemoryLoginAttemptStore s = newStore(900_000, 2);
        s.recordFailure("a");
        s.recordFailure("a");
        assertTrue(s.lockedRemainingMs("a") > 0);
        s.clear("a");
        assertEquals(0, s.lockedRemainingMs("a"));
        assertEquals(1, s.recordFailure("a"), "clear 后下次失败应从 1 开始");
    }

    @Test
    void 窗口期过后自动失效() throws InterruptedException {
        // 用 50ms 窗口模拟过期
        InMemoryLoginAttemptStore s = newStore(50, 2);
        s.recordFailure("a");
        s.recordFailure("a");
        assertTrue(s.lockedRemainingMs("a") > 0);
        Thread.sleep(80);
        assertEquals(0, s.lockedRemainingMs("a"), "窗口过期后应解除锁定");
        assertEquals(1, s.recordFailure("a"), "窗口过期后下次失败按新窗口计数");
    }

    @Test
    void cleanupExpired_清理过期条目() throws InterruptedException {
        InMemoryLoginAttemptStore s = newStore(30, 99);
        s.recordFailure("a");
        s.recordFailure("b");
        assertEquals(2, s.size());
        Thread.sleep(50);
        s.cleanupExpired();
        assertEquals(0, s.size(), "过期条目应被清理，防止内存泄漏");
    }

    @Test
    void 不同_key_独立计数() {
        InMemoryLoginAttemptStore s = newStore(900_000, 5);
        s.recordFailure("a");
        s.recordFailure("a");
        s.recordFailure("b");
        assertEquals(3, s.recordFailure("a"));
        assertEquals(2, s.recordFailure("b"));
    }
}
