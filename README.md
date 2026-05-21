# 每日财务管家

> 一款个人财务管理应用，支持账单记录、预算管理和可视化统计 —— 前后端分离，双后端支持

## 双后端架构

本项目同时维护两套后端，提供完全一致的 API 接口，前端通过一行注释切换：

| 后端 | 端口 | 目录 | 技术栈 |
|------|------|------|--------|
| Node.js | 3000 | `003.前端代码/backend/` | Node.js 22 + Express 4 |
| Java (Spring Boot) | 8080 | `005.后端工程师 (java工程师)/expense-tracker-server/` | Spring Boot 3.2 + MyBatis-Plus 3.5 |

切换方式见前端 [assets/js/auth.js](003.前端代码/finance/assets/js/auth.js)：

```javascript
// Node.js 后端: var API_BASE = 'http://localhost:3000';
var API_BASE = 'http://localhost:8080';
```

两套后端功能对等，响应格式一致，前端切换后端无需修改业务逻辑。

## 功能特性

- **用户认证** — 手机号注册/登录，JWT Token 认证（7天有效期），bcryptjs 密码加密；密码需包含大小写字母、数字和特殊字符，6-20位
- **安全机制** — 登录/注册频率限制（10次/15分钟），密码复杂度校验，错误提示防信息泄露
- **账单管理** — 记录收入/支出，支持分类、金额、备注；无限滚动分页加载（每页 10 条）
- **分类管理** — 内置 16 个预设收支分类（带图标），支持归档空分类
- **预算管理** — 月度总预算 + 分项预算，进度仪表盘实时追踪
- **统计分析** — 月度收支概览、日趋势折线图、分类饼图
- **用户中心** — 头像上传、昵称修改、币种切换（CNY/USD/EUR）、主题切换（浅色/深色）、手机号绑定

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | 原生 JavaScript、Tailwind CSS（CDN）、Material Symbols 图标 |
| 后端（Node.js） | Node.js 22 + Express 4 + MySQL 8.0 |
| 后端（Java） | Spring Boot 3.2 + MyBatis-Plus 3.5 + MySQL 8.0 |
| 数据库 | MySQL 8.0 |
| 认证 | JWT + bcryptjs（Node.js）/ spring-security-crypto（Java） |
| 安全 | express-rate-limit / Spring Boot 拦截器，频率限制 + 密码复杂度校验 |
| 文件上传 | Multer（头像，2MB 图片限制） |

## 项目结构

```
.
├── 001.产品PRD(产品经理)/          # 产品需求文档
├── 002.产品UI原型(美术设计)/        # UI原型与设计资源
├── 003.前端代码/
│   ├── backend/                    # Express 后端服务（Node.js，端口 3000）
│   │   ├── .env.example            # 环境变量模板
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
│       ├── assets/
│       │   └── js/auth.js          # API_BASE 变量，注释切换后端（:3000 / :8080）
│       └── pages/                  # 页面目录
│           ├── home.html           # 首页 — 月度概览 + 无限滚动账单列表
│           ├── record.html         # 记账页面
│           ├── statistics.html     # 统计页 — 收支图表
│           ├── budget.html         # 预算管理
│           ├── bill-detail.html    # 账单详情
│           ├── category-manage.html # 分类管理
│           ├── settings.html       # 用户设置
│           └── change-password.html # 修改密码
├── 004.数据库脚本(DBA)/
│   ├── 001_schema_ddl.sql          # 表结构（user / bill / category / budget）
│   ├── 002_seed_data.sql           # 16 个预设分类数据
│   └── 003_er_model.sql            # ER 模型文档
├── 005.后端工程师 (java工程师)/
│   ├── SpringBoot项目开发规范.md     # Java 后端开发规范
│   └── expense-tracker-server/       # Spring Boot 后端服务（Java，端口 8080）
│       ├── pom.xml                   # Maven 依赖配置
│       ├── .env.example              # 环境变量模板
│       ├── .env                      # 环境变量（数据库连接、JWT 密钥）
│       ├── .gitignore                # Git 忽略规则
│       ├── README.md                 # Java 后端文档
│       ├── test_java_full.sh         # 全量测试脚本（91 用例）
│       └── src/main/
│           ├── java/com/expense/
│           │   ├── controller/       # 控制器（Auth / User / Bill / Category / Budget）
│           │   ├── service/          # 业务逻辑接口 + 实现
│           │   ├── mapper/           # MyBatis-Plus 数据访问层
│           │   ├── entity/           # 实体类
│           │   ├── dto/              # 请求参数
│           │   ├── vo/               # 响应视图
│           │   ├── common/           # 统一响应、全局异常处理、枚举
│           │   ├── config/           # Web、MyBatis-Plus、JWT 配置
│           │   ├── interceptor/      # JWT 鉴权拦截器
│           │   ├── util/             # JWT 工具、BCrypt 工具
│           │   └── ExpenseTrackerApplication.java  # 启动类
│           └── resources/
│               ├── application.yml   # 主配置
│               ├── application-dev.yml # 开发环境
│               ├── application-prod.yml # 生产环境
│               └── META-INF/
│                   └── spring.factories # .env 自动加载注册
└── README.md                       # 项目说明
```

## 快速开始

### 环境要求

- **Node.js 后端**：Node.js >= 22
- **Java 后端**：JDK 21 + Maven 3.8+
- **数据库**：MySQL >= 8.0

### 1. 初始化数据库

```bash
# 按顺序执行 SQL 脚本
mysql -u root -p < 004.数据库脚本\(DBA\)/001_schema_ddl.sql
mysql -u root -p < 004.数据库脚本\(DBA\)/002_seed_data.sql
```

### 2. 启动后端服务

**选择其中一个后端启动**（默认使用 Node.js 端）：

```bash
# --- Node.js 后端（端口 3000）---
cd 003.前端代码/backend
cp .env.example .env
# 编辑 .env 填写数据库连接
npm install
npm start
# 访问 http://localhost:3000

# --- Java 后端（端口 8080）---
cd 005.后端工程师\ \(java工程师\)/expense-tracker-server
cp .env.example .env
# 编辑 .env 填写数据库连接和 JWT 密钥
mvn spring-boot:run
# 访问 http://localhost:8080/api/health
```

### 3. 切换后端

修改前端 `003.前端代码/finance/assets/js/auth.js` 的 `API_BASE` 变量即可：

```javascript
// Node.js 后端（端口 3000）：
// var API_BASE = 'http://localhost:3000';
// Java 后端（端口 8080）：
var API_BASE = 'http://localhost:8080';
```

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
| POST | `/api/auth/register` | 手机号注册（密码需大小写字母+数字+特殊字符） |
| POST | `/api/auth/login` | 登录，返回 JWT Token |
| POST | `/api/auth/send-sms-code` | 发送短信验证码（忘记密码用） |
| POST | `/api/auth/reset-password` | 重置密码（需手机 + 短信验证码） |
| GET | `/api/bills` | 账单列表，支持 `month`、`keyword`（备注关键词模糊搜索）、`page`、`pageSize`、`category_id`、`type` 筛选 |
| GET | `/api/bills/export` | 导出账单（CSV/Excel），支持 `format=csv|xlsx`、`month`、`start_date`、`end_date`、`type`、`keyword`、`category_id` 筛选，单次最多 50,000 行 |
| GET | `/api/bills/stats/month` | 月度统计 + 日趋势 + 分类排行 |
| GET | `/api/budgets/dashboard` | 预算进度仪表盘 |
| GET | `/api/categories?type=expense` | 分类列表 |
| GET/PUT/DELETE | `/api/bills/:id` | 账单详情/修改/删除 |
| GET/PUT | `/api/user/profile` | 获取/修改用户信息 |
| PUT | `/api/user/password` | 修改密码（需旧密码） |
| POST | `/api/user/upload-avatar` | 上传头像 |

受保护接口需在请求头携带 `Authorization: Bearer <token>`。

## 测试

项目内置两套测试脚本，分别对应两个后端：

```bash
# Node.js 后端测试（187 用例）
cd 003.前端代码
bash test_full_187.sh

# Java 后端测试（91 用例，功能等价）
cd 005.后端工程师\ \(java工程师\)/expense-tracker-server
bash test_java_full.sh
```

覆盖范围：认证模块（27）、账单 CRUD（20）、账单边界（18）、预算 CRUD（14）、预算仪表盘（10）、分类（13）、用户（15）、统计（12）、安全测试（SQL 注入、XSS、JWT 伪造 — 20）、UI（19 项跳过，需浏览器环境）。
