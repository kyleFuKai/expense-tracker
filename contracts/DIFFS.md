# API 差异清单（Node vs Java）

> 本文件跟踪两端与 `openapi.yaml` 契约的差异。
> **修复方向以"以 Java 为权威"为前提**（见 [README.md](README.md#权威基准)）。
> 每项的标签：
>   - `[security]` 安全/正确性影响，先修
>   - `[compat]`   客户端兼容性
>   - `[enhance]`  增强项
>   - `[bug]`      一致性 bug，两端都有或单端逻辑问题

按优先级倒序（先修影响安全的）。

---

## 优先级 0：阻塞性/安全问题

### #1 `[security]` 登录失败账号锁定

- **状态**：✅ done (Node 端抽 `InMemoryLoginAttemptStore` + 接入 login 处理器，2026-06-08)
- **现状**：Java 在 `UserServiceImpl.login()` 用 `LoginAttemptStore`（memory / redis）做 5 次失败 → 锁定 15 分钟，命中后返回 429。Node 后端**没有此机制**。
- **契约位置**：[openapi.yaml POST /api/auth/login → 429](openapi.yaml)
- **修复方向**：Node 后端
  1. 加进程内 `loginAttemptStore`（参考 Java 的 `InMemoryLoginAttemptStore`，含 @Scheduled 清理）
  2. 在 `routes/auth.js` 的 login 处理器首部插入锁定检查
  3. 暴露 `MAX_LOGIN_ATTEMPTS=5`、`LOGIN_LOCK_WINDOW_MS=900000` 为可配置项
- **预计工时**：小，~30 行 Node 代码

### #2 `[security]` `POST /api/auth/send-sms-code` 真实可发送

- **状态**：✅ done (Node 端抽 `SmsProvider` 接口 + `InMemorySmsCodeStore`，接 `SMS_PROVIDER=log` 默认，2026-06-08)
- **现状**：Node 返回 501 "短信验证码功能暂未开放"。Java 真在发送（`SecureRandom` 6 位码 + Redis/memory TTL + 60s 频控）。
- **契约位置**：[openapi.yaml POST /api/auth/send-sms-code](openapi.yaml)
- **修复方向**：Node 后端
  1. 抽 `SmsProvider` 接口（mirror Java：LogSmsProvider / NoopSmsProvider）
  2. dev 默认 `log`、prod 默认 `noop`（与 Java 一致）
  3. 验证码用 `crypto.randomInt(0, 1_000_000)` 生成 6 位
  4. 复用 #1 的 `inMemoryStore` 提供 `smsCodeStore`
- **预计工时**：中，~150 行（接口 + 两实现 + route 改造）

### #3 `[security]` `POST /api/auth/reset-password` 真实可执行

- **状态**：✅ done (Node 端改实现：smsCodeStore.get + 密码强度校验 + UPDATE user，2026-06-08)
- **现状**：Node 返回 501。Java 真在使用 `SmsCodeStore.get` + `remove`。
- **契约位置**：[openapi.yaml POST /api/auth/reset-password](openapi.yaml)
- **修复方向**：Node 后端
  1. 在 `routes/auth.js` 改实现：`smsCodeStore.get(phone)` + 校验 + 调 `userService.updatePassword(phone, newPassword)`
  2. `UserServiceImpl` Node 端目前不暴露"按手机号改密码"，需新增
- **预计工时**：中，需先确认 #2 完成（依赖 smsCodeStore）

### #13 `[security]` Node 登录不防枚举

- **状态**：✅ done (Node send-sms-code 对未注册手机号返 200 但不写码，2026-06-08)
- **现状**：Node `/api/auth/login` 不区分"用户不存在"和"密码错"，统一 401（✅）。但 `POST /api/auth/send-sms-code` 在 Node 端**先校验手机号是否注册**（404 "手机号未注册"），**这给攻击者提供了一个枚举入口**。
- **修复方向**：Node `/api/auth/send-sms-code` 在"未注册"时**不**返回 404，而是统一 200 + 不发送，规避枚举。
- **预计工时**：极小，5 行

### #14 `[security]` 端点统一 `Authorization` 校验

- **状态**：🟡 partial (Node 已做 CORS 白名单 + CORS_ORIGINS；Java 待对齐，2026-06-08)
- **现状**：
  - Node 全局走 `authMiddleware`（基于 `Authorization: Bearer xxx`），白名单挂载在路由文件内
  - Java 走 `JwtInterceptor` 拦截 5 个前缀（`/api/bills/**` `/api/user/**` ...）
  - **不一致**：Java 的 CORS 白名单只放行 `localhost:3000`/`5500`，Node 用 `cors()` 无白名单
- **修复方向**：Java 端把 CORS 来源改为可配置（application.yml 读 `app.cors.allowed-origins`），prod 通过环境变量注入
- **预计工时**：极小，~10 行

---

## 优先级 1：客户端兼容性

### #4 `[compat]` `/api/auth/register` 响应差异

- **状态**：✅ done (Java 改 AuthController/UserService 返 LoginVO{token,userId,nickname}，2026-06-08)
- **现状**：Node 返回 `{token, userId}`（注册即登录）。Java 返回 `Long`（仅 userId，需再调 login）。
- **契约位置**：[openapi.yaml POST /api/auth/register](openapi.yaml)（标注 `x-implemented-by-node`）
- **修复方向**：二选一
  - **A. Java 改**：注册成功后签发 JWT 一并返回（推荐，体验好）
  - **B. 契约改**：注册仅返 `userId`，前端始终走 login
- **建议**：A。Java UserServiceImpl.register 末尾调 `jwtUtil.generateToken(...)` + 返回 `LoginVO`。~10 行
- **预计工时**：极小

### #5 `[compat]` Java 端缺失 3 个用户端点

- **现状**：Node 有，Java 无：
  - `PUT /api/user/bind-phone`
  - `PUT /api/user/unbind-phone`
  - `POST /api/user/upload-avatar`（含 multipart，2 MiB 上限）
- **契约位置**：[openapi.yaml](openapi.yaml) 标注 `x-implemented-by: [node]`
- **修复方向**：Java 后端
  1. `bind/unbind`：在 `UserController` 加 `@PutMapping`，`UserServiceImpl` 加绑定/解绑方法（无第三方短信校验，纯登录态即可）
  2. `upload-avatar`：用 `MultipartFile`，落 `uploads/avatars/`，加 `spring.servlet.multipart.max-file-size=2MB`
  3. CORS 要放 `Content-Type: multipart/form-data`
- **预计工时**：中，~120 行

### #6 `[compat]` `/api/bills` 不支持 `tag_id` 过滤

- **状态**：✅ done (Java 端 controller + service 已实现，DIFFS 描述不准确；本批加注释明示 tagId 安全性，2026-06-08)
- **现状**：Node `bills.js:62` 完整支持 `INNER JOIN bill_tag_rel` + 复合 WHERE。Java `BillController` 不接 `tagId`，`BillServiceImpl.list` 也不带 join。
- **契约位置**：[openapi.yaml GET /api/bills → tag_id parameter](openapi.yaml)
- **修复方向**：Java
  1. `BillController.list` 加 `@RequestParam Long tagId`
  2. `BillServiceImpl.list` 用 `LEFT JOIN bill_tag_rel` + `WHERE btr.tag_id = ?` 拼接
- **预计工时**：小，~30 行

### #7 `[compat]` `/api/bills/{id}` 详情字段差异

- **状态**：✅ done (Node bills.js:147 用 `b.*` 通配，is_recurring/updated_at 本就包含，2026-06-08)
- **现状**：Node 返 `category_name, category_icon`；Java 返 `category_name, category_icon, ...is_recurring, updated_at`（更全）。
- **契约位置**：[openapi.yaml BillDetail](openapi.yaml)
- **修复方向**：Node 端 `bills.js:178` 把 join 的 `category_name, category_icon` 加上 `is_recurring, updated_at` 即可
- **预计工时**：极小，~5 行

### #8 `[compat]` `/api/bills/stats/month` 响应结构

- **状态**：✅ done (Java BillStatsVO 改嵌入式 + 删顶层 expenseChange/incomeChange，2026-06-08)
- **现状**：
  - Node：`{expense: {total, count, change}, income: {total, count, change}}`
  - Java：`{expense: {total, count}, income: {total, count}, expenseChange, incomeChange}`
- **契约位置**：[openapi.yaml MonthStats](openapi.yaml)（采用 Node 嵌入式）
- **修复方向**：Java
  1. `BillStatsVO` 改造：`expense`/`income` 加 `change` 字段
  2. `BillServiceImpl` 把 `calculateChange` 移到循环里赋值
  3. 删 `expenseChange`/`incomeChange` 顶层
- **预计工时**：小，~20 行 + 前端兼容（已用 Node 的话不动）

### #11 `[compat]` `/api/budgets` POST 响应差异

- **现状**：Node 返 `{data: {id}}`；Java 返 `data: Long`。
- **契约位置**：[openapi.yaml POST /api/budgets](openapi.yaml)（目标统一 `{id}`）
- **修复方向**：Java `BudgetController.set` 把 `data` 包成 `Map.of("id", id)`
- **预计工时**：极小，~3 行

### #12 `[compat]` `/finance/tags` GET 响应差异

- **现状**：Node 返 `{data: {list: [...]}}`；Java 返 `data: [...]`。
- **契约位置**：[openapi.yaml GET /finance/tags](openapi.yaml)（目标统一 `{list}`）
- **修复方向**：Java `BillTagController.list` 把 `data` 包成 `Map.of("list", ...)`
- **预计工时**：极小，~3 行

---

## 优先级 2：bug & cleanup

### #9 `[bug]` 预算 dashboard 月末日 29-31 漏算

- **现状**：两端都用 `month+'-28'` 作为右端点。`2026-02-28` 之后到月末（28/29/30/31）生效的预算被漏掉。
- **修复方向**：用"下月 1 日"或"本月最后一天"：
  ```js
  const lastDay = new Date(year, month, 0).getDate(); // month=3 → 2 月的最后一天
  const end = `${year}-${month}-${String(lastDay).padStart(2, '0')}`;
  ```
- **预计工时**：极小，~5 行 × 2 端

### #10 `[bug]` Java `Constants` 死代码

- **现状**：`Constants.java` 定义了一堆与实际校验不匹配的常量：
  - `MAX_NICKNAME_LENGTH=32`（实际用 50）
  - `MAX_CATEGORY_NAME_LENGTH=20`（CategoryService 根本不校验）
  - `DEFAULT_PAGE_SIZE=10`（BillController 用 50）
  - `DEFAULT_NICKNAME_PREFIX="用户"`（register 用手机号脱敏）
  - `MAX_REMARK_LENGTH=200`（BillServiceImpl 硬编码 200）
- **修复方向**：
  1. 删 `Constants.java` 中未实际使用的项
  2. 把"实际在用"的硬编码值改回读 `Constants`
  3. 在 [openapi.yaml](openapi.yaml) `x-constants` 段已声明所有数值，Java 端用 `@Value` 注入
- **预计工时**：小，~30 行 + review 是否还要保留这些常量

### #15 `[bug]` `Category name` 长度无校验

- **现状**：契约要求 ≤ 20。两端都**不**校验（Java 的 `Constants.MAX_CATEGORY_NAME_LENGTH=20` 是死代码，Node 在 `routes/categories.js` POST/PUT 里也没校验）。
- **修复方向**：
  1. Java：在 `CategoryServiceImpl.create/update` 加 `if (name.length() > 20) throw new BusinessException(400, "分类名过长")`
  2. Node：在 `routes/categories.js` POST/PUT 加同样校验
- **预计工时**：极小，~10 行 × 2 端

---

## 修复顺序建议

| 顺序 | 项目 | 理由 |
|---|---|---|
| 1 | #1, #2, #3, #13, #14 | 安全问题，先收敛 |
| 2 | #4 | 改 Java 一行，提升体验（注册即登录） |
| 3 | #6, #7, #8 | 字段/结构对齐，前端不用写两套适配 |
| 4 | #5 | Java 补 3 个端点（不算小但独立） |
| 5 | #11, #15, #9 | 简单 cleanup |
| 6 | #10 | 收尾，删死代码 |

## 同步契约

每修一项：
1. 把状态从 "open" 改为 "done"，附带 commit SHA
2. 如果响应/参数变了，先改 [openapi.yaml](openapi.yaml)，再改代码
3. 跑 `./contract-test.sh` 验证两端对该端点都返回一致
4. 把改动 commit 到 `contracts/DIFFS.md` 同一文件以便审计
