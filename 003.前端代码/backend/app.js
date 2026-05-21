require('dotenv').config();
const path = require('path');
const express = require('express');
const authRoutes = require('./routes/auth');
const billRoutes = require('./routes/bills');
const userRoutes = require('./routes/user');
const categoryRoutes = require('./routes/categories');
const budgetRoutes = require('./routes/budgets');

const app = express();
const PORT = process.env.PORT || 3000;

// 前端静态文件（finance 目录）
app.use(express.static(path.join(__dirname, '../finance')));

// 用户上传文件（头像）
app.use('/uploads', express.static(path.join(__dirname, '../finance/uploads')));

// 解析 JSON 请求体
app.use(express.json());

// API 路由
app.use('/api/auth', authRoutes);
app.use('/api/bills', billRoutes);
app.use('/api/user', userRoutes);
app.use('/api/categories', categoryRoutes);
app.use('/api/budgets', budgetRoutes);

// GET /api/health — 健康检查
app.get('/api/health', (req, res) => {
    res.json({ code: 0, data: { status: 'ok', timestamp: Date.now() } });
});

// 404 处理（API 接口不存在）
app.use((req, res) => {
    res.status(404).json({ code: 404, msg: '接口不存在' });
});

app.listen(PORT, () => {
    console.log(`Server running on http://localhost:${PORT}`);
    console.log(`Frontend: http://localhost:${PORT}/index.html`);
    console.log(`API:      http://localhost:${PORT}/api/auth`);
});
