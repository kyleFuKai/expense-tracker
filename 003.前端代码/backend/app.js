require('dotenv').config();
const path = require('path');
const cors = require('cors');
const express = require('express');
const rateLimit = require('express-rate-limit');
const authRoutes = require('./routes/auth');
const billRoutes = require('./routes/bills');
const userRoutes = require('./routes/user');
const categoryRoutes = require('./routes/categories');
const budgetRoutes = require('./routes/budgets');

const app = express();
const PORT = process.env.PORT || 3000;

// CORS 跨域支持
app.use(cors());

// Auth rate limiter: 30 requests / 15 min per IP
const authLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 30,
    standardHeaders: true,
    legacyHeaders: false,
    message: { code: 429, msg: '请求过于频繁，请稍后再试' }
});

// Global API rate limiter: 300 requests / 15 min per IP
const globalLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 300,
    standardHeaders: true,
    legacyHeaders: false,
    message: { code: 429, msg: '请求过于频繁，请稍后再试' }
});

// 前端静态文件（finance 目录）
app.use(express.static(path.join(__dirname, '../finance')));

// 用户上传文件（头像）
app.use('/uploads', express.static(path.join(__dirname, '../finance/uploads')));

// 解析 JSON 请求体
app.use(express.json());

// API 路由
app.use('/api/auth', authLimiter, authRoutes);
app.use('/api/bills', globalLimiter, billRoutes);
app.use('/api/user', globalLimiter, userRoutes);
app.use('/api/categories', globalLimiter, categoryRoutes);
app.use('/api/budgets', globalLimiter, budgetRoutes);

// GET /api/health — 健康检查
app.get('/api/health', (req, res) => {
    res.json({ code: 0, data: { status: 'ok', timestamp: Date.now() } });
});

// 404 处理（API 接口不存在）
app.use((req, res) => {
    res.status(404).json({ code: 404, msg: '接口不存在' });
});

// 全局错误处理中间件
app.use((err, req, res, next) => {
    console.error(err);
    res.status(err.status || 500).json({ code: err.status || 500, msg: '服务器错误' });
});

app.listen(PORT, () => {
    console.log(`Server running on http://localhost:${PORT}`);
    console.log(`Frontend: http://localhost:${PORT}/index.html`);
    console.log(`API:      http://localhost:${PORT}/api/auth`);
});
