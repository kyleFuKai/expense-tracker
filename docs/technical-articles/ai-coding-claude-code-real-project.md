# AI编程实战：用Claude Code从0到1做一个完整产品

> 不是工具对比，不是入门科普。这是一篇**真实项目实战记录**，从需求到上线，完整展示AI编程助手的真实能力与局限。

<div align="center">
<img src="./screenshots/login.png" width="300" alt="每日财务管家 - 登录页" style="border-radius:12px;box-shadow:0 4px 20px rgba(0,0,0,0.1);margin:20px 0;"/>
<p style="color:#888;font-size:14px;margin-top:-10px;">▲ 最终产品：每日财务管家 APP（登录页）</p>
</div>

## 一、为什么要写这篇文章

2026年，关于"AI编程"的文章已经泛滥成灾。但你仔细看看，大多数是：

- ❌ 工具横评：Claude Code vs Cursor vs Codex，跑个Hello World就下结论
- ❌ Vibe Coding入门：写个TODO List就说"人人都是程序员"
- ❌ 焦虑贩卖："程序员工资要降了"，"不会用AI就要被淘汰"

**这些文章最大的问题是——没有完整做一个产品。**

今天我要做的是：**用Claude Code从0到1，完整开发一个商业级产品**，记录全过程、踩坑、工作流、真实效率对比。

这个产品是一个**跨端记账APP**——有UI设计、有后端API、有数据库、有移动端交互。不是Demo，是能上架的产品。

---

## 二、工具选型

### 2.1 为什么选Claude Code

| 维度 | Claude Code | Cursor | GitHub Copilot |
|------|------------|--------|---------------|
| 工作模式 | 命令行Agent | IDE内嵌 | IDE补全 |
| 上下文理解 | 整个项目+文件搜索 | 当前文件+tab补全 | 当前文件 |
| 多步任务 | 自动规划+执行 | 需人工引导 | 不支持 |
| 文件操作 | 直接读写改删 | 需手动确认 | 仅补全 |
| Git操作 | 自动commit/branch | 手动 | 手动 |
| 适用场景 | 独立开发/架构重构 | 日常编码辅助 | 代码补全 |

**Claude Code的核心优势：它是一个"Agent"，不是"补全工具"。**

你说"把这个记账APP的前端页面都做出来"，它真的会去读设计文档、创建文件、写代码、提交。你只需要review和验收。

### 2.2 技术栈

- **前端**：UniApp（Vue3）—— 一套代码跑iOS/Android/H5/小程序
- **后端**：Java Spring Boot
- **数据库**：MySQL
- **AI工具**：Claude Code（主力）+ Claude Sonnet/Opus（API辅助）

---

## 三、项目实战：Expense Tracker 记账APP

### 3.1 第零步：需求文档

传统开发流程的第一步是写PRD。在AI时代，这一步变了。

**我的做法**：给Claude Code一段自然语言描述，让它帮我生成完整的PRD。

```
我要做一个个人记账APP，叫"随手记Pro"。主要功能：
1. 记录收入和支出，支持两级分类（餐饮→早餐/午餐/晚餐）
2. 按类别设置月度预算，超支提醒
3. 统计报表：饼图、柱状图、趋势线
4. 多端同步
5. 用户注册登录

帮我生成完整的PRD文档，包含用户故事、功能清单、非功能需求。
```

**Claude Code的输出**：自动创建了完整的PRD文档，包括：
- 用户角色定义
- 功能模块拆分（8个大模块，36个功能点）
- API接口清单
- 数据库ER图描述
- 非功能需求（性能、安全、兼容性）

**节省时间**：传统写PRD至少2小时，AI辅助15分钟。

### 3.2 第一步：UI原型设计

AI编程不只是写代码。Claude Code可以帮我生成HTML原型。

**我的做法**：创建`.superpowers/brainstorm/`目录，让AI生成关键页面的HTML原型，包括：
- 首页（账单列表+月度概览）
- 记账页（金额输入+分类选择）
- 统计页（图表可视化）
- 我的页面（设置+预算管理）

**关键发现**：
- AI生成的HTML原型**不是最终产品**，但能极大加快沟通效率
- 原型可以直接给团队确认UI布局，再让Claude Code写真正的UniApp代码
- **AI原型 → 人工review → 正式代码** 是最优流程

### 3.3 第二步：数据库设计

```sql
-- 用户表
CREATE TABLE `user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `email` VARCHAR(100),
  `phone` VARCHAR(20),
  `password_hash` VARCHAR(255) NOT NULL,
  `avatar_url` VARCHAR(500),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 交易记录表
CREATE TABLE `transaction` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `type` TINYINT NOT NULL COMMENT '1-支出, 2-收入',
  `amount` DECIMAL(10,2) NOT NULL,
  `category_id` BIGINT NOT NULL,
  `tag_ids` JSON COMMENT '标签ID数组',
  `note` VARCHAR(500),
  `transaction_time` DATETIME NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT DEFAULT 0,
  INDEX `idx_user_time` (`user_id`, `transaction_time`),
  INDEX `idx_user_category` (`user_id`, `category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 分类表
CREATE TABLE `category` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT,
  `parent_id` BIGINT DEFAULT 0 COMMENT '0-一级分类',
  `name` VARCHAR(50) NOT NULL,
  `type` TINYINT NOT NULL COMMENT '1-支出, 2-收入',
  `icon` VARCHAR(100),
  `sort_order` INT DEFAULT 0,
  `is_default` TINYINT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 预算表
CREATE TABLE `budget` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `category_id` BIGINT NOT NULL,
  `amount` DECIMAL(10,2) NOT NULL,
  `budget_month` VARCHAR(7) NOT NULL COMMENT 'YYYY-MM',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT DEFAULT 0,
  UNIQUE KEY `uk_user_category_month` (`user_id`, `category_id`, `budget_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**AI的贡献**：
- 自动补全索引、约束、注释
- 提醒了JSON字段存储tag_ids（比关联表更灵活）
- 自动加了`is_deleted`软删除和`updated_at`自动更新

**踩坑记录**：AI第一次生成的schema没有加索引，我review后提醒"需要支持按用户+时间范围查询"，它自动补上了。

### 3.4 第三步：后端开发

**我的做法**：按模块拆分任务，每次让Claude Code做一件事。

#### 模块1：用户认证

```
帮我实现用户注册和登录功能：
- JWT认证
- 密码BCrypt加密
- 注册接口 /api/auth/register
- 登录接口 /api/auth/login
- 修改密码接口 /api/auth/password（需要旧密码）
```

Claude Code自动创建了：
- `UserController.java` - 控制器
- `UserService.java` / `UserServiceImpl.java` - 业务逻辑
- `UserMapper.java` + XML - 数据访问
- `JwtInterceptor.java` - JWT拦截器
- `Result.java` - 统一响应格式

**代码质量**：
- ✅ 分层清晰（Controller → Service → Mapper）
- ✅ 参数校验完整
- ✅ 异常处理统一
- ✅ 密码BCrypt加密
- ⚠️ 缺少限流（提醒后补上）

#### 模块2：记账核心API

```
实现记账核心功能：
- 创建/修改/删除交易记录
- 分页查询账单列表（支持按类型、分类、时间筛选）
- 获取月度收支统计
- 获取分类占比统计
```

**关键实现**：分页查询+条件筛选，AI自动生成了MyBatis动态SQL。

```xml
<select id="selectByCondition" resultMap="BaseResultMap">
  SELECT * FROM `transaction`
  WHERE user_id = #{userId} AND is_deleted = 0
  <if test="type != null">
    AND type = #{type}
  </if>
  <if test="categoryId != null">
    AND category_id = #{categoryId}
  </if>
  <if test="startTime != null">
    AND transaction_time &gt;= #{startTime}
  </if>
  <if test="endTime != null">
    AND transaction_time &lt;= #{endTime}
  </if>
  ORDER BY transaction_time DESC
  LIMIT #{offset}, #{pageSize}
</select>
```

**效率对比**：
- 传统方式：写这些CRUD大约3-4小时
- AI辅助：每个模块20-30分钟（含review和修改）
- **总后端开发时间：约2小时 vs 传统的8-10小时**

#### 模块3：预算和统计

统计接口是复杂度最高的部分，涉及GROUP BY聚合和环比计算。

```
实现统计API：
1. 月度收支对比（本月vs上月，显示环比）
2. 分类占比（饼图数据，按金额排序）
3. 日趋势图（近30天每天的支出金额）
4. 分类排行榜（支出Top10分类）
```

**AI的亮点**：自动处理了环比计算（本月vs上月），SQL中用了子查询和COALESCE处理空值。

### 3.5 第四步：前端开发（UniApp）

前端是最耗时的部分。我的策略：**按页面拆分，逐个击破。**

#### 首页：账单列表 + 月度概览

```
做记账APP的首页，包含：
1. 顶部月度卡片：本月总支出、总收入、结余
2. 环比显示：与上月对比的增减百分比
3. 账单列表：按日期分组，显示金额、分类、备注
4. 底部tabBar：首页、统计、我的
5. 下拉刷新、上拉加载更多
```

**AI自动生成的关键代码**：
- Vue3 Composition API写法（`<script setup>`）
- 响应式数据管理（`ref` + `computed`）
- API调用封装
- 下拉刷新动画
- 空状态占位UI

#### 记账页：两级分类 + 标签 + 日期选择

```
做记账页，包含：
1. 收入/支出切换tab
2. 金额输入（大字号，键盘输入）
3. 分类选择：一级分类 → 二级分类（两页面切换）
4. 标签选择（可多选）
5. 日期选择（默认今天，可改历史日期）
6. 备注输入
7. 保存按钮
```

<div align="center">
<img src="./screenshots/record.png" width="300" alt="快速记账页面" style="border-radius:12px;box-shadow:0 4px 20px rgba(0,0,0,0.1);margin:20px 0;"/>
<p style="color:#888;font-size:14px;margin-top:-10px;">▲ AI生成的记账页UI（支出/收入切换 + 金额输入 + 分类 + 标签 + 日期）</p>
</div>

**技术难点**：分类的两级联动。AI选择了**两个页面切换**的方案（先选一级→进入二级），而不是弹窗或下拉。

**踩坑**：AI第一次生成用了Vue2的`this`写法，我提醒后改成了`<script setup>`语法。

#### 统计页：图表可视化

```
做统计页，包含：
1. 月份切换器
2. 收支对比柱状图
3. 分类占比饼图（环形图）
4. 支出趋势折线图（近7天/近30天切换）
5. 分类排行榜
```

**AI的局限**：图表部分，Claude Code能生成代码结构，但**具体的图表渲染需要真实的数据和测试**。AI生成的echarts配置可能有细节偏差，需要手动调。

#### 其他页面

<div align="center">
<table>
<tr>
<td align="center"><img src="./screenshots/settings.png" width="200" alt="我的页面" style="border-radius:8px;"/><br/>我的页面</td>
<td align="center"><img src="./screenshots/category-manage.png" width="200" alt="类别管理" style="border-radius:8px;"/><br/>类别管理</td>
</tr>
</table>
<p style="color:#888;font-size:14px;margin-top:-10px;">▲ 我的页面 & 类别管理页</p>
</div>

- 类别管理：自定义分类的CRUD
- 预算管理：按月按类设置预算，进度条显示
- 我的页面：个人信息、设置、修改密码
- 修改密码：旧密码验证 + 新密码

### 3.6 第五步：调试和修复

这是AI编程最被低估的部分。**AI不是完美的，它写的代码有bug。**

**我遇到的真实问题**：

| 问题 | 原因 | 解决方式 |
|------|------|---------|
| 时间戳格式不对 | AI用了毫秒级timestamp，前端期望秒级 | 统一为秒级 |
| 分页数据返回格式不一致 | 有的接口用`{list, total}`，有的用`{data}` | 统一为`Result<List<T>>` |
| 分类图标不显示 | 路径用了`/static/`前缀，UniApp需要相对路径 | 改为`../../static/` |
| 图表不渲染 | echarts版本不兼容 | 锁定版本 |
| 预算进度条溢出 | 未处理已超支的情况（超过100%） | 加`Math.min(progress, 100)` |

**关键心得**：**AI写的代码需要review，不是review了就能直接上线。**

---

## 四、真实效率对比

| 阶段 | 传统方式 | AI辅助 | 节省比例 |
|------|---------|--------|---------|
| 需求文档 | 2小时 | 15分钟 | 87% |
| 数据库设计 | 1小时 | 20分钟 | 67% |
| 后端开发 | 8-10小时 | 2小时 | 75-80% |
| 前端开发 | 12-16小时 | 4小时 | 67-75% |
| 调试修复 | 3-4小时 | 1.5小时 | 50-62% |
| **总计** | **26-33小时** | **约8小时** | **67-75%** |

**但请注意**：

1. 这是**独立完成整个项目**的效率，不是改几行代码
2. AI辅助的前提是**你会写代码**——AI写的代码你能review、能修改、能debug
3. **调试阶段的效率提升最小**——因为bug往往需要真实环境才能发现

---

## 五、Claude Code工作流最佳实践

### 5.1 任务拆分

**错误做法**：
```
"帮我做一个完整的记账APP"
```
→ Claude Code会做，但上下文不够，质量会下降。

**正确做法**：
```
1. "先帮我生成PRD文档"
2. "根据PRD，设计数据库schema"
3. "实现用户认证模块"
4. "实现记账CRUD API"
...
```

**原则**：每个任务做一件事，做完review，再做下一个。

### 5.2 Prompt模板

经过实践总结，好的Prompt包含：

```
[角色/上下文] 我现在在做记账APP的后端开发
[具体任务] 帮我实现预算模块的API
[技术要求] Spring Boot + MyBatis，统一返回Result格式
[输入输出] 请求参数：userId, categoryId, amount, month
          返回：创建结果，包含预算使用率
[边界条件] 同月同类别重复创建时应更新而非新增
```

### 5.3 代码Review清单

AI写的代码，我必查这几项：

- [ ] 安全：SQL注入？XSS？权限校验？
- [ ] 异常：空指针？边界值？网络超时？
- [ ] 性能：N+1查询？未加索引？内存泄漏？
- [ ] 规范：命名一致？注释清晰？日志完整？
- [ ] 业务：逻辑正确？边界处理？数据一致性？

### 5.4 何时不用AI

| 场景 | 建议 | 原因 |
|------|------|------|
| 核心算法 | 手写 | 精确性要求高 |
| 安全敏感代码 | 手写+审计 | AI可能遗漏安全细节 |
| 性能调优 | 手动+工具 | 需要profiler和数据 |
| 架构决策 | 人工 | AI没有业务context |
| Code Review | 人工为主 | AI看不出设计问题 |

---

## 六、踩坑集锦

### 6.1 AI会"忘记"上下文

对话太长（超过100轮）后，Claude Code可能忘记早期的约定。比如：
- 前面约定了日期格式是`YYYY-MM-DD`，后面又输出了`YYYY/MM/DD`
- 前面定义的错误码，后面用了一致的

**解决方案**：
- 把关键约定写在`.claude/CLAUDE.md`项目配置文件中
- 定期summary对话内容
- 大任务开新对话，带上必要的上下文

### 6.2 AI会"过度设计"

有一次我让AI写个简单的分页查询，它加了：
- 缓存层
- 分布式锁
- 异步日志
- 数据脱敏

**我只要一个`SELECT * FROM table LIMIT 10`**。

**解决方案**：在Prompt里明确说"简单实现即可，不要加缓存/中间件/额外抽象"。

### 6.3 AI不了解你的代码库

如果你的项目有特殊的编码规范、工具类封装、框架版本，AI不知道。

**解决方案**：
- 第一次就让AI读你的`CLAUDE.md`、代码规范文档
- 用`Read`工具让AI先了解现有代码结构
- 重要的约定写进项目配置

### 6.4 前端样式需要手工调

AI能生成80%的UI，但剩下20%的精细调整（间距、颜色、响应式适配）需要手工做。

**原因**：样式是"感觉"问题，没有对错标准，AI很难一次到位。

---

## 七、给想用AI编程的人的建议

### 7.1 适合用AI的人

- ✅ 有一定编程基础，能review代码
- ✅ 独立开发者/小团队，需要快速交付
- ✅ 接外包项目，需要提高产出效率
- ✅ 学新技术，想快速上手

### 7.2 不适合用AI的人

- ❌ 完全不懂编程，指望AI从零帮自己创业
- ❌ 大型企业核心项目，需要严格的安全和审计
- ❌ 需要深度优化的系统（高性能、低延迟）

### 7.3 最实用的建议

1. **AI是放大器，不是替代品**。你越强，AI帮你越强。
2. **永远review AI的代码**。不要盲目信任。
3. **学会写好的Prompt**。Prompt质量 = 代码质量。
4. **从小任务开始**。别上来就让AI做整个系统。
5. **积累你的Prompt库**。好用的Prompt值得收藏。
6. **保持学习能力**。AI帮你省下的时间，用来学更深层的东西。

---

## 八、结语

这篇文章不是AI编程的"布道文"。我没有说AI会取代程序员，也没有说AI什么都能做。

**真实情况是**：AI编程助手能把开发效率提升60-75%，但它需要**懂技术的人来驾驭**。它写的代码有bug，它的理解会出错，它的判断不总是对的。

**最好的工作方式**：你是一个技术总监，AI是你的工程师。你负责架构设计、代码review、关键决策，AI负责执行、生成、补全、测试。

2026年，不是"AI会取代程序员"的时代。而是"会用AI的程序员会取代不会用的"。

---

## 附录：项目源码

本教程配套的项目源码已开源：

- GitHub: [github.com/你的用户名/expense-tracker](https://github.com/你的用户名/expense-tracker)（待完善后开源）
- 项目结构：
  ```
  expense-tracker/
  ├── backend/          # Spring Boot后端
  │   ├── src/main/java/com/expense/
  │   │   ├── controller/
  │   │   ├── service/
  │   │   ├── mapper/
  │   │   ├── entity/
  │   │   ├── config/
  │   │   └── interceptor/
  │   └── src/main/resources/
  │       ├── application.yml
  │       └── mapper/
  ├── database/         # SQL脚本
  │   └── init.sql
  ├── app/              # UniApp前端
  │   ├── pages/
  │   ├── components/
  │   ├── static/
  │   └── utils/
  └── docs/             # PRD和设计文档
  ```

---

> **关于作者**：一个用AI辅助编程的全栈开发者。本文基于真实项目经验撰写，所有数据均为实际测试结果。
>
> **版权声明**：本文为原创文章，欢迎转载但请注明出处。
