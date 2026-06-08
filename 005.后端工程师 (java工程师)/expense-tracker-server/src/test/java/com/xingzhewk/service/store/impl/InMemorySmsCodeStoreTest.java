package com.xingzhewk.service.store.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemorySmsCodeStoreTest {

    @Test
    void 首次发送成功() {
        InMemorySmsCodeStore s = new InMemorySmsCodeStore();
        assertTrue(s.putIfNotThrottled("13800000000", "666666", 300, 60_000));
        assertEquals("666666", s.get("13800000000"));
    }

    @Test
    void 间隔内重复发送被拒() {
        InMemorySmsCodeStore s = new InMemorySmsCodeStore();
        assertTrue(s.putIfNotThrottled("a", "111111", 300, 60_000));
        assertFalse(s.putIfNotThrottled("a", "222222", 300, 60_000),
                "60s 内重复发送应被频控拦截");
        assertEquals("111111", s.get("a"), "频控拒绝后旧验证码保留");
    }

    @Test
    void 超过间隔后允许再发() throws InterruptedException {
        InMemorySmsCodeStore s = new InMemorySmsCodeStore();
        assertTrue(s.putIfNotThrottled("a", "111111", 300, 30));
        Thread.sleep(50);
        assertTrue(s.putIfNotThrottled("a", "222222", 300, 30));
        assertEquals("222222", s.get("a"));
    }

    @Test
    void ttl_过期后取不到() throws InterruptedException {
        InMemorySmsCodeStore s = new InMemorySmsCodeStore();
        // 用 ttlSeconds 不好直接给亚秒；用 0 秒等同立即过期
        s.putIfNotThrottled("a", "111111", 0, 0);
        Thread.sleep(20);
        assertNull(s.get("a"), "过期后 get 返回 null 并主动清理");
        assertEquals(0, s.size());
    }

    @Test
    void remove_一次性消费() {
        InMemorySmsCodeStore s = new InMemorySmsCodeStore();
        s.putIfNotThrottled("a", "111111", 300, 0);
        s.remove("a");
        assertNull(s.get("a"));
    }

    @Test
    void cleanupExpired_清理过期() throws InterruptedException {
        InMemorySmsCodeStore s = new InMemorySmsCodeStore();
        s.putIfNotThrottled("a", "1", 0, 0);
        s.putIfNotThrottled("b", "2", 0, 0);
        Thread.sleep(20);
        s.cleanupExpired();
        assertEquals(0, s.size());
    }

    @Test
    void 不同手机号互不干扰() {
        InMemorySmsCodeStore s = new InMemorySmsCodeStore();
        assertTrue(s.putIfNotThrottled("a", "111111", 300, 60_000));
        assertTrue(s.putIfNotThrottled("b", "222222", 300, 60_000),
                "a 的频控不应阻止 b 发送");
        assertEquals("111111", s.get("a"));
        assertEquals("222222", s.get("b"));
    }
}
