# 每日财务管家

> 一款个人财务管理应用，支持账单记录、预算管理和可视化统计 —— 基于 Node.js + Express + MySQL

## 功能特性

- **用户认证** — 手机号注册/登录，JWT Token 认证（7天有效期），bcryptjs 密码加密
- **账单管理** — 记录收入/支出，支持分类、金额、备注；无限滚动分页加载（每页 10 条）
- **分类管理** — 内置 16 个预设收支分类（带图标），支持归档空分类
- **预算管理** — 月度总预算 + 分项预算，进度仪表盘实时追踪
- **统计分析** — 月度收支概览、日趋势折线图、分类饼图
- **用户中心** — 头像上传、昵称修改、币种切换（CNY/USD/EUR）、主题切换（浅色/深色）、手机号绑定

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | 原生 JavaScript、Tailwind CSS（CDN）、Material Symbols 图标 |
| 后端 | Node.js 22 + Express 4 |
| 数据库 | MySQL 8.0 |
| 认证 | JWT (jsonwebtoken) + bcryptjs |
| 文件上传 | Multer（头像，2MB 图片限制） |

## 项目结构

```
.
├── 001.产品PRD(产品经理)/          # 产品需求文档
├── 002.产品UI原型(美术设计)/        # UI原型与设计资源
├── 003.前端代码/
│   ├── backend/                    # Express 后端服务
│   │   ├── app.js                  # 入口文件，挂载路由和静态资源
│   │   ├── config/db.js            # MySQL 连接池
│   │   ├── middleware/auth.js      # JWT 验证中间件
│   │   ├── routes/                 # API 路由
│   │   │   ├── auth.js             # 注册 / 登录接口
│   │   │   ├── bills.js            # 账单 CRUD、统计、分页加载
│   │   │   ├── budgets.js          # 预算创建/更新/删除、进度仪表盘
│   │   │   ├── categories.js       # 分类列表、新增、删除（软归档）
│   │   │   └── user.js             # 用户信息、密码修改、手机绑定、头像上传
│   │   └── utils/logger.js         # 结构化日志工具
│   └── finance/                    # 前端静态页面
│       ├── index.html              # 入口页
│       ├── pages/                  # 页面目录
│       │   ├── home.html           # 首页 — 月度概览 + 无限滚动账单列表
│       │   ├── record.html         # 记账页面
│       │   ├── statistics.html     # 统计页 — 收支图表
│       │   ├── budget.html         # 预算管理
│       │   ├── bill-detail.html    # 账单详情
│       │   ├── category-manage.html # 分类管理
│       │   ├── settings.html       # 用户设置
│       │   └── change-password.html # 修改密码
│       └── assets/                 # 静态资源（CSS、JS 工具类）
├── 004.数据库脚本(DBA)/
│   ├── 001_schema_ddl.sql          # 表结构（user / bill / category / budget）
│   ├── 002_seed_data.sql           # 16 个预设分类数据
│   └── 003_er_model.sql            # ER 模型文档
└── README.md                       # 项目说明
```

## 快速开始

### 环境要求

- Node.js >= 22
- MySQL >= 8.0

### 启动步骤

```bash
cd 003.前端代码/backend

# 安装依赖
npm install

# 初始化数据库（按顺序执行 SQL 脚本）
# mysql -u root -p < ../004.数据库脚本(DBA)/001_schema_ddl.sql
# mysql -u root -p < ../004.数据库脚本(DBA)/002_seed_data.sql

# 编辑 config/db.js 配置数据库连接信息，然后启动
npm start
```

服务启动后访问 `http://localhost:3000`，前端页面在 `/index.html`，API 在 `/api/`。

### 数据库表结构

| 表名 | 说明 |
|------|------|
| `user` | 用户表（手机号、密码哈希、昵称、头像、偏好设置） |
| `bill` | 账单表（类型：EXPENSE/INCOME、金额、分类、备注、时间） |
| `category` | 分类表（图标、名称，16 个默认分类） |
| `budget` | 预算表（月度总预算 + 分项预算，支持软删除） |

完整 DDL 见 `004.数据库脚本(DBA)/001_schema_ddl.sql`。

## API 接口速查

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 手机号注册 |
| POST | `/api/auth/login` | 登录，返回 JWT Token |
| GET | `/api/bills` | 账单列表，支持 `month`、`page`、`pageSize`、`category_id`、`type` 筛选 |
| GET | `/api/bills/stats/month` | 月度统计 + 日趋势 + 分类排行 |
| GET | `/api/budgets/dashboard` | 预算进度仪表盘 |
| GET | `/api/categories?type=expense` | 分类列表 |
| GET/PUT/DELETE | `/api/bills/:id` | 账单详情/修改/删除 |
| GET/PUT | `/api/user/profile` | 获取/修改用户信息 |
| PUT | `/api/user/password` | 修改密码（需旧密码） |
| POST | `/api/user/upload-avatar` | 上传头像 |

受保护接口需在请求头携带 `Authorization: Bearer <token>`。

## 测试

项目内置 187 个测试用例，覆盖所有功能模块：

```bash
cd 003.前端代码
bash test_full_187.sh
```

覆盖范围：认证模块（15）、账单 CRUD（20）、账单边界（18）、预算 CRUD（14）、预算仪表盘（10）、分类（13）、用户（15）、统计（12）、安全测试（SQL 注入、XSS、JWT 伪造 — 20）、UI（19 项跳过，需浏览器环境）。
