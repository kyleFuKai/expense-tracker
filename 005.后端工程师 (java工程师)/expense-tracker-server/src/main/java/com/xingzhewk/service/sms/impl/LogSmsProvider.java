package com.xingzhewk.service.sms.impl;

import com.xingzhewk.service.sms.SmsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 把验证码写到应用日志，开发者自己看 console 拿码。
 *
 * 在 {@code app.sms.provider=log}（默认）时启用。
 *
 * <p>使用 WARN 级别是故意的：任何看日志的人都应该一眼意识到「这是测试通道，
 * 还没接真短信」。如果发现 prod 的日志里也在刷这行，就该立刻去配 provider=aliyun
 * 或 provider=noop。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "log", matchIfMissing = true)
public class LogSmsProvider implements SmsProvider {

    @Override
    public boolean send(String phone, String code) {
        // 关键告警：phone 完整、code 完整、明确标注 dev-only
        log.warn("【SMS-LOG-PROVIDER · DEV-ONLY】phone={} code={} —— 这是测试通道，prod 必须切换 app.sms.provider", phone, code);
        return true;
    }

    @Override
    public String name() {
        return "log";
    }
}
