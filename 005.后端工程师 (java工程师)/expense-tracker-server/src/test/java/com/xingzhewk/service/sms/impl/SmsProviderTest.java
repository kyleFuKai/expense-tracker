package com.xingzhewk.service.sms.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SmsProviderTest {

    @Test
    void log_provider_总是返回_true() {
        LogSmsProvider p = new LogSmsProvider();
        assertTrue(p.send("13800000000", "123456"));
        assertEquals("log", p.name());
    }

    @Test
    void noop_provider_总是返回_false() {
        // 这是个特性，不是 bug：prod 没接通道时拒绝下发胜过假装成功
        NoopSmsProvider p = new NoopSmsProvider();
        assertFalse(p.send("13800000000", "123456"));
        assertEquals("noop", p.name());
    }
}
