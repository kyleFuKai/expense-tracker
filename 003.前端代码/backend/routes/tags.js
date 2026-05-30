const express = require('express');
const pool = require('../config/db');
const { authMiddleware } = require('../middleware/auth');
const logger = require('../utils/logger');

const router = express.Router();
router.use(authMiddleware);

// GET /finance/tags — 获取用户所有标签
router.get('/', async (req, res) => {
    const { user } = req;
    try {
        const [tags] = await pool.query(
            'SELECT bt.id, bt.name, COUNT(btr.bill_id) AS bill_count ' +
            'FROM bill_tag bt LEFT JOIN bill_tag_rel btr ON bt.id = btr.tag_id ' +
            'WHERE bt.user_id = ? GROUP BY bt.id, bt.name ORDER BY bt.created_at ASC',
            [user.id]
        );
        res.json({ code: 0, data: { list: tags } });
    } catch (err) {
        logger.error('get tags error:', err);
        res.status(500).json({ code: 500, msg: '获取标签失败' });
    }
});

// POST /finance/tags — 创建标签
router.post('/', async (req, res) => {
    const { user } = req;
    const { name } = req.body;

    if (!name || !name.trim()) {
        return res.status(400).json({ code: 400, msg: '标签名不能为空' });
    }
    if (name.trim().length > 16) {
        return res.status(400).json({ code: 400, msg: '标签名不能超过 16 个字符' });
    }

    try {
        const [[{ count }]] = await pool.query(
            'SELECT COUNT(*) AS count FROM bill_tag WHERE user_id = ? AND name = ?',
            [user.id, name.trim()]
        );
        if (count > 0) {
            return res.status(400).json({ code: 400, msg: '该标签已存在' });
        }

        const [result] = await pool.query(
            'INSERT INTO bill_tag (user_id, name) VALUES (?, ?)',
            [user.id, name.trim()]
        );
        res.json({ code: 0, data: { id: result.insertId } });
    } catch (err) {
        logger.error('create tag error:', err);
        res.status(500).json({ code: 500, msg: '创建标签失败' });
    }
});

// PUT /finance/tags/:id — 修改标签名
router.put('/:id', async (req, res) => {
    const { user } = req;
    const { name } = req.body;

    if (!name || !name.trim()) {
        return res.status(400).json({ code: 400, msg: '标签名不能为空' });
    }
    if (name.trim().length > 16) {
        return res.status(400).json({ code: 400, msg: '标签名不能超过 16 个字符' });
    }

    try {
        const [[existing]] = await pool.query(
            'SELECT id FROM bill_tag WHERE id = ? AND user_id = ?',
            [req.params.id, user.id]
        );
        if (!existing) {
            return res.status(404).json({ code: 404, msg: '标签不存在' });
        }

        const [[{ count }]] = await pool.query(
            'SELECT COUNT(*) AS count FROM bill_tag WHERE user_id = ? AND name = ? AND id != ?',
            [user.id, name.trim(), req.params.id]
        );
        if (count > 0) {
            return res.status(400).json({ code: 400, msg: '该标签已存在' });
        }

        await pool.query(
            'UPDATE bill_tag SET name = ? WHERE id = ? AND user_id = ?',
            [name.trim(), req.params.id, user.id]
        );
        res.json({ code: 0 });
    } catch (err) {
        logger.error('update tag error:', err);
        res.status(500).json({ code: 500, msg: '更新标签失败' });
    }
});

// DELETE /finance/tags/:id — 删除标签
router.delete('/:id', async (req, res) => {
    const { user } = req;

    try {
        const [[existing]] = await pool.query(
            'SELECT id FROM bill_tag WHERE id = ? AND user_id = ?',
            [req.params.id, user.id]
        );
        if (!existing) {
            return res.status(404).json({ code: 404, msg: '标签不存在' });
        }

        await pool.query('DELETE FROM bill_tag_rel WHERE tag_id = ?', [req.params.id]);
        await pool.query('DELETE FROM bill_tag WHERE id = ? AND user_id = ?', [req.params.id, user.id]);
        res.json({ code: 0 });
    } catch (err) {
        logger.error('delete tag error:', err);
        res.status(500).json({ code: 500, msg: '删除标签失败' });
    }
});

module.exports = router;
