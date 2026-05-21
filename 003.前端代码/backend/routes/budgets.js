const express = require('express');
const pool = require('../config/db');
const { authMiddleware } = require('../middleware/auth');

const router = express.Router();

// GET /api/budgets — 获取预算列表
router.get('/', authMiddleware, async (req, res) => {
    try {
        const { type } = req.query; // 'total' or 'category', default: all
        let sql = `SELECT b.id, b.user_id, b.category_id, b.amount, b.period, b.start_date, b.end_date, b.is_active, b.created_at, b.updated_at`;

        // Include category info for non-total budgets
        sql += ` FROM budget b`;
        const conditions = ['b.user_id = ?'];
        const params = [req.user.id];

        if (type === 'total') {
            conditions.push('b.category_id = 0');
        } else if (type === 'category') {
            conditions.push('b.category_id != 0');
        }

        conditions.push('b.is_active = 1');
        const where = 'WHERE ' + conditions.join(' AND ');

        sql += ` ${where} ORDER BY b.category_id ASC`;
        const [rows] = await pool.query(sql, params);

        // Attach category name/icon for category budgets
        const categoryIds = rows.filter(r => r.category_id !== 0).map(r => r.category_id);
        let categories = {};
        if (categoryIds.length > 0) {
            const placeholders = categoryIds.map(() => '?').join(',');
            const [catRows] = await pool.query(
                `SELECT id, name, icon FROM category WHERE id IN (${placeholders})`,
                categoryIds
            );
            catRows.forEach(c => { categories[c.id] = c; });
        }

        const data = rows.map(b => ({
            ...b,
            category_name: b.category_id !== 0 ? (categories[b.category_id]?.name || '') : null,
            category_icon: b.category_id !== 0 ? (categories[b.category_id]?.icon || '') : null
        }));

        res.json({ code: 0, data });
    } catch (err) {
        console.error('get budgets error:', err);
        res.status(500).json({ code: 500, msg: '获取预算失败' });
    }
});

// GET /api/budgets/dashboard — 预算仪表盘（总预算 + 本月已支出 + 分项进度）
router.get('/dashboard', authMiddleware, async (req, res) => {
    try {
        const month = req.query.month || new Date().toISOString().slice(0, 7); // YYYY-MM

        // 获取当前月的总预算
        const [[totalBudget]] = await pool.query(
            `SELECT amount FROM budget WHERE user_id = ? AND category_id = 0 AND is_active = 1
             AND start_date <= ? AND (end_date IS NULL OR end_date >= ?)
             AND period = 'MONTHLY'`,
            [req.user.id, month + '-28', month + '-01']
        );

        // 获取分项预算
        const [categoryBudgets] = await pool.query(
            `SELECT b.id, b.category_id, b.amount, c.name, c.icon
             FROM budget b
             LEFT JOIN category c ON b.category_id = c.id
             WHERE b.user_id = ? AND b.category_id != 0 AND b.is_active = 1
             AND b.start_date <= ? AND (b.end_date IS NULL OR b.end_date >= ?)
             AND b.period = 'MONTHLY'`,
            [req.user.id, month + '-28', month + '-01']
        );

        // 本月支出（按分类）
        const [expenseByCategory] = await pool.query(
            `SELECT category_id, COALESCE(SUM(amount), 0) AS spent
             FROM bill
             WHERE user_id = ? AND type = 'EXPENSE'
             AND DATE_FORMAT(bill_time, '%Y-%m') = ?
             GROUP BY category_id`,
            [req.user.id, month]
        );

        // 本月总支出
        const [[monthExpense]] = await pool.query(
            `SELECT COALESCE(SUM(amount), 0) AS total FROM bill
             WHERE user_id = ? AND type = 'EXPENSE'
             AND DATE_FORMAT(bill_time, '%Y-%m') = ?`,
            [req.user.id, month]
        );

        const spent = parseFloat(monthExpense.total);
        const totalBudgetAmount = totalBudget ? parseFloat(totalBudget.amount) : 0;
        const remaining = totalBudgetAmount - spent;
        const percent = totalBudgetAmount > 0 ? Math.round((spent / totalBudgetAmount) * 100) : 0;

        // 分项进度
        const expenseMap = {};
        expenseByCategory.forEach(e => { expenseMap[e.category_id] = parseFloat(e.spent); });

        const categoryProgress = categoryBudgets.map(cb => {
            const budgetAmount = parseFloat(cb.amount);
            const categorySpent = expenseMap[cb.category_id] || 0;
            const pct = budgetAmount > 0 ? Math.round((categorySpent / budgetAmount) * 100) : 0;
            return {
                budget_id: cb.id,
                cat_id: cb.category_id,
                category_name: cb.name,
                category_icon: cb.icon,
                budget_amount: budgetAmount,
                spent: categorySpent,
                percent: pct,
                remaining: budgetAmount - categorySpent
            };
        });

        res.json({
            code: 0,
            data: {
                total_budget: totalBudgetAmount,
                spent: spent,
                remaining: remaining,
                percent: percent,
                categories: categoryProgress
            }
        });
    } catch (err) {
        console.error('budget dashboard error:', err);
        res.status(500).json({ code: 500, msg: '获取预算数据失败' });
    }
});

// POST /api/budgets — 创建/更新预算
router.post('/', authMiddleware, async (req, res) => {
    const { category_id, amount, period, start_date, end_date } = req.body;
    if (!amount || amount <= 0) {
        return res.status(400).json({ code: 400, msg: '预算金额必须大于0' });
    }

    try {
        const catId = category_id ? parseInt(category_id) : 0;
        const pd = period || 'MONTHLY';
        const sd = start_date || new Date().toISOString().slice(0, 10);

        // 检查是否已有相同条件的预算，有则更新
        const [[existing]] = await pool.query(
            `SELECT id FROM budget WHERE user_id = ? AND category_id = ? AND period = ? AND is_active = 1`,
            [req.user.id, catId, pd]
        );

        if (existing) {
            await pool.query(
                `UPDATE budget SET amount = ?, start_date = ?, end_date = ? WHERE id = ?`,
                [parseFloat(amount), sd, end_date || null, existing.id]
            );
            res.json({ code: 0, data: { id: existing.id }, msg: '更新成功' });
        } else {
            const [result] = await pool.query(
                `INSERT INTO budget (user_id, category_id, amount, period, start_date, end_date) VALUES (?, ?, ?, ?, ?, ?)`,
                [req.user.id, catId, parseFloat(amount), pd, sd, end_date || null]
            );
            res.json({ code: 0, data: { id: result.insertId }, msg: '设置成功' });
        }
    } catch (err) {
        console.error('create budget error:', err);
        res.status(500).json({ code: 500, msg: '设置预算失败' });
    }
});

// DELETE /api/budgets/:id — 停用预算
router.delete('/:id', authMiddleware, async (req, res) => {
    try {
        const [[budget]] = await pool.query(
            'SELECT id FROM budget WHERE id = ? AND user_id = ?',
            [req.params.id, req.user.id]
        );
        if (!budget) {
            return res.status(404).json({ code: 404, msg: '预算不存在' });
        }
        await pool.query(
            `UPDATE budget SET is_active = 0 WHERE id = ? AND user_id = ?`,
            [req.params.id, req.user.id]
        );
        res.json({ code: 0, msg: '已停用' });
    } catch (err) {
        console.error('delete budget error:', err);
        res.status(500).json({ code: 500, msg: '操作失败' });
    }
});

module.exports = router;
