# 每日财务管家 — Spring Boot 后端服务

> 技术栈：Spring Boot 3.2 + MyBatis-Plus 3.5 + MySQL 8.0 + JWT

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.8+
- MySQL 8.0+

### 1. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env 填入数据库连接和 JWT 密钥
```

或直接在 `application-dev.yml` 中修改 `${ENV_VAR}` 默认值。

### 2. 初始化数据库

```bash
mysql -u root -p < ../004.数据库脚本(DBA)/001_schema_ddl.sql
mysql -u root -p < ../004.数据库脚本(DBA)/002_seed_data.sql
```

### 3. 编译运行

```bash
# 开发环境
mvn spring-boot:run

# 生产打包
mvn clean package -DskipTests
java -jar target/expense-tracker-server-1.0.0.jar --spring.profiles.active=prod
```

## API 接口

与 Node.js 后端完全一致，响应格式统一为：

```json
{ "code": 0, "msg": "操作成功", "data": {} }
```

### 认证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/register | 注册 |
| POST | /api/auth/login | 登录 |

### 用户接口（需认证）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/user/profile | 获取用户信息 |
| PUT | /api/user/profile | 更新用户信息 |
| PUT | /api/user/password | 修改密码 |

### 账单接口（需认证）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/bills | 账单列表（分页） |
| GET | /api/bills/:id | 账单详情 |
| POST | /api/bills | 创建账单 |
| PUT | /api/bills/:id | 修改账单 |
| DELETE | /api/bills/:id | 删除账单 |
| GET | /api/bills/stats/month | 月度统计 |

### 分类接口（需认证）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/categories | 分类列表 |
| POST | /api/categories | 创建分类 |
| PUT | /api/categories/:id | 修改分类 |
| DELETE | /api/categories/:id | 删除分类 |

### 预算接口（需认证）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/budgets | 预算列表 |
| GET | /api/budgets/dashboard | 预算仪表盘 |
| POST | /api/budgets | 创建/更新预算 |
| DELETE | /api/budgets/:id | 停用预算 |

### 健康检查

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/health | 健康检查 |

## 前端切换后端

修改前端 `assets/js/auth.js` 第 9-10 行：

```javascript
// Node.js 后端
// var API_BASE = 'http://localhost:3000';
// Java 后端
var API_BASE = 'http://localhost:8080';
```

## 项目结构

```
expense-tracker-server/
├── src/main/java/com/expense/
│   ├── controller/          # 控制器
│   ├── service/             # 业务逻辑
│   ├── mapper/              # 数据访问
│   ├── entity/              # 实体类
│   ├── dto/                 # 请求参数
│   ├── vo/                  # 响应视图
│   ├── common/              # 公共基础
│   ├── config/              # 配置类
│   ├── interceptor/         # 拦截器
│   └── util/                # 工具类
└── src/main/resources/
    ├── application.yml
    ├── application-dev.yml
    └── application-prod.yml
```

## 开发规范

详见 [SpringBoot项目开发规范.md](../SpringBoot项目开发规范.md)
