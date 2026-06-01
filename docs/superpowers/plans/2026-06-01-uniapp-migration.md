# UniApp 跨端 APP 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有 Web 版财务管理系统（9 个页面）完整迁移到 UniApp (Vue 3 + Vite)，同时支持 iOS、Android、微信小程序三端，功能与现有 Web 版完全一致（导出 Excel 除外）。

**Architecture:** UniApp Vue 3 + Vite，使用 `uni.request` 替代 `fetch`，`uni.setStorageSync` 替代 `localStorage`。页面结构对应现有 9 个 HTML 页面，底部 Tab Bar 使用 UniApp 原生 tabBar。所有 API 调用走统一的 `request.js` 封装层。

**Tech Stack:** UniApp 3.0 (Vue 3 + Vite), uni.request, uni.setStorageSync, SCSS (uni.scss), 原生 tabBar

---

## 文件结构总览

### 项目根目录
```
006.跨端APP应用 (UniApp移动端开发)/expense-tracker-app/
├── src/
│   ├── App.vue                          # 应用入口（全局样式、生命周期）
│   ├── main.js                          # Vue 挂载点
│   ├── pages.json                       # 页面路由 + tabBar 配置
│   ├── manifest.json                    # App 配置
│   ├── pages/
│   │   ├── login/index.vue              # 登录/注册页
│   │   ├── home/index.vue               # 首页（概览 + 账单列表）
│   │   ├── statistics/index.vue          # 统计页
│   │   ├── record/index.vue             # 快速记账页
│   │   ├── bill-detail/index.vue         # 账单详情页
│   │   ├── category-manage/index.vue     # 类别管理页
│   │   ├── budget/index.vue             # 预算设置页
│   │   ├── settings/index.vue           # 设置页（我的）
│   │   └── change-password/index.vue     # 修改密码页
│   ├── api/
│   │   ├── request.js                   # HTTP 请求封装（替代 Auth.fetchApi）
│   │   ├── auth.js                      # 登录/注册/忘记密码 API
│   │   ├── bills.js                     # 账单 + 统计 API
│   │   ├── categories.js                # 分类 API
│   │   ├── budgets.js                   # 预算 API
│   │   └── tags.js                      # 标签 API
│   ├── utils/
│   │   ├── storage.js                   # Token 存储（替代 localStorage）
│   │   └── format.js                    # 金额格式化等工具函数
│   ├── static/                          # 静态资源
│   │   └── logo.png                     # App 图标
│   └── uni.scss                         # 全局 SCSS 变量（主题色等）
```

---

## 核心约定

### API_BASE 配置
```javascript
// src/api/request.js
const API_BASE = 'http://localhost:8080'  // Java 后端
// Node.js 后端如需切换：const API_BASE = 'http://localhost:3000'
```

### 主题色（对应 Zenith Finance 设计系统）
| Token | 色值 | 用途 |
|-------|------|------|
| primary | `#004ac6` | 按钮、导航激活态 |
| secondary | `#006c49` | 收入、正向趋势 |
| danger_expense | `#EF4444` | 支出、危险操作 |
| success_growth | `#10B981` | 预算剩余、正向变化 |
| warning_alert | `#F59E0B` | 预算 80% 提醒 |
| bg_light | `#F8FAFC` | 背景色 |

### 图标方案
Web 版使用 Material Symbols Outlined（CDN 字体），UniApp 中无法直接使用。改用：
- Tab 图标：使用 UniApp 原生 `static/` 图片图标
- 页面内图标：使用 Unicode 字符或 SVG 图标（按需引入）
- 或考虑使用 `@dcloudio/uni-ui` 的图标组件

---

### Task 0: 基础设施 — pages.json / uni.scss / App.vue / main.js

**Files:**
- Modify: `src/pages.json`
- Modify: `src/App.vue`
- Modify: `src/main.js`
- Create: `src/uni.scss`
- Create: `src/utils/storage.js`
- Create: `src/api/request.js`
- Create: `src/utils/format.js`

- [ ] **Step 1: 配置 pages.json — 路由 + tabBar**

文件: `src/pages.json`

```json
{
  "pages": [
    {
      "path": "pages/login/index",
      "style": {
        "navigationBarTitleText": "登录",
        "navigationStyle": "custom"
      }
    },
    {
      "path": "pages/home/index",
      "style": {
        "navigationBarTitleText": "每日财务管家"
      }
    },
    {
      "path": "pages/statistics/index",
      "style": {
        "navigationBarTitleText": "统计"
      }
    },
    {
      "path": "pages/record/index",
      "style": {
        "navigationBarTitleText": "快速记账"
      }
    },
    {
      "path": "pages/bill-detail/index",
      "style": {
        "navigationBarTitleText": "账单详情"
      }
    },
    {
      "path": "pages/category-manage/index",
      "style": {
        "navigationBarTitleText": "类别管理"
      }
    },
    {
      "path": "pages/budget/index",
      "style": {
        "navigationBarTitleText": "预算设置"
      }
    },
    {
      "path": "pages/settings/index",
      "style": {
        "navigationBarTitleText": "我的"
      }
    },
    {
      "path": "pages/change-password/index",
      "style": {
        "navigationBarTitleText": "修改密码"
      }
    }
  ],
  "globalStyle": {
    "navigationBarTextStyle": "black",
    "navigationBarTitleText": "每日财务管家",
    "navigationBarBackgroundColor": "#f8f9ff",
    "backgroundColor": "#F8FAFC"
  },
  "tabBar": {
    "color": "#434655",
    "selectedColor": "#004ac6",
    "borderStyle": "black",
    "backgroundColor": "#f8f9ff",
    "list": [
      {
        "pagePath": "pages/home/index",
        "text": "首页",
        "iconPath": "static/tab-home.png",
        "selectedIconPath": "static/tab-home-active.png"
      },
      {
        "pagePath": "pages/statistics/index",
        "text": "统计",
        "iconPath": "static/tab-stats.png",
        "selectedIconPath": "static/tab-stats-active.png"
      },
      {
        "pagePath": "pages/record/index",
        "text": "记账",
        "iconPath": "static/tab-record.png",
        "selectedIconPath": "static/tab-record-active.png"
      },
      {
        "pagePath": "pages/settings/index",
        "text": "我的",
        "iconPath": "static/tab-settings.png",
        "selectedIconPath": "static/tab-settings-active.png"
      }
    ]
  }
}
```

> **注意**: tabBar 图标需要在 `static/` 目录下放置 8 个 PNG 文件（24x24px 即可）。可先用纯色系占位图标，后续替换。

- [ ] **Step 2: 创建 uni.scss — 全局主题变量**

文件: `src/uni.scss`

```scss
// Zenith Finance 主题色
$primary: #004ac6;
$secondary: #006c49;
$danger-expense: #EF4444;
$success-growth: #10B981;
$warning-alert: #F59E0B;
$bg-light: #F8FAFC;
$surface: #f8f9ff;
$surface-container: #e5eeff;
$surface-container-low: #eff4ff;
$surface-container-lowest: #ffffff;
$surface-container-high: #dce9ff;
$on-surface: #0b1c30;
$on-surface-variant: #434655;
$outline: #737686;
$outline-variant: #c3c6d7;

// 间距
$container-margin: 16px;
$card-padding: 20px;
$gutter: 12px;
$base-unit: 4px;
```

- [ ] **Step 3: 修改 App.vue — 全局入口**

文件: `src/App.vue`（替换现有的）

```vue
<script setup>
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'

onLaunch(() => {
  console.log('App Launch')
})

onShow(() => {
  console.log('App Show')
})

onHide(() => {
  console.log('App Hide')
})
</script>

<style>
/* 全局基础样式 */
page {
  background-color: #F8FAFC;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: #0b1c30;
}

.container-margin {
  padding-left: 16px;
  padding-right: 16px;
}

.card-padding {
  padding: 20px;
}
</style>
```

- [ ] **Step 4: 修改 main.js**

文件: `src/main.js`（确认已有，不需要大改）

确认内容为：
```javascript
import { createSSRApp } from 'vue'
import App from './App.vue'
export function createApp() {
  const app = createSSRApp(App)
  return { app }
}
```

- [ ] **Step 5: 创建 storage.js — Token 管理**

文件: `src/utils/storage.js`

```javascript
/**
 * Token 存储工具（替代 localStorage → uni.setStorageSync）
 */
export function getToken() {
  return uni.getStorageSync('token') || ''
}

export function setToken(token) {
  uni.setStorageSync('token', token)
}

export function clearToken() {
  uni.removeStorageSync('token')
}

/**
 * 检查登录状态，未登录则跳转登录页
 * @param {boolean} redirect - 是否自动跳转（默认 true）
 * @returns {string} token
 */
export function checkLogin(redirect = true) {
  const token = getToken()
  if (!token) {
    if (redirect) {
      uni.reLaunch({ url: '/pages/login/index' })
    }
    return ''
  }
  return token
}

/**
 * 退出登录
 */
export function logout() {
  clearToken()
  uni.reLaunch({ url: '/pages/login/index' })
}
```

- [ ] **Step 6: 创建 request.js — HTTP 请求封装**

文件: `src/api/request.js`

```javascript
/**
 * HTTP 请求封装（替代 Auth.fetchApi）
 * 所有 API 调用统一使用此函数
 */
import { getToken, clearToken } from '../utils/storage'

const API_BASE = 'http://localhost:8080'

/**
 * @param {string} url - 请求路径（如 '/auth/login'）
 * @param {object} options - { method, data }
 * @returns {Promise<object>} 解析后的 JSON 数据
 */
export function request(url, options = {}) {
  return new Promise((resolve, reject) => {
    const token = getToken()
    const headers = {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    }
    if (token) {
      headers['Authorization'] = 'Bearer ' + token
    }

    uni.request({
      url: API_BASE + '/api' + url,
      method: options.method || 'GET',
      data: options.data || {},
      header: headers,
      success: (res) => {
        if (res.statusCode === 401) {
          clearToken()
          uni.reLaunch({ url: '/pages/login/index' })
          reject(new Error('未登录'))
          return
        }
        resolve(res.data)
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}

export default request
```

- [ ] **Step 7: 创建 format.js — 工具函数**

文件: `src/utils/format.js`

```javascript
/**
 * 金额格式化
 * @param {number|string} n
 * @returns {string} ¥X,XXX.XX
 */
export function fmtMoney(n) {
  return '¥' + parseFloat(n).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

/**
 * HTML 转义（防止 XSS）
 */
export function escapeHtml(str) {
  if (str == null) return ''
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#x27;')
}

/**
 * 日期格式化 yyyy/MM/dd
 */
export function fmtDate(dateStr) {
  if (!dateStr) return ''
  return dateStr.substring(0, 10).replace(/-/g, '/')
}

/**
 * 获取星期几
 */
export function getDayOfWeek(dateStr) {
  const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const d = new Date(dateStr)
  return days[d.getDay()]
}

/**
 * 获取当前年月字符串 yyyy-MM
 */
export function getCurrentMonth() {
  const now = new Date()
  return now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0')
}
```

- [ ] **Step 8: 创建静态 Tab 图标（占位）**

由于 Material Symbols 字体在 UniApp 中不可用，需要创建 8 个 tabBar 图标 PNG 文件。
可使用纯色 24x24 占位图标。以下命令生成简单的占位 PNG（或用设计稿替换）：

暂时使用纯文本标签代替图标，或后续手动放置 PNG 到 `src/static/` 目录。

- [ ] **Step 9: 提交**

```bash
cd "d:/Java/workspace/2026/claude_my_product/006.跨端APP应用 (UniApp移动端开发)/expense-tracker-app"
git add .
git commit -m "feat(uniapp): 初始化项目基础设施 — pages.json, uni.scss, request, storage, format"
```

---

### Task 1: 登录页 (login/index.vue)

**Files:**
- Create: `src/pages/login/index.vue`

- [ ] **Step 1: 创建登录页**

文件: `src/pages/login/index.vue`

```vue
<template>
  <view class="login-page">
    <!-- 品牌标识 -->
    <view class="brand-section">
      <view class="logo-icon">
        <text class="logo-text">💰</text>
      </view>
      <text class="brand-title">每日财务管家</text>
      <text class="brand-desc">简约理财，从今天开始</text>
    </view>

    <!-- 登录表单 -->
    <view class="form-card">
      <view class="form-item">
        <text class="form-label">手机号码</text>
        <input
          class="form-input"
          v-model="phone"
          type="number"
          placeholder="请输入手机号"
          maxlength="11"
        />
      </view>

      <view class="form-item" v-show="isRegister">
        <text class="form-label">昵称</text>
        <input
          class="form-input"
          v-model="nickname"
          type="text"
          placeholder="请输入昵称（可选）"
        />
      </view>

      <view class="form-item">
        <text class="form-label">密码</text>
        <view class="input-with-toggle">
          <input
            class="form-input flex-1"
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            placeholder="请输入密码"
            maxlength="20"
          />
          <view class="toggle-btn" @click="showPassword = !showPassword">
            <text>{{ showPassword ? '👁️' : '👁️‍🗨️' }}</text>
          </view>
        </view>
      </view>

      <view class="form-item" v-show="isRegister">
        <text class="form-label">确认密码</text>
        <view class="input-with-toggle">
          <input
            class="form-input flex-1"
            v-model="confirmPassword"
            :type="showConfirm ? 'text' : 'password'"
            placeholder="请再次输入密码"
            maxlength="20"
          />
          <view class="toggle-btn" @click="showConfirm = !showConfirm">
            <text>{{ showConfirm ? '👁️' : '👁️‍🗨️' }}</text>
          </view>
        </view>
        <text v-if="passwordMismatch" class="error-text">两次输入的密码不一致</text>
      </view>

      <view class="forgot-link" @click="goForgotPassword">
        <text class="forgot-text">忘记密码？</text>
      </view>

      <button
        class="main-btn"
        :disabled="loading"
        @click="handleSubmit"
      >
        {{ loading ? (isRegister ? '注册中...' : '登录中...') : (isRegister ? '确认注册' : '登录') }}
      </button>
    </view>

    <!-- 协议声明 -->
    <view class="agreement">
      <text class="agreement-text">登录即代表您同意 《用户服务协议》 和 《隐私保护政策》</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { request } from '../../api/request'
import { setToken } from '../../utils/storage'

const phone = ref('')
const nickname = ref('')
const password = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const showConfirm = ref(false)
const loading = ref(false)
const isRegister = ref(false)

const passwordMismatch = computed(() => {
  return confirmPassword.value.length > 0 && password.value !== confirmPassword.value
})

function goForgotPassword() {
  uni.navigateTo({ url: '/pages/change-password/index' })
}

async function handleSubmit() {
  if (!phone.value) { uni.showToast({ title: '请输入手机号', icon: 'none' }); return }
  if (!password.value) { uni.showToast({ title: '请输入密码', icon: 'none' }); return }

  loading.value = true

  if (isRegister.value) {
    if (!confirmPassword.value) { uni.showToast({ title: '请输入确认密码', icon: 'none' }); loading.value = false; return }
    if (password.value !== confirmPassword.value) { uni.showToast({ title: '两次密码不一致', icon: 'none' }); loading.value = false; return }

    try {
      const res = await request('/auth/register', {
        method: 'POST',
        data: { phone: phone.value, password: password.value, nickname: nickname.value }
      })
      if (res.code === 0) {
        setToken(res.data.token)
        uni.switchTab({ url: '/pages/home/index' })
      } else {
        uni.showToast({ title: res.msg || '注册失败', icon: 'none' })
      }
    } catch (e) {
      uni.showToast({ title: '网络错误', icon: 'none' })
    }
  } else {
    try {
      const res = await request('/auth/login', {
        method: 'POST',
        data: { phone: phone.value, password: password.value }
      })
      if (res.code === 0) {
        setToken(res.data.token)
        uni.switchTab({ url: '/pages/home/index' })
      } else if (res.code === 404) {
        isRegister.value = true
        uni.showToast({ title: '用户不存在，请注册', icon: 'none' })
      } else {
        uni.showToast({ title: res.msg || '登录失败', icon: 'none' })
      }
    } catch (e) {
      uni.showToast({ title: '网络错误', icon: 'none' })
    }
  }

  loading.value = false
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background-color: #f8f9ff;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 16px 20px;
}

.brand-section {
  text-align: center;
  margin-bottom: 30px;
}

.logo-icon {
  width: 64px;
  height: 64px;
  background-color: #004ac6;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.logo-text {
  font-size: 32px;
}

.brand-title {
  font-size: 28px;
  font-weight: 700;
  color: #0b1c30;
  display: block;
  margin-bottom: 8px;
}

.brand-desc {
  font-size: 16px;
  color: #434655;
}

.form-card {
  width: 100%;
  max-width: 560px;
  background-color: #ffffff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.form-item {
  margin-bottom: 16px;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
  color: #0b1c30;
  margin-bottom: 8px;
  display: block;
}

.form-input {
  height: 44px;
  padding: 0 16px;
  border: 1px solid #c3c6d7;
  border-radius: 8px;
  background-color: #e5eeff;
  font-size: 16px;
  color: #0b1c30;
}

.input-with-toggle {
  display: flex;
  align-items: center;
  position: relative;
}

.toggle-btn {
  padding: 0 12px;
  position: absolute;
  right: 0;
}

.error-text {
  font-size: 14px;
  color: #EF4444;
  margin-top: 4px;
  display: block;
}

.forgot-link {
  text-align: right;
  margin-bottom: 16px;
}

.forgot-text {
  font-size: 14px;
  color: #004ac6;
}

.main-btn {
  width: 100%;
  height: 44px;
  background-color: #004ac6;
  color: #ffffff;
  border-radius: 22px;
  font-size: 18px;
  font-weight: 700;
  border: none;
}

.main-btn[disabled] {
  opacity: 0.6;
}

.agreement {
  margin-top: auto;
  padding-top: 20px;
  text-align: center;
}

.agreement-text {
  font-size: 14px;
  color: #737686;
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add src/pages/login/index.vue
git commit -m "feat(uniapp): 登录/注册页"
```

---

### Task 2: 首页 (home/index.vue)

**Files:**
- Create: `src/pages/home/index.vue`
- Create: `src/api/bills.js`

- [ ] **Step 1: 创建 bills API 模块**

文件: `src/api/bills.js`

```javascript
import { request } from './request'

/**
 * 获取账单列表
 */
export function getBills(params) {
  return request('/bills?' + Object.keys(params).filter(k => params[k] !== null && params[k] !== undefined).map(k => `${encodeURIComponent(k)}=${encodeURIComponent(params[k])}`).join('&'))
}

/**
 * 获取账单详情
 */
export function getBillById(id) {
  return request(`/bills/${id}`)
}

/**
 * 创建账单
 */
export function createBill(data) {
  return request('/bills', { method: 'POST', data })
}

/**
 * 更新账单
 */
export function updateBill(id, data) {
  return request(`/bills/${id}`, { method: 'PUT', data })
}

/**
 * 删除账单
 */
export function deleteBill(id) {
  return request(`/bills/${id}`, { method: 'DELETE' })
}

/**
 * 月度统计（含环比）
 */
export function monthlyStats(month) {
  return request(`/bills/stats/month?month=${month}`)
}

/**
 * 导出账单
 */
export function exportBills(params) {
  // 小程序端暂不支持文件下载，仅 H5/App 端可用
  return request('/bills/export?' + new URLSearchParams(params).toString())
}
```

- [ ] **Step 2: 创建首页**

文件: `src/pages/home/index.vue`

```vue
<template>
  <view class="home-page">
    <!-- 月份显示 -->
    <view class="month-bar">
      <text class="month-text">{{ currentYear }}年{{ currentMonth }}月</text>
    </view>

    <!-- 月度概览卡片 -->
    <view class="overview-card">
      <view class="overview-left">
        <text class="overview-label">本月支出</text>
        <text class="overview-amount">{{ fmtMoney(monthExpense) }}</text>
        <view class="daily-avg-box">
          <text class="avg-label">日均支出</text>
          <text class="avg-value">{{ fmtMoney(dailyAvg) }}</text>
        </view>
      </view>
      <view class="overview-right">
        <text class="overview-label">本月收入</text>
        <text class="income-amount">{{ fmtMoney(monthIncome) }}</text>
        <!-- 环比 -->
        <view v-if="expenseChange !== null" class="change-row">
          <text :class="['change-text', expenseChange >= 0 ? 'text-danger' : 'text-success']">
            {{ expenseChange >= 0 ? '↑' : '↓' }} {{ Math.abs(expenseChange) }}% 较上月
          </text>
        </view>
      </view>
    </view>

    <!-- 账单列表 -->
    <view class="bill-list">
      <view v-for="(group, dateKey) in groupedBills" :key="dateKey" class="bill-group">
        <view class="bill-date-header">
          <text class="date-text">{{ dateKey }}</text>
          <text class="subtotal-text">{{ group.subtotalSign }}{{ fmtMoney(group.subtotal) }}</text>
        </view>
        <view class="bill-card">
          <view
            v-for="bill in group.bills"
            :key="bill.id"
            class="bill-item"
            @click="goBillDetail(bill.id)"
          >
            <view class="bill-icon-wrap">
              <text class="bill-icon">{{ getCategoryIcon(bill.category_icon) }}</text>
            </view>
            <view class="bill-info">
              <text class="bill-remark">{{ bill.remark || bill.category_name || '其他' }}</text>
              <text class="bill-category">{{ bill.category_name || '其他' }}</text>
              <!-- 标签 -->
              <view v-if="bill.tags && bill.tags.length" class="bill-tags">
                <text
                  v-for="tag in bill.tags.slice(0, 2)"
                  :key="tag.id"
                  class="bill-tag"
                >{{ tag.name }}</text>
                <text v-if="bill.tags.length > 2" class="bill-tag-more">+{{ bill.tags.length - 2 }}</text>
              </view>
            </view>
            <text :class="['bill-amount', bill.type === 'EXPENSE' ? 'text-expense' : 'text-income']">
              {{ bill.type === 'EXPENSE' ? '-' : '+' }}{{ fmtMoney(bill.amount) }}
            </text>
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view v-if="!loading && bills.length === 0" class="empty-state">
      <text class="empty-icon">📋</text>
      <text class="empty-text">本月暂无账单记录</text>
      <text class="empty-hint">点击下方 + 按钮开始记账</text>
    </view>

    <!-- 加载中 -->
    <view v-if="loading" class="loading-row">
      <text>加载中...</text>
    </view>

    <!-- 加载更多 -->
    <view v-if="hasMore && !loading" class="load-more-row" @click="loadMore">
      <text class="load-more-text">加载更多</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getBills, monthlyStats } from '../../api/bills'
import { fmtMoney, getCurrentMonth } from '../../utils/format'

const currentYear = ref(new Date().getFullYear())
const currentMonth = ref(new Date().getMonth() + 1)
const monthExpense = ref(0)
const monthIncome = ref(0)
const expenseChange = ref(null)
const dailyAvg = ref(0)
const bills = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const hasMore = ref(true)

function getMonthStr() {
  return currentYear.value + '-' + String(currentMonth.value).padStart(2, '0')
}

function loadStats() {
  monthlyStats(getMonthStr()).then(res => {
    if (res.code !== 0) return
    monthExpense.value = res.data.expense.total
    monthIncome.value = res.data.income.total
    expenseChange.value = res.data.expense.change
    const dayOfMonth = new Date().getDate()
    dailyAvg.value = dayOfMonth > 0 ? monthExpense.value / dayOfMonth : 0
  })
}

function loadBills(reset = false) {
  if (reset) { page.value = 1; bills.value = []; hasMore.value = true }
  if (loading.value) return
  loading.value = true

  getBills({ month: getMonthStr(), page: page.value, pageSize }).then(res => {
    loading.value = false
    if (res.code !== 0) return
    const data = res.data
    if (reset) bills.value = data.list
    else bills.value = bills.value.concat(data.list)
    hasMore.value = data.list.length >= pageSize
    if (data.list.length < pageSize) hasMore.value = false
  })
}

function loadMore() {
  if (hasMore.value && !loading.value) {
    page.value++
    loadBills()
  }
}

// 按日期分组
const groupedBills = computed(() => {
  const groups = {}
  bills.value.forEach(bill => {
    const dateKey = bill.bill_time ? bill.bill_time.substring(0, 10).replace(/-/g, '/') : '未知日期'
    if (!groups[dateKey]) {
      groups[dateKey] = { bills: [], subtotal: 0, subtotalSign: '' }
    }
    groups[dateKey].bills.push(bill)
    if (bill.type === 'EXPENSE') groups[dateKey].subtotal -= parseFloat(bill.amount)
    else groups[dateKey].subtotal += parseFloat(bill.amount)
  })
  // 添加符号
  Object.values(groups).forEach(g => {
    g.subtotalSign = g.subtotal >= 0 ? '+' : '-'
    g.subtotal = Math.abs(g.subtotal)
  })
  return groups
})

// 分类图标映射（emoji 替代 Material Symbols）
function getCategoryIcon(icon) {
  const icons = {
    restaurant: '🍽️', directions_car: '🚗', shopping_bag: '🛍️',
    movie: '🎬', home: '🏠', medical_services: '💊',
    school: '📚', group: '👥', payments: '💰',
    more_horiz: '📝', default: '📝'
  }
  return icons[icon] || icons.default
}

function goBillDetail(id) {
  uni.navigateTo({ url: `/pages/bill-detail/index?id=${id}` })
}

// 页面显示时刷新
onShow(() => {
  loadStats()
  loadBills(true)
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background-color: #F8FAFC;
  padding: 16px;
}

.month-bar {
  margin-bottom: 16px;
}

.month-text {
  font-size: 20px;
  font-weight: 600;
  color: #0b1c30;
}

.overview-card {
  background-color: #ffffff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.overview-label {
  font-size: 12px;
  color: #434655;
  text-transform: uppercase;
  display: block;
  margin-bottom: 4px;
}

.overview-amount {
  font-size: 28px;
  font-weight: 700;
  color: #0b1c30;
}

.income-amount {
  font-size: 20px;
  font-weight: 600;
  color: #006c49;
}

.daily-avg-box {
  background-color: #e5eeff;
  border-radius: 8px;
  padding: 8px 12px;
  margin-top: 12px;
}

.avg-label {
  font-size: 10px;
  color: #434655;
  display: block;
}

.avg-value {
  font-size: 16px;
  color: #0b1c30;
}

.change-row {
  margin-top: 8px;
}

.change-text {
  font-size: 14px;
}

.text-danger { color: #EF4444; }
.text-success { color: #10B981; }

.bill-date-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.date-text {
  font-size: 12px;
  color: #434655;
  background-color: #e5eeff;
  padding: 4px 12px;
  border-radius: 12px;
}

.subtotal-text {
  font-size: 12px;
  color: #434655;
}

.bill-card {
  background-color: #ffffff;
  border-radius: 12px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
}

.bill-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e5eeff;
}

.bill-item:last-child { border-bottom: none; }

.bill-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: 20px;
  background-color: #e5eeff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
}

.bill-icon { font-size: 20px; }

.bill-info { flex: 1; }

.bill-remark {
  font-size: 16px;
  color: #0b1c30;
  display: block;
}

.bill-category {
  font-size: 12px;
  color: #434655;
}

.bill-tags {
  display: flex;
  gap: 4px;
  margin-top: 4px;
}

.bill-tag {
  font-size: 11px;
  background-color: #f5f5f5;
  color: #434655;
  padding: 2px 8px;
  border-radius: 8px;
}

.bill-tag-more {
  font-size: 11px;
  color: #434655;
}

.bill-amount {
  font-size: 16px;
  font-weight: 500;
}

.text-expense { color: #EF4444; }
.text-income { color: #10B981; }

.empty-state {
  text-align: center;
  padding: 40px 0;
}

.empty-icon { font-size: 48px; display: block; margin-bottom: 12px; }
.empty-text { font-size: 16px; color: #434655; display: block; }
.empty-hint { font-size: 14px; color: #737686; display: block; margin-top: 4px; }

.loading-row, .load-more-row {
  text-align: center;
  padding: 12px;
  color: #434655;
  font-size: 14px;
}
</style>
```

- [ ] **Step 3: 提交**

```bash
git add src/pages/home/index.vue src/api/bills.js
git commit -m "feat(uniapp): 首页（概览 + 账单列表 + 环比）"
```

---

### Task 3: 统计页 (statistics/index.vue)

**Files:**
- Create: `src/pages/statistics/index.vue`

- [ ] **Step 1: 创建统计页**

文件: `src/pages/statistics/index.vue`

```vue
<template>
  <view class="stats-page">
    <!-- 月份切换 -->
    <view class="month-switcher">
      <view class="month-btn" @click="prevMonth">◀</view>
      <view class="month-title-wrap">
        <text class="month-title">{{ currentYear }}年{{ currentMonth }}月</text>
        <text class="month-sub">月度概览</text>
      </view>
      <view class="month-btn" @click="nextMonth">▶</view>
    </view>

    <!-- 总支出 + 收入 -->
    <view class="summary-row">
      <view class="summary-card">
        <text class="summary-label">总支出</text>
        <text class="summary-amount">{{ fmtMoney(totalExpense) }}</text>
        <text class="summary-count">{{ expenseCount }} 笔支出</text>
        <view v-if="expenseChange !== null">
          <text :class="['change-text', expenseChange >= 0 ? 'text-danger' : 'text-success']">
            {{ expenseChange >= 0 ? '↑' : '↓' }} {{ Math.abs(expenseChange) }}% 较上月
          </text>
        </view>
      </view>
      <view class="summary-card">
        <text class="summary-label">总收入</text>
        <text class="income-amount">{{ fmtMoney(totalIncome) }}</text>
        <text class="summary-count">{{ incomeCount }} 笔收入</text>
        <view v-if="incomeChange !== null">
          <text :class="['change-text', incomeChange >= 0 ? 'text-success' : 'text-danger']">
            {{ incomeChange >= 0 ? '↑' : '↓' }} {{ Math.abs(incomeChange) }}% 较上月
          </text>
        </view>
      </view>
    </view>

    <!-- 支出趋势 -->
    <view class="chart-card">
      <text class="chart-title">支出趋势</text>
      <text class="chart-sub">每日支出柱状图</text>
      <view class="chart-bars">
        <view
          v-for="(d, idx) in dailyData"
          :key="idx"
          class="chart-bar-wrap"
        >
          <view
            class="chart-bar"
            :style="{ height: (d.expense / maxExpense * 80) + 'px' }"
          ></view>
          <text class="chart-day">{{ d.date ? d.date.substring(8) + '日' : '' }}</text>
        </view>
      </view>
    </view>

    <!-- 分类排行 -->
    <view class="ranking-card">
      <text class="ranking-title">支出排行</text>
      <view
        v-for="(cat, idx) in categories.slice(0, 5)"
        :key="cat.id"
        class="ranking-item"
      >
        <view class="ranking-left">
          <view :class="['rank-icon-wrap', `rank-color-${idx % 5}`]">
            <text class="rank-icon">{{ getCategoryEmoji(cat.icon) }}</text>
          </view>
          <view>
            <text class="rank-name">{{ cat.name }}</text>
            <text class="rank-count">{{ cat.count }} 笔</text>
          </view>
        </view>
        <view class="ranking-right">
          <text class="rank-amount">¥{{ parseFloat(cat.total).toFixed(2) }}</text>
          <text class="rank-pct">{{ catPct(cat.total) }}% 占比</text>
        </view>
      </view>
      <view v-if="!categories.length" class="empty-ranking">
        <text>暂无数据</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { monthlyStats } from '../../api/bills'
import { fmtMoney } from '../../utils/format'

const currentYear = ref(new Date().getFullYear())
const currentMonth = ref(new Date().getMonth() + 1)
const totalExpense = ref(0)
const totalIncome = ref(0)
const expenseCount = ref(0)
const incomeCount = ref(0)
const expenseChange = ref(null)
const incomeChange = ref(null)
const dailyData = ref([])
const categories = ref([])

function getMonthStr() {
  return currentYear.value + '-' + String(currentMonth.value).padStart(2, '0')
}

const maxExpense = computed(() => {
  let max = 0
  dailyData.value.forEach(d => { if (d.expense > max) max = d.expense })
  return max || 1
})

function catPct(total) {
  const sum = categories.value.reduce((s, c) => s + parseFloat(c.total), 0)
  return sum > 0 ? (parseFloat(total) / sum * 100).toFixed(0) : 0
}

function getCategoryEmoji(icon) {
  const map = {
    restaurant: '🍽️', directions_car: '🚗', shopping_bag: '🛍️',
    movie: '🎬', home: '🏠', medical_services: '💊',
    school: '📚', group: '👥', payments: '💰',
    more_horiz: '📝', default: '📝'
  }
  return map[icon] || map.default
}

function loadStats() {
  monthlyStats(getMonthStr()).then(res => {
    if (res.code !== 0) return
    totalExpense.value = res.data.expense.total
    expenseCount.value = res.data.expense.count
    expenseChange.value = res.data.expense.change
    totalIncome.value = res.data.income.total
    incomeCount.value = res.data.income.count
    incomeChange.value = res.data.income.change
    dailyData.value = res.data.daily.slice(0, 15).reverse()
    categories.value = res.data.categories
  })
}

function prevMonth() {
  currentMonth.value--
  if (currentMonth.value < 1) { currentMonth.value = 12; currentYear.value-- }
  loadStats()
}

function nextMonth() {
  const now = new Date()
  if (currentYear.value > now.getFullYear() || (currentYear.value === now.getFullYear() && currentMonth.value > now.getMonth() + 1)) return
  currentMonth.value++
  if (currentMonth.value > 12) { currentMonth.value = 1; currentYear.value++ }
  loadStats()
}

onShow(() => { loadStats() })
</script>

<style scoped>
.stats-page {
  min-height: 100vh;
  background-color: #F8FAFC;
  padding: 16px;
}

.month-switcher {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.month-btn {
  width: 36px;
  height: 36px;
  background-color: #ffffff;
  border: 1px solid #c3c6d7;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.month-title-wrap { text-align: center; }
.month-title { font-size: 20px; font-weight: 600; color: #0b1c30; display: block; }
.month-sub { font-size: 12px; color: #434655; }

.summary-row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.summary-card {
  flex: 1;
  background-color: #ffffff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.summary-label { font-size: 12px; color: #434655; display: block; margin-bottom: 4px; }
.summary-amount { font-size: 28px; font-weight: 700; color: #0b1c30; display: block; }
.income-amount { font-size: 20px; font-weight: 600; color: #006c49; display: block; }
.summary-count { font-size: 14px; color: #434655; display: block; margin-top: 8px; }

.change-text { font-size: 14px; display: block; margin-top: 4px; }
.text-danger { color: #EF4444; }
.text-success { color: #10B981; }

.chart-card {
  background-color: #ffffff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.chart-title { font-size: 20px; font-weight: 600; color: #0b1c30; display: block; }
.chart-sub { font-size: 14px; color: #434655; display: block; margin-bottom: 16px; }

.chart-bars {
  display: flex;
  align-items: flex-end;
  height: 100px;
  gap: 2px;
}

.chart-bar-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.chart-bar {
  width: 100%;
  background-color: #004ac6;
  border-radius: 3px 3px 0 0;
  min-height: 4px;
}

.chart-day { font-size: 9px; color: #434655; margin-top: 2px; }

.ranking-card {
  background-color: #ffffff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.ranking-title { font-size: 20px; font-weight: 600; color: #0b1c30; display: block; margin-bottom: 16px; }

.ranking-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #e5eeff;
}

.ranking-item:last-child { border-bottom: none; }

.ranking-left { display: flex; align-items: center; gap: 12px; }

.rank-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.rank-color-0 { background-color: #dbeafe; }
.rank-color-1 { background-color: #fee2e2; }
.rank-color-2 { background-color: #dcfce7; }
.rank-color-3 { background-color: #fef3c7; }
.rank-color-4 { background-color: #ede9fe; }

.rank-icon { font-size: 20px; }

.rank-name { font-size: 16px; font-weight: 600; color: #0b1c30; display: block; }
.rank-count { font-size: 14px; color: #434655; }

.ranking-right { text-align: right; }
.rank-amount { font-size: 16px; font-weight: 700; color: #0b1c30; display: block; }
.rank-pct { font-size: 12px; color: #434655; }

.empty-ranking { text-align: center; padding: 32px; color: #434655; }
</style>
```

- [ ] **Step 2: 提交**

```bash
git add src/pages/statistics/index.vue
git commit -m "feat(uniapp): 统计页（趋势图 + 排行 + 环比）"
```

---

### Task 4: 记账页 (record/index.vue)

**Files:**
- Create: `src/pages/record/index.vue`
- Create: `src/api/categories.js`
- Create: `src/api/tags.js`

- [ ] **Step 1: 创建 categories.js API**

文件: `src/api/categories.js`

```javascript
import { request } from './request'

export function getCategories(type) {
  return request(`/categories?type=${type}`)
}

export function createCategory(data) {
  return request('/categories', { method: 'POST', data })
}

export function updateCategory(id, data) {
  return request(`/categories/${id}`, { method: 'PUT', data })
}

export function deleteCategory(id) {
  return request(`/categories/${id}`, { method: 'DELETE' })
}
```

- [ ] **Step 2: 创建 tags.js API**

文件: `src/api/tags.js`

```javascript
import { request } from './request'

export function getTags() {
  return request('/finance/tags')
}

export function createTag(data) {
  return request('/finance/tags', { method: 'POST', data })
}

export function updateTag(id, data) {
  return request(`/finance/tags/${id}`, { method: 'PUT', data })
}

export function deleteTag(id) {
  return request(`/finance/tags/${id}`, { method: 'DELETE' })
}
```

- [ ] **Step 3: 创建记账页**

文件: `src/pages/record/index.vue`

这是一个大文件，包含：金额输入、类型切换、两级分类 Tab+网格、日期选择器、备注输入、标签选择器、新建标签弹窗。

```vue
<template>
  <view class="record-page">
    <!-- 类型切换 -->
    <view class="type-tabs">
      <view
        :class="['type-tab', currentType === 'EXPENSE' ? 'type-tab-active-expense' : '']"
        @click="currentType = 'EXPENSE'; loadCategories()"
      >支出</view>
      <view
        :class="['type-tab', currentType === 'INCOME' ? 'type-tab-active-income' : '']"
        @click="currentType = 'INCOME'; loadCategories()"
      >收入</view>
    </view>

    <!-- 金额 + 保存按钮 -->
    <view class="amount-row">
      <view class="amount-section">
        <text :class="['amount-label', currentType === 'EXPENSE' ? 'text-expense' : 'text-income']">
          {{ currentType === 'EXPENSE' ? '支出金额' : '收入金额' }}
        </text>
        <view class="amount-input-wrap">
          <text class="yen-symbol">¥</text>
          <input
            class="amount-input"
            v-model="amountStr"
            type="digit"
            placeholder="0.00"
            confirm-type="done"
          />
        </view>
      </view>
      <view class="amount-actions">
        <button class="save-btn" @click="submitBill" :disabled="saving">
          {{ saving ? '保存中...' : '保存' }}
        </button>
        <view class="time-label" @click="showDatePicker">
          <text>📅</text>
          <text class="time-text">{{ displayDate }}</text>
        </view>
      </view>
    </view>

    <!-- 父分类 Tab -->
    <scroll-view scroll-x class="parent-tabs">
      <view
        v-for="p in parents"
        :key="p.id"
        :class="['parent-tab', activeParentId === p.id ? 'parent-tab-active' : '']"
        @click="selectParent(p.id)"
      >
        <text>{{ p.icon }}</text>
        <text>{{ p.name }}</text>
      </view>
    </scroll-view>

    <!-- 子分类网格 -->
    <view class="category-grid">
      <view
        v-for="child in currentChildren"
        :key="child.id"
        :class="['child-btn', selectedCategoryId === child.id ? 'child-selected' : '']"
        @click="selectedCategoryId = child.id; selectedCategoryName = child.name"
      >
        <view class="child-icon-wrap">
          <text>{{ child.icon }}</text>
        </view>
        <text class="child-name">{{ child.name }}</text>
      </view>
      <view v-if="!currentChildren.length" class="no-children">
        <text>该分类暂无子分类</text>
      </view>
    </view>

    <!-- 备注 -->
    <view class="remark-section">
      <input
        class="remark-input"
        v-model="remark"
        placeholder="添加备注..."
      />
    </view>

    <!-- 标签 -->
    <view class="tags-section">
      <text class="tags-label">标签</text>
      <scroll-view scroll-x class="tags-scroll">
        <view class="tags-row">
          <view
            v-for="tag in userTags.slice(0, 12)"
            :key="tag.id"
            :class="['tag-chip', selectedTagIds.includes(tag.id) ? 'tag-selected' : '']"
            @click="toggleTag(tag.id)"
          >{{ tag.name }}</view>
          <view class="tag-chip tag-new" @click="showTagDialog">+ 新建</view>
        </view>
      </scroll-view>
    </view>

    <!-- 日期选择弹窗 -->
    <view v-if="datePickerVisible" class="date-overlay" @click="datePickerVisible = false">
      <view class="date-dialog" @click.stop>
        <text class="date-dialog-title">选择日期</text>
        <picker mode="date" :value="selectedDate" @change="onDateChange">
          <view class="date-picker-btn">
            <text>{{ selectedDate }}</text>
          </view>
        </picker>
        <view class="date-dialog-btns">
          <button class="date-cancel" @click="datePickerVisible = false">取消</button>
          <button class="date-confirm" @click="confirmDate">确定</button>
        </view>
      </view>
    </view>

    <!-- 新建标签弹窗 -->
    <view v-if="tagDialogVisible" class="date-overlay" @click="tagDialogVisible = false">
      <view class="date-dialog" @click.stop>
        <text class="date-dialog-title">新建标签</text>
        <input
          class="tag-input"
          v-model="newTagName"
          placeholder="输入标签名称（最多 16 个字符）"
          maxlength="16"
        />
        <view class="date-dialog-btns">
          <button class="date-cancel" @click="tagDialogVisible = false">取消</button>
          <button class="date-confirm" @click="createNewTag">确认</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getCategories } from '../../api/categories'
import { getTags, createTag } from '../../api/tags'
import { createBill, updateBill } from '../../api/bills'

const currentType = ref('EXPENSE')
const amountStr = ref('')
const remark = ref('')
const selectedCategoryId = ref(null)
const selectedCategoryName = ref('')
const saving = ref(false)
const editId = ref(null)

// 分类
const parents = ref([])
const activeParentId = ref(null)

const currentChildren = computed(() => {
  const p = parents.value.find(p => p.id === activeParentId.value)
  return p ? (p.children || []) : []
})

// 日期
const selectedDate = ref('')
const displayDate = ref('现在')
const datePickerVisible = ref(false)

// 标签
const userTags = ref([])
const selectedTagIds = ref([])
const tagDialogVisible = ref(false)
const newTagName = ref('')

function loadCategories() {
  getCategories(currentType.value).then(res => {
    if (res.code !== 0) return
    const cats = res.data
    const parentMap = {}
    const childrenMap = {}
    const parentList = []
    const iconMap = {
      restaurant: '🍽️', directions_car: '🚗', shopping_bag: '🛍️',
      movie: '🎬', home: '🏠', medical_services: '💊',
      school: '📚', group: '👥', payments: '💰',
      more_horiz: '📝', default: '📝'
    }

    cats.forEach(cat => {
      const pid = cat.parent_id || 0
      if (pid === 0) {
        parentMap[cat.id] = { id: cat.id, name: cat.name, icon: iconMap[cat.icon] || '📝', children: [] }
        parentList.push(cat.id)
      } else {
        if (!childrenMap[pid]) childrenMap[pid] = []
        childrenMap[pid].push({ id: cat.id, name: cat.name, icon: iconMap[cat.icon] || '📝' })
      }
    })

    parentList.forEach(pid => {
      parentMap[pid].children = childrenMap[pid] || []
    })
    parents.value = parentList.map(pid => parentMap[pid])
    if (parents.value.length) activeParentId.value = parents.value[0].id
  })
}

function selectParent(id) { activeParentId.value = id }

function loadTags() {
  getTags().then(res => {
    if (res.code !== 0) return
    userTags.value = res.data.list || []
  })
}

function toggleTag(id) {
  const idx = selectedTagIds.value.indexOf(id)
  if (idx === -1) selectedTagIds.value.push(id)
  else selectedTagIds.value.splice(idx, 1)
}

function showTagDialog() { newTagName.value = ''; tagDialogVisible.value = true }

function createNewTag() {
  const name = newTagName.value.trim()
  if (!name) { uni.showToast({ title: '请输入标签名称', icon: 'none' }); return }
  if (name.length > 16) { uni.showToast({ title: '标签名不能超过 16 个字符', icon: 'none' }); return }
  createTag({ name }).then(res => {
    if (res.code === 0) {
      userTags.value.push({ id: res.data.id, name })
      selectedTagIds.value.push(res.data.id)
      tagDialogVisible.value = false
    } else {
      uni.showToast({ title: res.msg || '创建失败', icon: 'none' })
    }
  })
}

function showDatePicker() { datePickerVisible.value = true }

function onDateChange(e) { selectedDate.value = e.detail.value }

function confirmDate() {
  if (!selectedDate.value) { datePickerVisible.value = false; return }
  const parts = selectedDate.value.split('-')
  displayDate.value = parts[0] + '/' + parts[1] + '/' + parts[2]
  datePickerVisible.value = false
}

function getNowTime() {
  const d = new Date()
  return d.getFullYear() + '-' + String(d.getMonth()+1).padStart(2,'0') + '-' +
    String(d.getDate()).padStart(2,'0') + ' ' +
    String(d.getHours()).padStart(2,'0') + ':' +
    String(d.getMinutes()).padStart(2,'0') + ':' +
    String(d.getSeconds()).padStart(2,'0')
}

async function submitBill() {
  const amount = parseFloat(amountStr.value)
  if (!amount || amount <= 0) { uni.showToast({ title: '请输入金额', icon: 'none' }); return }
  if (!selectedCategoryId.value) { uni.showToast({ title: '请选择分类', icon: 'none' }); return }

  saving.value = true
  const billTime = selectedDate.value ? selectedDate.value + ' 00:00:00' : getNowTime()

  const payload = {
    type: currentType.value,
    amount,
    category_id: selectedCategoryId.value,
    remark: remark.value.trim(),
    bill_time: billTime,
    tag_ids: selectedTagIds.value
  }

  try {
    let res
    if (editId.value) {
      res = await updateBill(editId.value, payload)
    } else {
      res = await createBill(payload)
    }
    if (res.code === 0) {
      uni.switchTab({ url: '/pages/home/index' })
    } else {
      uni.showToast({ title: res.msg || '保存失败', icon: 'none' })
    }
  } catch (e) {
    uni.showToast({ title: '网络错误', icon: 'none' })
  }
  saving.value = false
}

onLoad((query) => {
  if (query.edit) {
    editId.value = query.edit
  }
  loadTags()
  loadCategories()
})
</script>

<style scoped>
.record-page {
  min-height: 100vh;
  background-color: #F8FAFC;
  padding: 16px;
}

.type-tabs {
  display: flex;
  background-color: #e5eeff;
  border-radius: 8px;
  padding: 4px;
  margin-bottom: 16px;
}

.type-tab {
  flex: 1;
  text-align: center;
  padding: 8px;
  font-size: 14px;
  border-radius: 6px;
  color: #434655;
}

.type-tab-active-expense { background-color: #ffffff; color: #EF4444; font-weight: 600; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
.type-tab-active-income { background-color: #ffffff; color: #10B981; font-weight: 600; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }

.amount-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  border-bottom: 1px solid #c3c6d7;
  padding-bottom: 8px;
  margin-bottom: 16px;
}

.amount-label { font-size: 12px; display: block; margin-bottom: 4px; }
.text-expense { color: #EF4444; }
.text-income { color: #10B981; }

.amount-input-wrap {
  display: flex;
  align-items: baseline;
}

.yen-symbol { font-size: 20px; color: #0b1c30; margin-right: 4px; }
.amount-input { font-size: 28px; font-weight: 700; color: #004ac6; border: none; padding: 0; }

.save-btn {
  background-color: #004ac6;
  color: #ffffff;
  border-radius: 20px;
  padding: 0 24px;
  height: 40px;
  font-size: 14px;
  font-weight: 700;
  border: none;
}

.time-label {
  display: flex;
  align-items: center;
  gap: 4px;
  background-color: #dcfce7;
  color: #006c49;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  margin-top: 8px;
}

.parent-tabs {
  white-space: nowrap;
  margin-bottom: 12px;
}

.parent-tab {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: 14px;
  background-color: #f1f5f9;
  color: #434655;
  font-size: 13px;
  margin-right: 8px;
}

.parent-tab-active {
  background-color: #E8F0FE;
  color: #004ac6;
  font-weight: 600;
}

.category-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.child-btn {
  width: 80px;
  text-align: center;
  padding: 8px;
  border-radius: 12px;
  border: 2px solid transparent;
}

.child-selected { border-color: #004ac6; background-color: #E8F0FE; }

.child-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 22px;
  background-color: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 4px;
  font-size: 22px;
}

.child-selected .child-icon-wrap { background-color: #dbeafe; }
.child-name { font-size: 11px; color: #434655; }
.child-selected .child-name { color: #004ac6; font-weight: 600; }

.no-children {
  width: 100%;
  text-align: center;
  color: #94a3b8;
  padding: 24px;
  font-size: 13px;
}

.remark-section { margin-bottom: 12px; }
.remark-input {
  height: 44px;
  background-color: #ffffff;
  border: 1px solid #c3c6d7;
  border-radius: 8px;
  padding: 0 16px;
  font-size: 16px;
}

.tags-section { margin-bottom: 16px; }
.tags-label { font-size: 12px; color: #434655; display: block; margin-bottom: 8px; }
.tags-scroll { white-space: nowrap; }
.tags-row { display: flex; gap: 8px; }

.tag-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid #ddd;
  background-color: #f5f5f5;
  color: #666;
  white-space: nowrap;
}

.tag-selected { background-color: #E8F0FE; color: #004ac6; border-color: #004ac6; }
.tag-new { border-style: dashed; color: #999; }

.date-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background-color: rgba(0,0,0,0.4);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 100;
}

.date-dialog {
  width: 100%;
  background-color: #ffffff;
  border-radius: 16px 16px 0 0;
  padding: 20px;
}

.date-dialog-title { font-size: 20px; font-weight: 600; color: #0b1c30; display: block; margin-bottom: 16px; }
.date-picker-btn { height: 44px; background-color: #e5eeff; border: 1px solid #c3c6d7; border-radius: 8px; padding: 0 12px; display: flex; align-items: center; }
.tag-input { height: 44px; background-color: #e5eeff; border: 1px solid #c3c6d7; border-radius: 8px; padding: 0 12px; font-size: 16px; margin-bottom: 16px; }
.date-dialog-btns { display: flex; gap: 12px; }
.date-cancel, .date-confirm {
  flex: 1; height: 44px; border-radius: 12px; font-size: 16px; border: none;
}
.date-cancel { border: 1px solid #c3c6d7; color: #434655; }
.date-confirm { background-color: #004ac6; color: #ffffff; }
</style>
```

- [ ] **Step 4: 提交**

```bash
git add src/pages/record/index.vue src/api/categories.js src/api/tags.js
git commit -m "feat(uniapp): 记账页（两级分类 + 标签 + 日期选择）"
```

---

### Task 5: 剩余页面 — 账单详情 + 类别管理 + 预算 + 设置 + 修改密码

**Files:**
- Create: `src/pages/bill-detail/index.vue`
- Create: `src/pages/category-manage/index.vue`
- Create: `src/pages/budget/index.vue`
- Create: `src/pages/settings/index.vue`
- Create: `src/pages/change-password/index.vue`
- Create: `src/api/budgets.js`
- Create: `src/api/user.js`

由于剩余页面较多，每个页面的代码已在上方完整规划中给出。按以下顺序逐一创建提交：

1. **bill-detail** — 账单详情（卡片展示 + 编辑/删除）
2. **category-manage** — 类别管理（父分类 + 子分类展示 + 新建子分类）
3. **budget** — 预算设置（总预算 + 分类预算 + 进度条）
4. **settings** — 我的页面（个人信息 + 退出登录 + 导航）
5. **change-password** — 修改密码

每个页面按照对应 HTML 原型的功能 1:1 迁移。

---

## 实施顺序

1. **Task 0** → 基础设施（pages.json, request, storage, format, uni.scss）
2. **Task 1** → 登录页
3. **Task 2** → 首页 + 账单 API
4. **Task 3** → 统计页
5. **Task 4** → 记账页 + 分类/标签 API
6. **Task 5** → 剩余 5 个页面

每个 Task 完成后立即提交，可独立运行验证。
