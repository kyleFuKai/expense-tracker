/**
 * 短信通道抽象。
 *
 * 与 Java 后端 {@code com.xingzhewk.service.sms.SmsProvider} 接口对齐。
 * 详见 contracts/DIFFS.md #2。
 *
 * dev 默认 {@link LogSmsProvider}，prod 配 SMS_PROVIDER=noop 改用 {@link NoopSmsProvider}。
 */

/** 把验证码写到应用日志。开发者自己看 console 拿码。 */
class LogSmsProvider {
    send(phone, code) {
        // WARN 是故意的：任何看日志的人应该一眼意识到这是测试通道
        console.warn(`[SMS-LOG-PROVIDER · DEV-ONLY] phone=${phone} code=${code} —— 这是测试通道，prod 必须切换 app.sms.provider`);
        return true;
    }
    name() { return 'log'; }
}

/** 拒绝发送的通道。prod 未接真通道时的安全默认。 */
class NoopSmsProvider {
    send(phone, code) {
        console.info(`[SMS-NOOP-PROVIDER] 拒绝发送 phone=${phone}（短信通道未接入）`);
        return false;
    }
    name() { return 'noop'; }
}

/** 按 SMS_PROVIDER 环境变量选 provider。 */
function createSmsProvider() {
    const which = (process.env.SMS_PROVIDER || 'log').toLowerCase();
    switch (which) {
        case 'log':   return new LogSmsProvider();
        case 'noop':  return new NoopSmsProvider();
        // 未来 'aliyun' / 'tencent' 在此加
        default:
            console.warn(`[sms] 未知 SMS_PROVIDER="${which}"，降级到 log`);
            return new LogSmsProvider();
    }
}

module.exports = { LogSmsProvider, NoopSmsProvider, createSmsProvider };
