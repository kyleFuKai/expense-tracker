package com.xingzhewk.service.sms.impl;

import com.xingzhewk.service.sms.SmsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 拒绝发送的通道。
 *
 * 在 {@code app.sms.provider=noop} 时启用。
 *
 * <p>用途：prod 环境**还没接入**真实短信通道时的安全兜底。
 * 比「悄悄 log 一下假装发了」强 —— 前端会拿到 false，
 * 业务层可以提示「短信功能暂未开放，请联系管理员重置密码」，
 * 避免用户傻等收不到的短信、也避免攻击者通过 666666 接管账号。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "noop")
public class NoopSmsProvider implements SmsProvider {

    @Override
    public boolean send(String phone, String code) {
        log.info("SMS noop provider 拒绝发送 phone={}（短信通道未接入）", phone);
        return false;
    }

    @Override
    public String name() {
        return "noop";
    }
}
