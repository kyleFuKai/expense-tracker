const express = require('express');
const bcrypt = require('bcryptjs');
const multer = require('multer');
const path = require('path');
const pool = require('../config/db');
const { authMiddleware } = require('../middleware/auth');

const router = express.Router();

// 头像上传配置
const storage = multer.diskStorage({
    destination: path.join(__dirname, '../../finance/uploads/avatars'),
    filename: (req, file, cb) => {
        cb(null, `user_${req.user.id}_${Date.now()}${path.extname(file.originalname)}`);
    }
});
const upload = multer({
    storage,
    limits: { fileSize: 2 * 1024 * 1024 },
    fileFilter: (req, file, cb) => {
        if (/^image\/(jpeg|jpg|png|gif|webp)$/.test(file.mimetype)) {
            cb(null, true);
        } else {
            cb(new Error('仅支持图片格式'));
        }
    }
});

// GET /api/user/profile — 获取用户信息
router.get('/profile', authMiddleware, async (req, res) => {
    try {
        const [[user]] = await pool.query(
            'SELECT id, nickname, avatar_url, phone, country_code, currency, theme, status, created_at FROM user WHERE id = ?',
            [req.user.id]
        );
        if (!user) {
            return res.status(404).json({ code: 404, msg: '用户不存在' });
        }
        res.json({ code: 0, data: user });
    } catch (err) {
        console.error('get profile error:', err);
        res.status(500).json({ code: 500, msg: '获取用户信息失败' });
    }
});

// PUT /api/user/profile — 更新用户信息
router.put('/profile', authMiddleware, async (req, res) => {
    const { nickname, avatar_url, currency, theme } = req.body;
    try {
        const fields = [];
        const params = [];
        if (nickname) {
            if (nickname.length > 50) {
                return res.status(400).json({ code: 400, msg: '昵称长度不能超过 50 个字符' });
            }
            fields.push('nickname = ?'); params.push(nickname);
        }
        if (avatar_url !== undefined) { fields.push('avatar_url = ?'); params.push(avatar_url); }
        if (currency) { fields.push('currency = ?'); params.push(currency); }
        if (theme) { fields.push('theme = ?'); params.push(theme); }

        if (fields.length === 0) {
            return res.status(400).json({ code: 400, msg: '没有需要更新的字段' });
        }

        params.push(req.user.id);
        await pool.query(`UPDATE user SET ${fields.join(', ')} WHERE id = ?`, params);
        res.json({ code: 0, msg: '更新成功' });
    } catch (err) {
        console.error('update profile error:', err);
        res.status(500).json({ code: 500, msg: '更新失败' });
    }
});

// PUT /api/user/bind-phone — 绑定手机号
router.put('/bind-phone', authMiddleware, async (req, res) => {
    const { phone } = req.body;
    if (!phone) {
        return res.status(400).json({ code: 400, msg: '手机号不能为空' });
    }
    if (!/^\d{8,15}$/.test(phone.replace(/\s/g, ''))) {
        return res.status(400).json({ code: 400, msg: '手机号格式不正确' });
    }
    try {
        const [existing] = await pool.query('SELECT id FROM user WHERE phone = ? AND id != ?', [phone, req.user.id]);
        if (existing.length > 0) {
            return res.status(409).json({ code: 409, msg: '该手机号已被其他账号绑定' });
        }
        await pool.query('UPDATE user SET phone = ? WHERE id = ?', [phone, req.user.id]);
        res.json({ code: 0, msg: '绑定成功' });
    } catch (err) {
        console.error('bind phone error:', err);
        res.status(500).json({ code: 500, msg: '绑定失败' });
    }
});

// PUT /api/user/unbind-phone — 解绑手机号
router.put('/unbind-phone', authMiddleware, async (req, res) => {
    try {
        await pool.query('UPDATE user SET phone = \'\', country_code = \'+86\' WHERE id = ?', [req.user.id]);
        res.json({ code: 0, msg: '解绑成功' });
    } catch (err) {
        console.error('unbind phone error:', err);
        res.status(500).json({ code: 500, msg: '解绑失败' });
    }
});

// PUT /api/user/password — 修改密码
router.put('/password', authMiddleware, async (req, res) => {
    const { old_password, new_password } = req.body;
    if (!old_password || !new_password) {
        return res.status(400).json({ code: 400, msg: '旧密码和新密码不能为空' });
    }
    if (new_password.length < 6 || new_password.length > 20) {
        return res.status(400).json({ code: 400, msg: '密码长度需为6-20位' });
    }
    if (!/[a-z]/.test(new_password) || !/[A-Z]/.test(new_password) || !/\d/.test(new_password) || !/[^a-zA-Z0-9]/.test(new_password)) {
        return res.status(400).json({ code: 400, msg: '密码需包含大小写字母、数字和特殊字符' });
    }
    try {
        const [[user]] = await pool.query('SELECT password_hash FROM user WHERE id = ?', [req.user.id]);
        if (!user || !await bcrypt.compare(old_password, user.password_hash)) {
            return res.status(401).json({ code: 401, msg: '旧密码错误' });
        }
        const hash = await bcrypt.hash(new_password, 10);
        await pool.query('UPDATE user SET password_hash = ? WHERE id = ?', [hash, req.user.id]);
        res.json({ code: 0, msg: '密码修改成功' });
    } catch (err) {
        console.error('change password error:', err);
        res.status(500).json({ code: 500, msg: '修改密码失败' });
    }
});

// POST /api/user/upload-avatar — 上传头像
router.post('/upload-avatar', authMiddleware, upload.single('avatar'), (req, res) => {
    if (!req.file) {
        return res.status(400).json({ code: 400, msg: '未选择文件' });
    }
    const avatarUrl = `/uploads/avatars/${req.file.filename}`;
    pool.query('UPDATE user SET avatar_url = ? WHERE id = ?', [avatarUrl, req.user.id])
        .then(() => {
            res.json({ code: 0, data: { url: avatarUrl } });
        })
        .catch((err) => {
            console.error('save avatar error:', err);
            res.status(500).json({ code: 500, msg: '保存头像失败' });
        });
});

module.exports = router;
