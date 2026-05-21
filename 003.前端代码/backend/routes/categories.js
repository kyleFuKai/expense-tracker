const express = require('express');
const pool = require('../config/db');
const { authMiddleware } = require('../middleware/auth');

const router = express.Router();

// GET /api/categories — 获取分类列表
router.get('/', authMiddleware, async (req, res) => {
    const { type } = req.query; // expense / income
    if (type && !['EXPENSE', 'INCOME'].includes(type.toUpperCase())) {
        return res.status(400).json({ code: 400, msg: '分类类型不合法' });
    }
    try {
        const conditions = ['user_id = ? OR user_id = 0'];
        const params = [req.user.id];

        if (type) {
            conditions.push('type = ?');
            params.push(type.toUpperCase());
        }

        const where = 'WHERE ' + conditions.join(' AND ');
        const [rows] = await pool.query(
            `SELECT id, name, icon, type, parent_id, sort_order, is_preset, is_archived, user_id, created_at
             FROM category
             ${where} AND is_archived = 0
             ORDER BY sort_order ASC, id ASC`,
            params
        );
        res.json({ code: 0, data: rows });
    } catch (err) {
        console.error('get categories error:', err);
        res.status(500).json({ code: 500, msg: '获取分类失败' });
    }
});

// POST /api/categories — 创建自定义分类
router.post('/', authMiddleware, async (req, res) => {
    const { name, icon, type, parent_id, sort_order } = req.body;
    if (!name || !type) {
        return res.status(400).json({ code: 400, msg: '分类名称和类型不能为空' });
    }
    try {
        const [result] = await pool.query(
            `INSERT INTO category (name, icon, type, parent_id, sort_order, is_preset, user_id)
             VALUES (?, ?, ?, ?, ?, 0, ?)`,
            [name, icon || '', type.toUpperCase(), parent_id || 0, sort_order || 0, req.user.id]
        );
        res.json({ code: 0, data: { id: result.insertId } });
    } catch (err) {
        console.error('create category error:', err);
        res.status(500).json({ code: 500, msg: '创建分类失败' });
    }
});

// PUT /api/categories/:id — 更新分类
router.put('/:id', authMiddleware, async (req, res) => {
    const { name, icon, sort_order } = req.body;
    try {
        const [[cat]] = await pool.query(
            'SELECT id FROM category WHERE id = ? AND user_id = ?',
            [req.params.id, req.user.id]
        );
        if (!cat) {
            return res.status(404).json({ code: 404, msg: '分类不存在' });
        }
        const fields = [];
        const params = [];
        if (name) { fields.push('name = ?'); params.push(name); }
        if (icon !== undefined) { fields.push('icon = ?'); params.push(icon); }
        if (sort_order !== undefined) { fields.push('sort_order = ?'); params.push(sort_order); }
        if (fields.length === 0) {
            return res.status(400).json({ code: 400, msg: '没有需要更新的字段' });
        }
        params.push(req.params.id);
        await pool.query(`UPDATE category SET ${fields.join(', ')} WHERE id = ?`, params);
        res.json({ code: 0, msg: '更新成功' });
    } catch (err) {
        console.error('update category error:', err);
        res.status(500).json({ code: 500, msg: '更新分类失败' });
    }
});

// DELETE /api/categories/:id — 归档分类（软删除）
router.delete('/:id', authMiddleware, async (req, res) => {
    try {
        const [[cat]] = await pool.query(
            'SELECT id FROM category WHERE id = ? AND user_id = ? AND is_preset = 0',
            [req.params.id, req.user.id]
        );
        if (!cat) {
            return res.status(404).json({ code: 404, msg: '分类不存在或为系统预设分类' });
        }
        // 检查是否有关联账单
        const [[billCount]] = await pool.query(
            'SELECT COUNT(*) AS cnt FROM bill WHERE category_id = ?',
            [req.params.id]
        );
        if (billCount.cnt > 0) {
            // 有账单只能归档
            await pool.query('UPDATE category SET is_archived = 1 WHERE id = ?', [req.params.id]);
            return res.json({ code: 0, msg: '分类已归档' });
        }
        await pool.query('DELETE FROM category WHERE id = ?', [req.params.id]);
        res.json({ code: 0, msg: '删除成功' });
    } catch (err) {
        console.error('delete category error:', err);
        res.status(500).json({ code: 500, msg: '删除分类失败' });
    }
});

module.exports = router;
