const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const pool = require('../config/db');

const router = express.Router();

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

        const hash = bcrypt.hashSync(password, 10);
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
    const { phone, password } = req.body;

    if (!phone || !password) {
        return res.status(400).json({ code: 400, msg: '手机号和密码不能为空' });
    }

    try {
        const [rows] = await pool.query('SELECT id, phone, password_hash, nickname FROM user WHERE phone = ?', [phone]);
        if (rows.length === 0) {
            return res.status(401).json({ code: 401, msg: '手机号或密码错误' });
        }

        const user = rows[0];
        if (!bcrypt.compareSync(password, user.password_hash)) {
            return res.status(401).json({ code: 401, msg: '手机号或密码错误' });
        }

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

module.exports = router;
