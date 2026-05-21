# 每日财务管家 — 前端原型项目

> 轻量级、零门槛的个人财务管理 Web 原型，聚焦 Mobile-First 体验。

---

## 项目概述

| 维度 | 说明 |
|------|------|
| **产品定位** | 轻量级、零门槛、以"每日"为节奏的个人财务管理工具 |
| **核心理念** | 每天 30 秒，看清你的钱 |
| **当前版本** | V1.0（Web 网页端，适配 375px–414px 手机屏幕） |
| **目标用户** | 职场新人、年轻家庭、极简记账需求者 |
| **设计系统** | Zenith Finance — Financial Clarity 主题 |

---

## 目录结构

```
finance/
├── index.html                      # 入口页（自动跳转至登录页）
├── assets/
│   ├── css/
│   │   └── base.css                # 共享样式（图标字体、全局样式、工具类）
│   └── js/
│       └── nav.js                  # 底部导航栏渲染器（IIFE 模块）
├── pages/                          # 原型页面（9 个）
│   ├── login.html                  # 登录页
│   ├── home.html                   # 首页（概览 + 账单列表）
│   ├── statistics.html             # 统计页（饼图 + 折线图 + 排行）
│   ├── record.html                 # 快速记账页
│   ├── bill-detail.html            # 账单详情页
│   ├── category-manage.html        # 类别管理页
│   ├── budget.html                 # 预算设置页
│   ├── settings.html               # 设置页（我的）
│   └── change-password.html        # 修改密码页
├── _originals/                     # Stitch AI 原始生成文件（备份）
│   ├── _1/code.html                # 原设置页
│   ├── _2/code.html                # 原统计页
│   ├── _3/code.html                # 原登录页
│   ├── _4/code.html                # 原账单详情页
│   ├── _5/code.html                # 原预算设置页
│   ├── _6/code.html                # 原类别管理页
│   ├── _7/code.html                # 原快速记账页
│   └── _8/code.html                # 原首页
└── zenith_finance/
    └── DESIGN.md                   # Zenith Finance 设计系统文档
```

---

## 页面跳转关系

```
┌──────────┐
│  login   │───────────────────────────────┐
│  登录页   │ 登录/注册后跳转                │
└──────────┘                               │
     │                                     ▼
     │ 登录/注册                       ┌───────────────┐
     ▼                             ┌───│  bill-detail  │
┌──────────┐     ┌──────────────┐ │   │  账单详情      │
│  home    │←────│  statistics  │ │   └───────┬───────┘
│  首页     │←────│  统计页       │ │         │ 修改/删除后
└────┬─────┘     └──────────────┘ │         ▼
     │                            │   ┌───────────────┐
     │ 点击 FAB ┌──────────────┐  │   │  record       │
     │ 记账按钮  │  category-   │  │   │  快速记账      │
     ▼ ───────→│  manage       │  │   └───────────────┘
┌──────────┐  │  类别管理      │──┘
│ settings │──┘                │
│ 设置页    │                   │
│     │     │   ┌───────────────┤
│     └─────┴──►│  budget       │
│               │  预算设置      │
│     ┌─────────┘               │
│     ▼                         │
│  change-password              │
│  修改密码                      │
└───────────────────────────────┘
```

**导航入口**：

| 页面 | 入口 | 描述 |
|------|------|------|
| `index.html` → `pages/login.html` | 自动重定向 | 应用统一入口 |
| `login.html` → `home.html` | 登录/注册按钮 | 登录成功跳转首页 |
| `home.html` → `record.html` | FAB（底部中央 + 按钮） | 快速记账 |
| `home.html` → `bill-detail.html` | 概览卡片点击 | 查看账单详情 |
| `home.html` → `budget.html` | 推广卡片"立即了解" | 引导设置预算 |
| `statistics.html` ↔ `home.html` | 底部导航切换 | 统计/首页互跳 |
| `bill-detail.html` → `home.html` | 返回按钮 / 删除确认后 | 返回首页 |
| `bill-detail.html` → `record.html` | 修改账单按钮 | 编辑模式下重新记账 |
| `settings.html` → `category-manage.html` | 账单管理 → 类别管理 | 分类设置 |
| `settings.html` → `budget.html` | 账单管理 → 预算设置 | 预算管理 |
| `settings.html` → `change-password.html` | 系统设置 → 账号与安全 | 修改密码 |
| `category-manage.html` → `settings.html` | 返回按钮 | 返回设置 |
| `budget.html` → `settings.html` | 返回按钮 | 返回设置 |
| `change-password.html` → `settings.html` | 确认修改按钮 / 返回 | 返回设置 |
| 所有页面 → 其他页 | 底部 Tab Bar | 首页 / 统计 / 我的（3 Tab + FAB） |

---

## 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| **框架** | 纯 HTML5 | 无框架依赖，单文件原型 |
| **样式** | Tailwind CSS (CDN) | 通过 `cdn.tailwindcss.com` 加载，含 forms + container-queries 插件 |
| **图标** | Material Symbols Outlined | Google Fonts CDN，支持 FILL 可变轴（filled/outlined 切换） |
| **字体** | Inter | Google Fonts CDN，600 字重覆盖 400–700 |
| **布局** | CSS Grid / Flexbox | 响应式 Mobile-First（375px 基准） |
| **暗黑模式** | Tailwind `dark:` 前缀 | 通过 `<html class="dark">` 切换 |
| **导航** | 原生 JS（IIFE 模块） | `nav.js` 渲染底部 Tab Bar，路由集中管理 |
| **适配** | `env(safe-area-inset-bottom)` | 适配 iPhone Safari 底部横条安全区 |

### 浏览器兼容

| 浏览器 | 最低版本 |
|--------|----------|
| iOS Safari | ≥ 15 |
| Android Chrome | ≥ 90 |
| 桌面 Chrome / Firefox | 最新稳定版 |

---

## 设计系统（Zenith Finance）

设计系统完整文档见 [`zenith_finance/DESIGN.md`](zenith_finance/DESIGN.md)，以下为关键要点：

### 色彩体系

| 角色 | Token | 色值 | 用途 |
|------|-------|------|------|
| **主色（Trust Blue）** | `primary` | `#004ac6` | 按钮、导航激活态、品牌 |
| **辅色（Growth Green）** | `secondary` | `#006c49` | 收入、正向趋势 |
| **危险（Expense Red）** | `danger-expense` | `#EF4444` | 支出、超支预警、删除 |
| **警告（Caution Orange）** | `warning-alert` | `#F59E0B` | 预算 80% 提醒 |
| **增长（Success Green）** | `success-growth` | `#10B981` | 预算剩余、正向变化 |

> 暗黑模式背景切换为 `#0F172A`（深蓝灰），非纯黑，保持深度感。

### 字体层级

| Token | 大小 / 字重 | 行高 | 典型用途 |
|-------|-------------|------|----------|
| `display-hero` | 36px / 700 | 44px | 月度总支出/总收入 |
| `display-hero-mobile` | 28px / 700 | 34px | 移动端标题金额 |
| `headline-md` | 20px / 600 | 28px | 页面标题、卡片标题 |
| `body-lg` | 18px / 400 | 26px | 按钮文字 |
| `body-md` | 16px / 400 | 24px | 正文 |
| `body-sm` | 14px / 400 | 20px | 辅助文字 |
| `label-caps` | 12px / 600 | 16px | 标签、日期分组 |
| `numeric-data` | 20px / 500 | 24px | 数字金额显示 |

### 间距规范

| Token | 值 | 用途 |
|-------|----|------|
| `base-unit` | 4px | 最小间距单位（4pt 栅格） |
| `gutter` | 12px | 网格列间距 |
| `container-margin` | 16px | 页面左右边距 |
| `card-padding` | 20px | 卡片内边距 |
| `touch-target-min` | 44px | 最小触控区域（移动端规范） |

### 布局规范

- **网格**：移动端 4 列（375px），桌面端 12 列
- **Rhythm**：8pt 线性刻度
- **Thumb Zone**：核心操作（FAB、Tab Bar）位于屏幕底部 25%
- **Elevation**：3 层表面层级（Base → Card → Navigation）
- **圆角**：卡片 0.5rem，按钮/图标 1rem，FAB 圆形

---

## 开发说明

### 快速启动

直接双击 `index.html` 在浏览器中打开即可预览（无需构建工具）。

```bash
# 或使用任意本地服务器（推荐）
cd 003.前端代码/finance
npx serve .          # Node.js serve
# 或 python -m http.server 8080
```

### 共享模块使用

**底部导航栏（`nav.js`）**：

```html
<!-- 1. 在 </body> 前添加占位符 -->
<nav>
    <div id="bottom-nav"></div>
</nav>

<!-- 2. 引入脚本 -->
<script src="../assets/js/nav.js"></script>

<!-- 3. 初始化，传入当前页面 key -->
<script>Nav.init('home');</script>
<!-- 可选值：'home' | 'statistics' | 'record' | 'settings' -->
```

> 注：`'record'` 不渲染到 Tab Bar，记账入口由首页 FAB 按钮提供。

**共享样式（`base.css`）**：

```html
<link href="../assets/css/base.css" rel="stylesheet">
```

包含：Material Symbols 图标字体配置、body 全局样式、input 聚焦效果、`pb-safe` 安全区适配、`no-scrollbar` 隐藏滚动条。

### 页面模板约定

每个页面遵循统一结构：

```
TopAppBar（品牌 + 功能按钮）
    ↓
<main> 主内容区
    ├── 区块 1（标题/数据）
    ├── 区块 2（列表/表单）
    └── 区块 N
    ↓
FAB（可选，首页记账按钮）
    ↓
BottomNavBar（底部导航，由 nav.js 渲染）
```

### 新增页面规范

1. 页面文件放置于 `pages/` 目录，使用语义文件名（如 `profile.html`）
2. 页面 `<head>` 中引用共享资源：
   ```html
   <link href="../assets/css/base.css" rel="stylesheet">
   ```
3. 页面 `</body>` 前引用导航脚本：
   ```html
   <script src="../assets/js/nav.js"></script>
   <script>Nav.init('新页面key');</script>
   ```
4. 在 `nav.js` 的 `PAGES` 和 `TABS` 中添加新路由映射
5. 相关页面中更新跳转链接
6. 在本 README 的**页面跳转关系**表格中更新说明

### 暗黑模式切换

在 `<html>` 标签添加 `class="dark"` 即可启用暗黑模式：

```javascript
// 切换逻辑示例
document.documentElement.classList.toggle('dark');
```

> 目前 `settings.html` 中已包含暗黑模式开关 UI（待对接 JS 逻辑）。

---

## PRD 参考

完整产品需求文档见 `001.产品PRD(产品经理)/财务APP_PRD.md`。

### V1.0 交付范围

| 模块 | 功能 | 优先级 | 对应原型页 |
|------|------|--------|-----------|
| **消费记录** | 快速记账 + 账单列表 + 编辑/删除 | P0 | `home.html`, `record.html`, `bill-detail.html` |
| **分类体系** | 预设分类 + 自定义 + 二级分类 + 归档 | P0 | `category-manage.html` |
| **基础统计** | 月度概览 + 饼图 + 折线图 + 排行 | P0 | `statistics.html` |
| **预算管理** | 总预算 + 分项预算 + 进度条 + 超支预警 | P0 | `budget.html` |

### 配套基础设施

| 功能 | 对应原型页 |
|------|-----------|
| 用户登录 | `login.html` |
| 个人设置 | `settings.html` |
| 修改密码 | `change-password.html` |
| 暗黑模式 | `settings.html`（开关 UI 已就绪） |
| Web Notification | 预留接口（V1.0 暂不触发） |

### 不在 V1.0 范围

- ❌ 语音记账
- ❌ 周期性账单
- ❌ 共享账本
- ❌ 数据导出（CSV/Excel）
- ❌ 原生移动 App（iOS/Android）
- ❌ 微信小程序
- ❌ 离线记账能力

### 版本迭代规划

| 版本 | 平台 | 核心功能 | 预估周期 |
|------|------|----------|----------|
| **V1.0** | Web | 消费记录 + 分类 + 统计 + 预算 | 当前 |
| V1.1 | Web | 预算管理 + 月度报告 | 4 周 |
| V1.2 | Web | 周期性账单 + 语音记账 | 4 周 |
| V2.0 | 原生 App + 小程序 | 全功能迁移 + 共享账本 + 数据导出 | 10 周 |

---

## 维护说明

- 所有文件均添加了详细注释，建议修改时保持注释风格一致
- HTML 注释格式：`<!-- === 区块名称 === -->` + 功能描述
- CSS 注释格式：`/* 区块描述 — 说明 */`
- JS 注释格式：JSDoc 风格，标注参数和返回值
- 修改页面跳转关系后，务必同步更新本 README 的**页面跳转关系**表格
- 原始 Stitch AI 生成文件保留在 `_originals/` 目录，请勿直接修改
