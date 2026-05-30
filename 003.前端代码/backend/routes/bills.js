const express = require('express');
const pool = require('../config/db');
const { authMiddleware } = require('../middleware/auth');
const logger = require('../utils/logger');
const ExcelJS = require('exceljs');

const router = express.Router();
router.use(authMiddleware);

// GET /api/bills — 获取账单列表（支持按月/分类/标签筛选，分页）
router.get('/', async (req, res) => {
    const { user } = req;
    const month = req.query.month; // 格式: 2026-05
    const categoryId = req.query.category_id;
    const type = req.query.type; // expense / income
    const keyword = req.query.keyword; // 备注关键词搜索
    const tagId = req.query.tag_id;
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
        if (keyword) {
            conditions.push('remark LIKE ?');
            params.push(`%${keyword}%`);
        }

        const where = 'WHERE ' + conditions.join(' AND ');
        // WHERE clause for queries with table alias 'b' (e.g., bill b)
        const whereB = 'WHERE ' + conditions.map(function (c) {
            // Already has table prefix or function wrapper — keep as-is
            if (/^[a-z_]+\./.test(c) || c.indexOf('(') !== -1) return c;
            return 'b.' + c;
        }).join(' AND ');
        const offset = (page - 1) * pageSize;

        let query, countQuery, queryParams;

        if (tagId) {
            // Filter by tag_id: INNER JOIN bill_tag_rel
            query = `SELECT b.*, c.name AS category_name, c.icon AS category_icon,
                        (SELECT JSON_ARRAYAGG(JSON_OBJECT('id', bt.id, 'name', bt.name))
                         FROM bill_tag_rel btr
                         JOIN bill_tag bt ON btr.tag_id = bt.id
                         WHERE btr.bill_id = b.id) AS tags
                     FROM bill b
                     LEFT JOIN category c ON b.category_id = c.id
                     INNER JOIN bill_tag_rel btr2 ON b.id = btr2.bill_id AND btr2.tag_id = ?
                     ${whereB.replace(/^WHERE /, 'WHERE btr2.tag_id = ? AND ').replace('user_id = ?', 'b.user_id = ?')}
                     ORDER BY b.bill_time DESC
                     LIMIT ? OFFSET ?`;
            queryParams = [parseInt(tagId), parseInt(tagId), ...params.slice(1), pageSize, offset];

            countQuery = `SELECT COUNT(*) AS total FROM bill b INNER JOIN bill_tag_rel btr2 ON b.id = btr2.bill_id ${whereB.replace(/^WHERE /, 'WHERE btr2.tag_id = ? AND ').replace('user_id = ?', 'b.user_id = ?')}`;
        } else {
            query = `SELECT b.*, c.name AS category_name, c.icon AS category_icon,
                        (SELECT JSON_ARRAYAGG(JSON_OBJECT('id', bt.id, 'name', bt.name))
                         FROM bill_tag_rel btr
                         JOIN bill_tag bt ON btr.tag_id = bt.id
                         WHERE btr.bill_id = b.id) AS tags
                     FROM bill b
                     LEFT JOIN category c ON b.category_id = c.id
                     ${whereB}
                     ORDER BY b.bill_time DESC
                     LIMIT ? OFFSET ?`;
            queryParams = [...params, pageSize, offset];
            countQuery = `SELECT COUNT(*) AS total FROM bill b ${whereB}`;
        }

        const [rows] = await pool.query(query, queryParams);

        const [[{ total }]] = await pool.query(countQuery, tagId ? [parseInt(tagId), ...params.slice(1)] : params);

        const list = rows.map(row => {
            const r = { ...row };
            r.tags = row.tags ? JSON.parse(row.tags) : [];
            return r;
        });

        res.json({ code: 0, data: { list, total, page, pageSize } });
    } catch (err) {
        logger.error('get bills error:', err);
        res.status(500).json({ code: 500, msg: '获取账单失败' });
    }
});

// GET /api/bills/export — 导出账单（CSV/Excel）
router.get('/export', async (req, res) => {
    const { user } = req;
    const format = req.query.format || 'csv';
    const month = req.query.month;
    const type = req.query.type;
    const categoryId = req.query.category_id;
    const keyword = req.query.keyword;
    const startDate = req.query.start_date;
    const endDate = req.query.end_date;

    try {
        const conditions = ['b.user_id = ?'];
        const params = [user.id];

        if (month) {
            conditions.push('DATE_FORMAT(b.bill_time, "%Y-%m") = ?');
            params.push(month);
        }
        if (type) {
            conditions.push('b.type = ?');
            params.push(type.toUpperCase());
        }
        if (categoryId) {
            conditions.push('b.category_id = ?');
            params.push(parseInt(categoryId));
        }
        if (keyword) {
            conditions.push('b.remark LIKE ?');
            params.push(`%${keyword}%`);
        }
        if (startDate) {
            conditions.push('b.bill_time >= ?');
            params.push(startDate);
        }
        if (endDate) {
            conditions.push('b.bill_time <= ?');
            params.push(endDate + ' 23:59:59');
        }

        const where = 'WHERE ' + conditions.join(' AND ');
        const [rows] = await pool.query(
            `SELECT b.bill_time, b.type, c.name AS category_name, b.amount, b.remark,
                    (SELECT GROUP_CONCAT(bt.name SEPARATOR ', ')
                     FROM bill_tag_rel btr
                     JOIN bill_tag bt ON btr.tag_id = bt.id
                     WHERE btr.bill_id = b.id) AS tag_names,
                    b.created_at
             FROM bill b LEFT JOIN category c ON b.category_id = c.id
             ${where}
             ORDER BY b.bill_time DESC
             LIMIT 50001`,
            params
        );

        if (rows.length > 50000) {
            return res.status(400).json({ code: 400, msg: '导出数据超过 50000 行限制，请缩小筛选范围' });
        }

        const filename = buildExportFilename(month, startDate, endDate, format);

        if (format === 'xlsx') {
            await writeExcel(rows, filename, res);
        } else {
            writeCsv(rows, filename, res);
        }
    } catch (err) {
        logger.error('export bills error:', err);
        res.status(500).json({ code: 500, msg: '导出失败' });
    }
});

// GET /api/bills/:id — 获取账单详情
router.get('/:id', async (req, res) => {
    const { user } = req;
    try {
        const [[bill]] = await pool.query(
            `SELECT b.*, c.name AS category_name, c.icon AS category_icon,
                    (SELECT JSON_ARRAYAGG(JSON_OBJECT('id', bt.id, 'name', bt.name))
                     FROM bill_tag_rel btr
                     JOIN bill_tag bt ON btr.tag_id = bt.id
                     WHERE btr.bill_id = b.id) AS tags
             FROM bill b
             LEFT JOIN category c ON b.category_id = c.id
             WHERE b.id = ? AND b.user_id = ?`,
            [req.params.id, user.id]
        );
        if (!bill) {
            return res.status(404).json({ code: 404, msg: '账单不存在' });
        }
        bill.tags = bill.tags ? JSON.parse(bill.tags) : [];
        res.json({ code: 0, data: bill });
    } catch (err) {
        logger.error('get bill error:', err);
        res.status(500).json({ code: 500, msg: '获取账单失败' });
    }
});

// POST /api/bills — 创建账单
router.post('/', async (req, res) => {
    const { user } = req;
    const { type, amount, category_id, remark, bill_time, tag_ids } = req.body;

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
        const billId = result.insertId;

        if (tag_ids && tag_ids.length > 0) {
            if (tag_ids.length > 10) {
                return res.status(400).json({ code: 400, msg: '每笔账单最多关联 10 个标签' });
            }
            const [[{ count }]] = await pool.query(
                'SELECT COUNT(*) AS count FROM bill_tag WHERE id IN (?) AND user_id = ?',
                [tag_ids, user.id]
            );
            if (count !== tag_ids.length) {
                return res.status(400).json({ code: 400, msg: '部分标签不存在' });
            }
            const relValues = tag_ids.map(id => [billId, id]);
            await pool.query('INSERT INTO bill_tag_rel (bill_id, tag_id) VALUES ?', [relValues]);
        }

        res.json({ code: 0, data: { id: billId } });
    } catch (err) {
        logger.error('create bill error:', err);
        res.status(500).json({ code: 500, msg: '创建账单失败' });
    }
});

// PUT /api/bills/:id — 更新账单
router.put('/:id', async (req, res) => {
    const { user } = req;
    const { type, amount, category_id, remark, bill_time, tag_ids } = req.body;

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

        if (fields.length > 0) {
            params.push(req.params.id);
            await pool.query(`UPDATE bill SET ${fields.join(', ')} WHERE id = ?`, params);
        }

        // Handle tag relations (full replacement)
        if (tag_ids !== undefined) {
            if (tag_ids.length > 10) {
                return res.status(400).json({ code: 400, msg: '每笔账单最多关联 10 个标签' });
            }
            await pool.query('DELETE FROM bill_tag_rel WHERE bill_id = ?', [req.params.id]);
            if (tag_ids.length > 0) {
                const [[{ count }]] = await pool.query(
                    'SELECT COUNT(*) AS count FROM bill_tag WHERE id IN (?) AND user_id = ?',
                    [tag_ids, user.id]
                );
                if (count !== tag_ids.length) {
                    return res.status(400).json({ code: 400, msg: '部分标签不存在' });
                }
                const relValues = tag_ids.map(id => [req.params.id, id]);
                await pool.query('INSERT INTO bill_tag_rel (bill_id, tag_id) VALUES ?', [relValues]);
            }
        }

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
        await pool.query('DELETE FROM bill_tag_rel WHERE bill_id = ?', [req.params.id]);
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

const HEADERS = ['账单时间', '类型', '分类', '金额', '备注', '标签', '创建时间'];

function buildExportFilename(month, startDate, endDate, format) {
    let base;
    if (month) {
        base = `bills_${month}`;
    } else if (startDate && endDate) {
        base = `bills_${startDate}_to_${endDate}`;
    } else {
        const today = new Date().toISOString().slice(0, 10).replace(/-/g, '');
        base = `bills_all_${today}`;
    }
    return `${base}.${format}`;
}

function formatTime(time) {
    if (!time) return '';
    if (time instanceof Date) {
        return time.toISOString().replace('T', ' ').slice(0, 19);
    }
    const d = new Date(time);
    if (isNaN(d.getTime())) return String(time);
    return d.toISOString().replace('T', ' ').slice(0, 19);
}

function formatType(type) {
    if (type === 'EXPENSE') return '支出';
    if (type === 'INCOME') return '收入';
    return type || '';
}

function formatAmount(amount) {
    if (amount == null) return '0.00';
    return Number(amount).toFixed(2);
}

function escapeCsv(value) {
    if (value == null || value === 'null') return '';
    const s = String(value);
    if (s.includes(',') || s.includes('"') || s.includes('\n')) {
        return `"${s.replace(/"/g, '""')}"`;
    }
    return s;
}

function writeCsv(rows, filename, res) {
    res.setHeader('Content-Type', 'text/csv; charset=utf-8');
    res.setHeader('Content-Disposition', `attachment; filename="${filename}"`);

    const bom = '﻿';
    const headerLine = HEADERS.join(',');
    res.write(bom + headerLine + '\n');

    for (const row of rows) {
        const cells = [
            formatTime(row.bill_time),
            formatType(row.type),
            escapeCsv(row.category_name),
            formatAmount(row.amount),
            escapeCsv(row.remark),
            escapeCsv(row.tag_names),
            formatTime(row.created_at)
        ];
        res.write(cells.join(',') + '\n');
    }
    res.end();
}

async function writeExcel(rows, filename, res) {
    const wb = new ExcelJS.Workbook();
    const sheet = wb.addWorksheet('账单');

    // Header
    const headerRow = sheet.addRow(HEADERS);
    headerRow.eachCell((cell) => {
        cell.font = { bold: true };
    });

    // Amount column format
    const amountCol = 4;

    for (const row of rows) {
        sheet.addRow([
            formatTime(row.bill_time),
            formatType(row.type),
            row.category_name || '',
            Number(row.amount || 0),
            row.remark || '',
            row.tag_names || '',
            formatTime(row.created_at)
        ]);
        const lastRow = sheet.lastRow;
        if (lastRow) {
            lastRow.getCell(amountCol).numFmt = '#,##0.00';
        }
    }

    res.setHeader('Content-Type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
    res.setHeader('Content-Disposition', `attachment; filename="${filename}"`);
    await wb.xlsx.write(res);
    res.end();
}

module.exports = router;
