# API 契约

本目录是项目唯一的 **API 真相源**。

两套后端（Node 在 [003.前端代码/backend/](../003.前端代码/backend/)，Java 在 [005.后端工程师 (java工程师)/expense-tracker-server/](../005.后端工程师%20(java工程师)/expense-tracker-server/)）共用同一份契约，避免在两处独立演进，导致密码规则、登录锁定窗口、分页上限等业务规则两边各写一份、不一致甚至冲突。

## 目录

| 文件 | 作用 |
|---|---|
| [openapi.yaml](openapi.yaml) | OpenAPI 3.0 规范 —— 端点、参数、响应 schema、共享枚举/常量。**改任何 API 必须先改这里**。 |
| [contract-test.sh](contract-test.sh) | 跨后端契约测试。同一组用例同时打 Node 和 Java 两个端口，对比 HTTP 状态码和响应中的关键字段，把差异落到 `DIFFS.md`。 |
| [DIFFS.md](DIFFS.md) | 已知差异清单。记录哪边偏离了契约、影响、修复方向。每次扫描后更新。 |

## 权威基准

**当前 OpenAPI 以 Java 后端的实现为权威**（账号锁定、随机短信验证码、Redis 可插拔等安全特性更完整）。Node 后端有 12 项与契约不一致，详见 [DIFFS.md](DIFFS.md)。

Node 端的"额外端点"（手机绑定/解绑、头像上传）已纳入 OpenAPI 作为**目标状态**，标注 `x-implemented-by: [node]`，提示 Java 后续补齐。

## 维护规约

### 改 API 的标准流程

1. **先改 [openapi.yaml](openapi.yaml)**（加端点 / 改字段 / 调常量）
2. 提交 yaml 改动，让 reviewer 先在契约层面对齐
3. 实现两端（或单端，但需在 [DIFFS.md](DIFFS.md) 标注另一端待跟进）
4. 跑 `./contract-test.sh` 验证
5. 同一 PR 同时更新 yaml + 两端代码 + DIFFS.md

不允许只改 controller 不改 yaml。yaml 不通过 review，代码 PR 不合。

### 跑契约测试

需要先把两个后端都启起来：

```bash
# Terminal 1 — Node 后端，默认 3000
cd "003.前端代码/backend"
npm start

# Terminal 2 — Java 后端，默认 8080
cd "005.后端工程师 (java工程师)/expense-tracker-server"
mvn spring-boot:run

# Terminal 3 — 跑测
cd contracts
./contract-test.sh
```

只启一端也能跑：脚本会标记另一端"未启动"，但仍会生成已启动那一端的实际响应快照。

环境变量覆盖端口：
```bash
NODE_BASE=http://localhost:3000 JAVA_BASE=http://localhost:8080 ./contract-test.sh
```

### 脚本依赖

`contract-test.sh` 只依赖系统工具，**不需要装任何 npm 包**：

| 工具 | 用途 | 安装 |
|---|---|---|
| `bash 4+` | 脚本本体 | macOS / Linux 自带；Windows 用 Git Bash 或 WSL |
| `curl` | 调 HTTP | 几乎所有系统预装 |
| `jq` | 解析响应、做字段断言 | 见下方 |

**安装 `jq`**：
```bash
# macOS
brew install jq

# Ubuntu / Debian
sudo apt-get install -y jq

# Windows (winget)
winget install jqlang.jq

# Windows (portable，临时用)
curl -sL -o /tmp/jq.exe https://github.com/jqlang/jq/releases/download/jq-1.7.1/jq-win64.exe
# 然后把 /tmp/jq.exe 放到 PATH 任一目录下
```

> 注意：winget 在 Windows 上安装后**新开 shell**才生效；旧 shell 仍会报"jq: command not found"。

### 业务常量「单一来源」

下表的所有数值由 [openapi.yaml](openapi.yaml) 的 `components.x-constants` 段统一声明，两端代码必须从环境变量或常量类读取，**不要再硬编码**。

| 常量 | 值 | 用于 |
|---|---|---|
| `password.minLength` / `maxLength` | 6 / 20 | 注册、改密、重置 |
| `password.pattern` | 含大小写+数字+特殊字符 | 同上 |
| `phone.pattern` | `^\d{8,15}$` | 注册、登录、绑定、短信 |
| `login.maxAttempts` | 5 | 登录锁定 |
| `login.lockWindowMs` | 900000（15 min） | 同上 |
| `sms.codeLength` / `ttlSeconds` / `sendIntervalMs` | 6 / 300 / 60000 | 短信验证 |
| `bills.maxPageSize` | 100 | 列表分页 |
| `bills.remarkMaxLength` | 200 | 创建/更新账单 |
| `bills.tagsPerBillMax` | 10 | 同上 |
| `bills.exportMaxRows` | 50000 | 导出 |
| `tag.nameMaxLength` | 16 | 标签 |
| `category.nameMaxLength` | 20 | 分类（**Java 当前未校验**）|
| `nickname.maxLength` | 50 | 资料更新（**Constants 定义 32 与此冲突**）|
| `avatar.maxBytes` / `mimeWhitelist` | 2 MiB / image/jpeg,png,gif,webp | 头像上传（**Java 未实现**）|
| `jwt.expirationMs` | 604800000（7 天） | 登录态 |

## 后续阶段

- **阶段 1（本次）**：交付 yaml + 脚本 + DIFFS 报告 ✅
- **阶段 2**：按 [DIFFS.md](DIFFS.md) 逐项决策、修 Node 后端拉齐契约
- **阶段 3**：修两端共有 bug（预算 dashboard 日期窗口）+ 清理未使用 Constants
