const express = require('express');
const pool = require('../config/db');
const { authMiddleware } = require('../middleware/auth');
const logger = require('../utils/logger'); // 新增日志工具

const router = express.Router();
router.use(authMiddleware);

// GET /api/bills — 获取账单列表（支持按月/分类筛选，分页）
router.get('/', async (req, res) => {
    const { user } = req;
    const month = req.query.month; // 格式: 2026-05
    const categoryId = req.query.category_id;
    const type = req.query.type; // expense / income
    const page = Math.max(1, parseInt(req.query.page) || 1);
    const pageSize = Math.max(1, Math.min(100, parseInt(req.query.pageSize) || 50));

    try {
        const conditions = ['user_id = ?'];
        const params = [user.id];

        if (month) {
            conditions.push('DATE_FORMAT(bill_time, "%Y-%m") = ?');
            params.push(month);
        }
        if (categoryId) {
            conditions.push('category_id = ?');
            params.push(parseInt(categoryId));
        }
        if (type) {
            conditions.push('type = ?');
            params.push(type.toUpperCase());
        }

        const where = 'WHERE ' + conditions.join(' AND ');
        // WHERE clause for queries with table alias 'b' (e.g., bill b)
        const whereB = 'WHERE ' + conditions.map(function (c) {
            // Already has table prefix or function wrapper — keep as-is
            if (/^[a-z_]+\./.test(c) || c.indexOf('(') !== -1) return c;
            return 'b.' + c;
        }).join(' AND ');
        const offset = (page - 1) * pageSize;

        const [rows] = await pool.query(
            `SELECT b.*, c.name AS category_name, c.icon AS category_icon
             FROM bill b
             LEFT JOIN category c ON b.category_id = c.id
             ${whereB}
             ORDER BY b.bill_time DESC
             LIMIT ? OFFSET ?`,
            [...params, pageSize, offset]
        );

        const [[{ total }]] = await pool.query(
            `SELECT COUNT(*) AS total FROM bill b ${whereB}`,
            params
        );

        res.json({ code: 0, data: { list: rows, total, page, pageSize } });
    } catch (err) {
        logger.error('get bills error:', err);
        res.status(500).json({ code: 500, msg: '获取账单失败' });
    }
});

// GET /api/bills/:id — 获取账单详情
router.get('/:id', async (req, res) => {
    const { user } = req;
    try {
        const [[bill]] = await pool.query(
            `SELECT b.*, c.name AS category_name, c.icon AS category_icon
             FROM bill b
             LEFT JOIN category c ON b.category_id = c.id
             WHERE b.id = ? AND b.user_id = ?`,
            [req.params.id, user.id]
        );
        if (!bill) {
            return res.status(404).json({ code: 404, msg: '账单不存在' });
        }
        res.json({ code: 0, data: bill });
    } catch (err) {
        logger.error('get bill error:', err);
        res.status(500).json({ code: 500, msg: '获取账单失败' });
    }
});

// POST /api/bills — 创建账单
router.post('/', async (req, res) => {
    const { user } = req;
    const { type, amount, category_id, remark, bill_time } = req.body;

    if (!type || !amount || !category_id) {
        return res.status(400).json({ code: 400, msg: '类型、金额、分类不能为空' });
    }
    if (remark && remark.length > 200) {
        return res.status(400).json({ code: 400, msg: '备注不能超过 200 个字符' });
    }

    try {
        const [result] = await pool.query(
            `INSERT INTO bill (user_id, type, amount, category_id, remark, bill_time)
             VALUES (?, ?, ?, ?, ?, ?)`,
            [user.id, type.toUpperCase(), parseFloat(amount), parseInt(category_id), remark || '', bill_time || new Date()]
        );
        res.json({ code: 0, data: { id: result.insertId } });
    } catch (err) {
        logger.error('create bill error:', err);
        res.status(500).json({ code: 500, msg: '创建账单失败' });
    }
});

// PUT /api/bills/:id — 更新账单
router.put('/:id', async (req, res) => {
    const { user } = req;
    const { type, amount, category_id, remark, bill_time } = req.body;

    if (remark && remark.length > 200) {
        return res.status(400).json({ code: 400, msg: '备注不能超过 200 个字符' });
    }

    try {
        const [[existing]] = await pool.query(
            'SELECT id FROM bill WHERE id = ? AND user_id = ?',
            [req.params.id, user.id]
        );
        if (!existing) {
            return res.status(404).json({ code: 404, msg: '账单不存在' });
        }

        const fields = [];
        const params = [];
        if (type) {
            if (!['EXPENSE', 'INCOME'].includes(type.toUpperCase())) {
                return res.status(400).json({ code: 400, msg: '类型不合法' });
            }
            fields.push('type = ?'); params.push(type.toUpperCase());
        }
        if (amount !== undefined) {
            if (parseFloat(amount) <= 0) {
                return res.status(400).json({ code: 400, msg: '金额必须大于 0' });
            }
            fields.push('amount = ?'); params.push(parseFloat(amount));
        }
        if (category_id) { fields.push('category_id = ?'); params.push(parseInt(category_id)); }
        if (remark !== undefined) { fields.push('remark = ?'); params.push(remark); }
        if (bill_time) { fields.push('bill_time = ?'); params.push(bill_time); }

        if (fields.length === 0) {
            return res.status(400).json({ code: 400, msg: '没有需要更新的字段' });
        }

        params.push(req.params.id);
        await pool.query(`UPDATE bill SET ${fields.join(', ')} WHERE id = ?`, params);
        res.json({ code: 0, data: { id: req.params.id } });
    } catch (err) {
        logger.error('update bill error:', err);
        res.status(500).json({ code: 500, msg: '更新账单失败' });
    }
});

// DELETE /api/bills/:id — 删除账单
router.delete('/:id', async (req, res) => {
    const { user } = req;
    try {
        const [[existing]] = await pool.query(
            'SELECT id FROM bill WHERE id = ? AND user_id = ?',
            [req.params.id, user.id]
        );
        if (!existing) {
            return res.status(404).json({ code: 404, msg: '账单不存在' });
        }
        await pool.query('DELETE FROM bill WHERE id = ? AND user_id = ?', [req.params.id, user.id]);
        res.json({ code: 0, data: { id: req.params.id } });
    } catch (err) {
        logger.error('delete bill error:', err);
        res.status(500).json({ code: 500, msg: '删除账单失败' });
    }
});

// GET /api/bills/stats — 月度统计
router.get('/stats/month', async (req, res) => {
    const { user } = req;
    const month = req.query.month; // 格式: 2026-05

    try {
        const conditions = ['user_id = ?'];
        const params = [user.id];

        if (month) {
            conditions.push('DATE_FORMAT(bill_time, "%Y-%m") = ?');
            params.push(month);
        }

        const where = 'WHERE ' + conditions.join(' AND ');
        // WHERE clause for queries with table alias 'b' (e.g., bill b)
        const whereB = 'WHERE ' + conditions.map(function (c) {
            // Already has table prefix or function wrapper — keep as-is
            if (/^[a-z_]+\./.test(c) || c.indexOf('(') !== -1) return c;
            return 'b.' + c;
        }).join(' AND ');

        const [[expense]] = await pool.query(
            `SELECT COALESCE(SUM(amount), 0) AS total, COUNT(*) AS count
             FROM bill ${where} AND type = 'EXPENSE'`,
            params
        );
        const [[income]] = await pool.query(
            `SELECT COALESCE(SUM(amount), 0) AS total, COUNT(*) AS count
             FROM bill ${where} AND type = 'INCOME'`,
            params
        );

        // 按日分组统计（近30天趋势用）
        const [dailyStats] = await pool.query(
            `SELECT DATE(bill_time) AS date,
                    COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS expense,
                    COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS income
             FROM bill ${where}
             GROUP BY DATE(bill_time)
             ORDER BY date DESC
             LIMIT 30`,
            params
        );

        // 按分类统计
        const [categoryStats] = await pool.query(
            `SELECT c.id, c.name, c.icon,
                    COALESCE(SUM(b.amount), 0) AS total,
                    COUNT(*) AS count
             FROM bill b
             LEFT JOIN category c ON b.category_id = c.id
             ${whereB} AND b.type = 'EXPENSE'
             GROUP BY c.id, c.name, c.icon
             ORDER BY total DESC`,
            params
        );

        res.json({
            code: 0,
            data: {
                expense: { total: parseFloat(expense.total), count: expense.count },
                income: { total: parseFloat(income.total), count: income.count },
                daily: dailyStats.map(function (d) { return { date: d.date, expense: parseFloat(d.expense), income: parseFloat(d.income) }; }),
                categories: categoryStats.map(function (c) { return { id: c.id, name: c.name, icon: c.icon, total: parseFloat(c.total), count: c.count }; })
            }
        });
    } catch (err) {
        logger.error('stats error:', err);
        res.status(500).json({ code: 500, msg: '获取统计失败' });
    }
});

module.exports = router;
