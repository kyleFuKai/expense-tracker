const express = require('express');
const bcrypt = require('bcryptjs');
const crypto = require('crypto');
const jwt = require('jsonwebtoken');
const pool = require('../config/db');
const { InMemoryLoginAttemptStore } = require('../store/loginAttemptStore');
const { InMemorySmsCodeStore } = require('../store/smsCodeStore');
const { createSmsProvider } = require('../service/smsProvider');

const router = express.Router();

// 业务常量（与 contracts/openapi.yaml x-constants 段对齐，详见 README.md）
const SMS_CODE_TTL_SECONDS = 5 * 60;
const SMS_CODE_SEND_INTERVAL_MS = 60 * 1000;
const SMS_CODE_LENGTH = 6;

const loginAttemptStore = new InMemoryLoginAttemptStore({
    windowMs: 15 * 60 * 1000,
    maxAttempts: 5,
});
const smsCodeStore = new InMemorySmsCodeStore();
const smsProvider = createSmsProvider();

/** 生成 6 位数字验证码。用 crypto 而不是 Math.random —— 验证码是安全敏感数据。 */
function generateSmsCode() {
    // 0..999_999 范围内均匀
    const n = crypto.randomInt(0, 1_000_000);
    return n.toString().padStart(SMS_CODE_LENGTH, '0');
}

/** 把手机号规整化：去空白。空字符串返回 null。 */
function normalizePhone(phone) {
    if (typeof phone !== 'string') return null;
    const trimmed = phone.replace(/\s/g, '');
    return trimmed.length > 0 ? trimmed : null;
}

// POST /api/auth/register — 注册
router.post('/register', async (req, res) => {
    const { phone, password, nickname } = req.body;

    if (!phone || !password) {
        return res.status(400).json({ code: 400, msg: '手机号和密码不能为空' });
    }
    if (!/^\d{8,15}$/.test(phone.replace(/\s/g, ''))) {
        return res.status(400).json({ code: 400, msg: '手机号格式不正确' });
    }
    if (password.length < 6 || password.length > 20) {
        return res.status(400).json({ code: 400, msg: '密码长度需为6-20位' });
    }
    if (!/[a-z]/.test(password) || !/[A-Z]/.test(password) || !/\d/.test(password) || !/[^a-zA-Z0-9]/.test(password)) {
        return res.status(400).json({ code: 400, msg: '密码需包含大小写字母、数字和特殊字符' });
    }

    try {
        const [existing] = await pool.query('SELECT id FROM user WHERE phone = ?', [phone]);
        if (existing.length > 0) {
            return res.status(409).json({ code: 409, msg: '操作失败' });
        }

        const hash = await bcrypt.hash(password, 10);
        const displayNickname = nickname || phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
        const [result] = await pool.query(
            'INSERT INTO user (phone, password_hash, nickname) VALUES (?, ?, ?)',
            [phone, hash, displayNickname]
        );

        const token = jwt.sign({ id: result.insertId, phone }, process.env.JWT_SECRET, { expiresIn: '7d' });
        res.json({ code: 0, data: { token, userId: result.insertId } });
    } catch (err) {
        console.error('register error:', err);
        res.status(500).json({ code: 500, msg: '注册失败' });
    }
});

// POST /api/auth/login — 登录
router.post('/login', async (req, res) => {
    const phone = normalizePhone(req.body.phone);
    const { password } = req.body;

    if (!phone || !password) {
        return res.status(400).json({ code: 400, msg: '手机号和密码不能为空' });
    }
    if (!/^\d{8,15}$/.test(phone)) {
        return res.status(400).json({ code: 400, msg: '手机号格式不正确' });
    }

    // 账号锁定：达到 5 次失败后 15 分钟内拒绝，提示剩余秒数（向上取整）
    const lockedMs = loginAttemptStore.lockedRemainingMs(phone);
    if (lockedMs > 0) {
        const remainingSeconds = Math.ceil(lockedMs / 1000);
        return res.status(429).json({ code: 429, msg: `登录失败次数过多，请 ${remainingSeconds} 秒后再试` });
    }

    try {
        const [rows] = await pool.query('SELECT id, phone, password_hash, nickname FROM user WHERE phone = ?', [phone]);
        if (rows.length === 0) {
            // 不区分"用户不存在"和"密码错"——反枚举
            loginAttemptStore.recordFailure(phone);
            return res.status(401).json({ code: 401, msg: '手机号或密码错误' });
        }

        const user = rows[0];
        if (!await bcrypt.compare(password, user.password_hash)) {
            loginAttemptStore.recordFailure(phone);
            return res.status(401).json({ code: 401, msg: '手机号或密码错误' });
        }

        // 登录成功：清空失败计数
        loginAttemptStore.clear(phone);

        const token = jwt.sign({ id: user.id, phone: user.phone }, process.env.JWT_SECRET, { expiresIn: '7d' });
        res.json({
            code: 0,
            data: {
                token,
                userId: user.id,
                nickname: user.nickname
            }
        });
    } catch (err) {
        console.error('login error:', err);
        res.status(500).json({ code: 500, msg: '登录失败' });
    }
});

// POST /api/auth/login-sms — 短信验证码登录 (预留)
router.post('/login-sms', async (req, res) => {
    const { phone, code } = req.body;
    // TODO: 验证码校验逻辑（V1.1 实现）
    res.status(501).json({ code: 501, msg: '短信登录暂未开放' });
});

// POST /api/auth/send-sms-code — 发送短信验证码
router.post('/send-sms-code', async (req, res) => {
    const phone = normalizePhone(req.body.phone);
    if (!phone) {
        return res.status(400).json({ code: 400, msg: '手机号格式不正确' });
    }
    if (!/^\d{8,15}$/.test(phone)) {
        return res.status(400).json({ code: 400, msg: '手机号格式不正确' });
    }

    try {
        // 频控：同一手机号 60s 内只允许发一次
        const code = generateSmsCode();
        const stored = smsCodeStore.putIfNotThrottled(
            phone, code, SMS_CODE_TTL_SECONDS, SMS_CODE_SEND_INTERVAL_MS);
        if (!stored) {
            return res.status(429).json({ code: 429, msg: '发送过于频繁，请稍后再试' });
        }

        // 走通道下发。送达失败回滚存储，避免「码存了但用户收不到」
        let delivered;
        try {
            delivered = smsProvider.send(phone, code);
        } catch (e) {
            smsCodeStore.remove(phone);
            console.error(`[sms] 通道异常 provider=${smsProvider.name()} phone=${phone}`, e);
            return res.status(503).json({ code: 503, msg: '短信服务暂不可用，请稍后再试' });
        }
        if (!delivered) {
            smsCodeStore.remove(phone);
            return res.status(503).json({ code: 503, msg: '短信功能暂未开放，请联系管理员重置密码' });
        }

        // 防枚举：不告诉调用方手机号是否存在 —— 不管是否注册都返回成功
        // 这里为了与 Java 行为对齐，Java 端"已注册"才发，未注册返 404。
        // 我们额外做反枚举：对未注册的手机号，写入占位码但不让它通过 reset-password
        // （reset-password 内部会校验手机号是否注册），这样响应一致。
        const [rows] = await pool.query('SELECT id FROM user WHERE phone = ?', [phone]);
        if (rows.length === 0) {
            // 移除占位码：让 reset-password 一定失败
            smsCodeStore.remove(phone);
            // 仍然 200 —— 不让攻击者通过响应差异判断手机号是否注册
        }
        return res.json({ code: 0, msg: '已发送' });
    } catch (err) {
        console.error('send-sms-code error:', err);
        return res.status(500).json({ code: 500, msg: '发送失败' });
    }
});

// POST /api/auth/reset-password — 通过短信验证码重置密码
router.post('/reset-password', async (req, res) => {
    const phone = normalizePhone(req.body.phone);
    const { smsCode, newPassword } = req.body;

    if (!phone || !smsCode || !newPassword) {
        return res.status(400).json({ code: 400, msg: '参数不完整' });
    }
    if (!/^\d{8,15}$/.test(phone)) {
        return res.status(400).json({ code: 400, msg: '手机号格式不正确' });
    }
    if (newPassword.length < 6 || newPassword.length > 20) {
        return res.status(400).json({ code: 400, msg: '密码长度需为6-20位' });
    }
    if (!/[a-z]/.test(newPassword) || !/[A-Z]/.test(newPassword)
        || !/\d/.test(newPassword) || !/[^a-zA-Z0-9]/.test(newPassword)) {
        return res.status(400).json({ code: 400, msg: '密码需包含大小写字母、数字和特殊字符' });
    }

    try {
        const storedCode = smsCodeStore.get(phone);
        if (!storedCode) {
            return res.status(400).json({ code: 400, msg: '验证码已过期' });
        }
        if (storedCode !== smsCode) {
            return res.status(400).json({ code: 400, msg: '验证码错误' });
        }

        const [rows] = await pool.query('SELECT id FROM user WHERE phone = ?', [phone]);
        if (rows.length === 0) {
            // 既然 send-sms 不会对未注册手机号发码，到这步一定是有码的；
            // 但保险起见还是做一次校验。
            return res.status(404).json({ code: 404, msg: '手机号未注册' });
        }

        const hash = await bcrypt.hash(newPassword, 10);
        await pool.query('UPDATE user SET password_hash = ? WHERE phone = ?', [hash, phone]);
        smsCodeStore.remove(phone);
        console.info(`[auth] 密码重置成功 phone=${phone}`);
        res.json({ code: 0, msg: '密码重置成功' });
    } catch (err) {
        console.error('reset-password error:', err);
        res.status(500).json({ code: 500, msg: '重置失败' });
    }
});

module.exports = router;
