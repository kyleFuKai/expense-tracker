# Release Notes — 2026 W23 双后端契约化 + 安全加固

> 时间窗：2026-06-03 ~ 2026-06-10
> 范围：建立 OpenAPI 契约 → 修复 15 项两端不一致 → 拉齐 Node 后端缺失的安全特性
> 远端：github.com/kyleFuKai/expense-tracker · gitee.com/xingzhewk/daily-finance

## 总览

| 指标 | 数值 |
|---|---:|
| 提交数 | 8（不含 1 次撤销） |
| 文件改动 | 60+ |
| 净增代码 | +3673 / -245 |
| 单元测试 | 73/73 Java + 26 Node 全过 |
| DIFFS 关闭 | 15/15 |

## 阶段 1：建立 OpenAPI 契约（commit 4e6069a）

之前两套后端（Node `003.前端代码/backend/` + Java `005.后端工程师/expense-tracker-server/`）独立演进，密码规则、登录锁定窗口、分页上限等业务规则两边各写一份，开始出现冲突。

新增 [contracts/](../../contracts/) 目录，**项目唯一 API 真相源**：

- [openapi.yaml](../../contracts/openapi.yaml)：21 unique path、16 schema，含 `x-constants` 集中声明 15 个业务常量（密码 6-20、手机号 8-15、登录锁 5 次/15 分、短信 6 位/5 分/60s、JWT 7 天、分页 100 上限、备注 200、标签名 16、分类名 20、头像 2MiB 等）
- [contract-test.sh](../../contracts/contract-test.sh)：bash + curl + jq，同时打 Node:3000 / Java:8080，对每个端点做：HTTP 状态码对比 → jq 点检关键字段 → 生成 markdown 报告 + JSON 快照
- [DIFFS.md](../../contracts/DIFFS.md)：15 项 Node vs Java 差异分 3 级（安全 / 兼容 / bug）
- [README.md](../../contracts/README.md)：维护规约——**改 API 必须先改 yaml**

## 阶段 2：Node 后端安全加固（commit 7af9d84）

Node 后端的 4 个 `/api/auth/*` 端点之前是占位 501、登录无锁定、CORS 全开。对齐 Java 已有的安全特性：

- **DIFFS #1** 登录账号锁定：抽 `store/loginAttemptStore.js`，5 次失败 → 15 分钟锁定（429 + 剩余秒数）；不区分"用户不存在"和"密码错"
- **DIFFS #2** send-sms-code 真发送：抽 `service/smsProvider.js`（`LogSmsProvider` / `NoopSmsProvider`）+ `store/smsCodeStore.js` 频控；`crypto.randomInt` 6 位码；送达失败回滚
- **DIFFS #3** reset-password 真执行：smsCodeStore.get/remove + 密码强度校验 + bcrypt + UPDATE
- **DIFFS #13** send-sms-code 防枚举：未注册手机号也返 200 但不写码
- **DIFFS #14** CORS 收紧：`cors()` → 白名单 + `CORS_ORIGINS` 环境变量

单元测试 14/14（store + smsCodeStore）。

## 阶段 3：Java 后端集群可用 + SMS 抽象（commit 12bea91）

把登录失败计数 / 短信验证码从 `ConcurrentHashMap` 字段抽到 Store 接口，**集群部署不再被绕过**：

- `service/store/LoginAttemptStore` + `SmsCodeStore` 接口
- `InMemory*` 实现（dev 默认）+ `@Scheduled` 60s 兜底清理（修内存泄漏）
- `Redis*` 实现（prod 默认）+ `INCR/EXPIRE` / `SETNX` 原子操作
- `pom.xml`: + spring-boot-starter-data-redis
- `service/sms/SmsProvider`：`LogSmsProvider`（dev WARN 写日志）/ `NoopSmsProvider`（prod 拒绝下发 503）
- **UserServiceImpl** 删除硬编码 `TEST_SMS_CODE = "666666"` → `SecureRandom` 6 位
- application-dev.yml: `SESSION_STORE=memory, SMS_PROVIDER=log`
- application-prod.yml: `SESSION_STORE=redis, SMS_PROVIDER=noop` + Lett 连接池配置

单元测试 16/16（2 个 SmsProvider + 7 个 LoginAttemptStore + 7 个 SmsCodeStore）。

## 阶段 4：Node SQL 拼接重构（commit 9a5b3cf）

`bills.js` 之前用 `'user_id = ?'.replace('user_id = ?', 'b.user_id = ?')` 这种**只替换第一个匹配的字符串改写**拼 SQL，配合 `params.slice(1)` 隐式约定，新增任何字段都可能错位。

抽 [utils/billQuery.js](../../003.前端代码/backend/utils/billQuery.js)：

- `buildBillWhere(filters, {alias, only})` —— 每个 filter 独立声明 `{a}.` 占位符 + when + value，无运行时字符串编辑
- `appendCondition({where, params}, sql, params)` —— 安全追加额外条件
- 3 处调用（`/api/bills` / `/api/bills/export` / `/api/bills/stats/month`）统一改造

单元测试 12/12，含回归测试模拟旧 `.replace` 失手的场景。

## 阶段 5：Java 客户端兼容（commit 5509240）

- **DIFFS #4** `/api/auth/register` 改返 `LoginVO{token, userId, nickname}`，注册即登录（前端不用再调 /login）
- **DIFFS #6** `/api/bills` tagId 过滤已实现，加注释说明 SQL 拼接安全
- **DIFFS #7** Node `/api/bills/{id}` 用 `b.*` 通配已含 is_recurring/updated_at，无需改
- **DIFFS #8** `BillStatsVO` 改嵌入式：`StatsItem` 加 `change` 字段；删顶层 `expenseChange/incomeChange`

## 阶段 6：Java 补 3 个用户端点（commit 303fd8d）

Node 独有的 3 个端点 Java 补齐：

- `PUT /api/user/bind-phone` —— 手机号正则 + 被占检查 (409)
- `PUT /api/user/unbind-phone` —— phone 置空、country_code 重置 +86
- `POST /api/user/upload-avatar` —— `MultipartFile`，2 MiB 上限 + MIME 白名单
  - 文件名 `user_<uid>_<ts>.<ext>`，扩展名取原文件名 + MIME 兜底防 `evil.exe` 伪装
  - `WebConfig.addResourceHandlers` 把 `/uploads/avatars/**` 映射到 `app.upload.avatar-dir`
  - JwtInterceptor `excludePathPatterns` 加 `/uploads/**`
  - dev 默认 `../finance/uploads/avatars`（与 Node 共享）
  - prod 默认 `/data/uploads/avatars`（备注挂 NFS / 持久卷）

## 阶段 7：预算 dashboard 日期 bug（commit f194fb0）

**两个并发 bug 一起修**，根因都是日期拼接：

- **DIFFS #9** 漏算：两端 `month+'-28'` 让 29-31 日 startDate 的预算被漏掉
- **隐藏 bug** 直接崩：Java `month+'-31'` 在 2/4/6/9/11 月抛 `Incorrect DATETIME value: '2026-06-31'`，正是 BudgetServiceTest 两个 pre-existing fail 的根因

修复：
- Java：`YearMonth.parse(month).atEndOfMonth()` 拿真实月末，账单右端再加 ` 23:59:59`
- Node：`new Date(year, mon, 0).getDate()` 同样拿真实月末

Java 测试从 71/73 → 73/73 ✅

## 阶段 8：cleanup 三连（commit 7b66235）

- **DIFFS #11** Java budget POST 响应包 `{id}`：`Result<Long>` → `Result<Map<String,Long>>`
- **DIFFS #15** category name 长度 ≤ 20 校验：Java + Node 两端 POST/PUT
- **DIFFS #10** Constants 清理：
  - 删 `DEFAULT_PAGE_SIZE` / `DEFAULT_NICKNAME_PREFIX`（完全未引用）
  - `MAX_NICKNAME_LENGTH 32 → 50` 对齐实际校验
  - UserServiceImpl 昵称 50 + BillServiceImpl 备注 200 改读 Constants

## 维护规约

后续改 API：

1. **先改 [contracts/openapi.yaml](../../contracts/openapi.yaml)**
2. 同一 PR 同步两端代码
3. 跑 `cd contracts && bash contract-test.sh` 验证
4. 更新 [contracts/DIFFS.md](../../contracts/DIFFS.md) 状态

详见 [contracts/README.md](../../contracts/README.md)。

## 已知遗留

- contract-test.sh 只检键存在 + 类型对，未做 JSON Schema 全量校验（可选增强）
- 没接 git pre-push hook / CI（可选增强）
- 部分历史 commit 含 mock JWT 字符串（已确认是 `tampered` / `invalid` / `faketoken` 测试值，无实际凭据风险）

## 已撤销

- `8be04fe chore(docs+uniapp): uniapp manifest 微调 + 技术文章 4 篇` → 由 `442fff4` revert
  - 原因：误把 `docs/technical-articles/` 11 个文件纳入提交，整体回退
