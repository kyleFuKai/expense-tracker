# Spring Boot 项目开发规范

> 适用于中大型后端服务开发，覆盖项目结构、编码、分层、安全、配置等全维度规范。

---

## 1. 项目结构

### 1.1 标准目录结构

```
project-name/
├── pom.xml
├── src/main/java/com/xingzhewk/project/
│   ├── Application.java                      # 启动类（唯一入口）
│   ├── config/                               # 配置类
│   ├── common/                               # 公共基础
│   │   ├── Result.java                       # 统一响应
│   │   ├── Constants.java                    # 常量
│   │   ├── enums/                            # 枚举
│   │   ├── exception/                        # 异常
│   │   └── validator/                        # 自定义校验器
│   ├── interceptor/                          # 拦截器
│   ├── filter/                               # Servlet 过滤器
│   ├── util/                                 # 工具类
│   ├── entity/                               # 实体类（与数据库表映射）
│   ├── dto/                                  # 数据传输对象
│   ├── vo/                                   # 视图对象（返回前端）
│   ├── mapper/                               # 数据访问层
│   ├── service/                              # 业务逻辑接口
│   │   └── impl/                             # 业务逻辑实现
│   └── controller/                           # 控制器
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── mapper/                               # XML 映射文件
├── src/test/java/                            # 单元测试
└── README.md
```

### 1.2 分层职责

| 层 | 职责 | 禁止事项 |
|---|---|---|
| **Controller** | 接收请求、参数校验、调用 Service、返回统一响应 | 禁止写业务逻辑、禁止直接操作 Mapper/SqlSession |
| **Service** | 业务逻辑编排、事务控制、跨组件协调 | 禁止操作 HttpServletRequest/Response、禁止返回 View |
| **Mapper** | 数据访问，封装数据库 CRUD | 禁止写业务逻辑、禁止包含校验逻辑 |
| **Interceptor** | 鉴权、日志、限流、上下文设置 | 禁止写业务逻辑、禁止直接返回业务结果 |
| **Filter** | 请求预处理、编码设置、CORS | 禁止写业务逻辑 |

### 1.3 包结构原则

- **按业务分包，不按技术分包**
- 包名：`com.xingzhewk.{module}.{layer}` 或 `com.xingzhewk.{layer}`
- 小项目按层分包，大项目按模块分包后再按层分包

### 1.4 包依赖方向

```
controller → service → mapper → entity
    ↓            ↓
    dto ←──→   vo
```

- 上层可依赖下层，**下层禁止依赖上层**
- `common` 为基础设施层，可被所有层依赖
- `entity` 禁止依赖任何业务包

---

## 2. 命名规范

### 2.1 包命名

- 全小写，域名倒置：`com.xingzhewk.project`
- 不超过 4 级：`com.xingzhewk.project.module.layer`

### 2.2 类命名

- **大驼峰（PascalCase）**
- 后缀标识职责：

| 类型 | 命名规则 | 示例 |
|---|---|---|
| Controller | `{模块}Controller` | `UserController` |
| Service 接口 | `{模块}Service` | `UserService` |
| Service 实现 | `{模块}ServiceImpl` | `UserServiceImpl` |
| Mapper | `{模块}Mapper` | `UserMapper` |
| Entity | `{表名单数}` | `User` |
| DTO | `{模块}DTO` 或 `{模块}{操作}DTO` | `UserCreateDTO` |
| VO | `{模块}VO` 或 `{模块}{操作}VO` | `UserDetailVO` |
| Config | `{功能}Config` | `MyBatisPlusConfig` |
| Exception | `{异常名}Exception` | `BusinessException` |
| Util | `{功能}Util` | `DateUtil` |
| Interceptor | `{功能}Interceptor` | `JwtInterceptor` |
| Filter | `{功能}Filter` | `CorsFilter` |

### 2.3 方法命名

- **小驼峰（camelCase）**
- 动词开头，语义清晰：

| 操作 | 前缀 | 示例 |
|---|---|---|
| 查询单个 | `get` / `find` | `getById` |
| 查询列表 | `list` | `listByStatus` |
| 新增 | `create` / `add` | `createUser` |
| 修改 | `update` / `edit` | `updateProfile` |
| 删除 | `delete` / `remove` | `deleteById` |
| 存在判断 | `exists` / `check` | `existsByEmail` |
| 统计 | `count` | `countByType` |

### 2.4 变量命名

- **小驼峰**：`userId`、`totalAmount`
- 常量：**全大写下划线**：`MAX_RETRY_COUNT`、`DEFAULT_PAGE_SIZE`
- 布尔变量：`isDeleted`、`hasPermission`（避免用 `deleted`、`permission` 表示布尔）
- 禁止单字母变量（`i`、`j` 用于循环，`e` 用于 catch 参数）
- 禁止拼音、拼音缩写

### 2.5 数据库命名

- 表名：**小写下划线**：`user_info`、`order_detail`
- 字段名：**小写下划线**：`user_id`、`created_at`
- 主键：**统一为 `id`**（bigint 自增）
- 外键：`{关联表名}_id`：`user_id`、`order_id`
- 时间字段：`created_at`、`updated_at`、`deleted_at`
- 状态字段：`status`、`is_deleted`（tinyint）

---

## 3. 分层编码规范

### 3.1 Controller 层

```java
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理")
public class UserController {

    private final UserService userService;

    // 构造器注入，禁止使用 @Autowired 字段注入
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public Result<UserVO> getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody UserCreateDTO dto) {
        return userService.create(dto);
    }
}
```

**规范要点：**
- 使用构造器注入，不用 `@Autowired` 字段注入
- `@RequestMapping` 加在类上，方法上用 `@GetMapping`/`@PostMapping`
- 参数校验用 `@Valid` + Bean Validation 注解
- Controller 方法不超过 15 行，只做请求转发和响应封装
- 禁止使用 `HttpServletRequest`/`HttpServletResponse`，用方法参数替代

### 3.2 Service 层

```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> create(UserCreateDTO dto) {
        // 1. 参数校验
        if (existsByEmail(dto.getEmail())) {
            return Result.error(409, "邮箱已存在");
        }

        // 2. 组装实体
        User user = new User();
        BeanUtils.copyProperties(dto, user);

        // 3. 数据访问
        int rows = userMapper.insert(user);
        if (rows <= 0) {
            throw new BusinessException("创建用户失败");
        }

        return Result.success(user.getId());
    }
}
```

**规范要点：**
- Service 接口不加后缀，实现类加 `ServiceImpl`
- 必须加 `@Transactional(rollbackFor = Exception.class)` 控制事务
- 业务逻辑集中在此层，Controller 禁止写业务逻辑
- 使用 `log` 记录关键操作，禁止 `System.out.println`
- 禁止依赖 Controller 层组件

### 3.3 Mapper 层

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // MyBatis-Plus 自带 CRUD 方法
    // 复杂查询写 XML 或使用 LambdaQueryWrapper
}
```

**规范要点：**
- 继承 `BaseMapper<T>`，简单 CRUD 用 MyBatis-Plus 自带方法
- 复杂 SQL 写在 XML 映射文件中
- 禁止使用字符串拼接 SQL
- 禁止在 Mapper 中写业务逻辑

### 3.4 Entity 层

```java
@Data
@TableName("user_info")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_name")
    private String userName;

    private String email;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

**规范要点：**
- 类名与表名一致（去掉下划线，转大驼峰）
- 使用 Lombok `@Data` 简化 Getter/Setter
- 字段类型与数据库一致：`DATETIME` → `LocalDateTime`，`TINYINT` → `Integer`
- 禁止在 Entity 中加业务方法

### 3.5 DTO / VO 层

```java
// DTO：接收前端请求参数
@Data
public class UserCreateDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 32, message = "用户名长度为2-32个字符")
    private String userName;

    @Email(message = "邮箱格式不正确")
    private String email;
}

// VO：返回前端的展示数据
@Data
public class UserVO {
    private Long id;
    private String userName;
    private String email;
    private LocalDateTime createdAt;
}
```

**规范要点：**
- DTO 用于接收请求参数，VO 用于返回响应数据
- **Entity 禁止直接暴露给前端**（可能泄露敏感字段如密码）
- Entity 到 VO 的转换使用 `BeanUtils.copyProperties` 或 MapStruct
- DTO 必须加参数校验注解（`@NotBlank`、`@Size`、`@Email` 等）

---

## 4. 统一响应格式

### 4.1 响应结构

```json
{
  "code": 0,
  "msg": "操作成功",
  "data": {}
}
```

### 4.2 状态码定义

| code | 含义 | 使用场景 |
|---|---|---|
| 0 | 成功 | 所有成功请求 |
| 400 | 参数错误 | 参数校验失败、格式不正确 |
| 401 | 未认证 | 未登录、token 过期、token 无效 |
| 403 | 无权限 | 已认证但无操作权限 |
| 404 | 资源不存在 | 请求的资源不存在 |
| 409 | 冲突 | 重复数据、状态冲突 |
| 429 | 请求频繁 | 触发限流 |
| 500 | 服务器错误 | 未捕获异常、系统故障 |

### 4.3 Result 封装

```java
@Data
public class Result<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(0);
        r.setMsg("操作成功");
        r.setData(data);
        return r;
    }

    public static <T> Result<T> error(int code, String msg) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }
}
```

### 4.4 分页响应

```json
{
  "code": 0,
  "data": {
    "list": [...],
    "total": 100,
    "page": 1,
    "pageSize": 10
  }
}
```

---

## 5. 异常处理

### 5.1 异常体系

```
RuntimeException
├── BusinessException          // 业务异常（可预期）
└── SystemException            // 系统异常（不可预期）
```

```java
@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
```

### 5.2 全局异常处理器

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusiness(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError().getDefaultMessage();
        return Result.error(400, msg);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<?> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.error(405, "请求方法不支持");
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("服务器内部错误", e);
        return Result.error(500, "服务器内部错误");
    }
}
```

**规范要点：**
- 业务异常用 `BusinessException` 抛出，全局处理器统一捕获
- 不要将原始堆栈或 SQL 错误返回给前端
- `catch` 后必须记录日志或抛出，禁止吞掉异常
- 全局异常处理器必须覆盖 `Exception.class` 兜底

---

## 6. 日志规范

### 6.1 日志级别

| 级别 | 用途 | 示例 |
|---|---|---|
| `ERROR` | 需要立即介入的故障 | 数据库连接失败、第三方接口超时 |
| `WARN` | 可恢复的异常、需要关注的情况 | 业务校验失败、重试成功 |
| `INFO` | 关键业务操作 | 用户登录、订单创建 |
| `DEBUG` | 开发调试信息 | 入参、出参详情 |

### 6.2 日志格式

```java
@Slf4j
public class OrderServiceImpl implements OrderService {

    public Result<Long> createOrder(Long userId, OrderDTO dto) {
        // 正确：使用占位符
        log.info("创建订单, userId={}, itemCount={}", userId, dto.getItems().size());

        try {
            // ...
            log.info("订单创建成功, orderId={}", order.getId());
        } catch (Exception e) {
            // 错误：只写 e.getMessage()
            // log.error("创建订单失败: {}", e.getMessage());
            // 正确：传入异常对象保留堆栈
            log.error("创建订单失败, userId={}", userId, e);
            throw e;
        }
    }
}
```

### 6.3 日志规范

- 使用 `@Slf4j` 注解，不手动定义 `Logger`
- **禁止使用占位符拼接字符串**（`"创建订单失败: " + orderId`），用 `{}` 占位符
- 禁止 `System.out.println`
- 禁止 `e.printStackTrace()`
- 日志内容应包含：操作标识、关键参数、结果标识
- 生产环境日志级别：`INFO`

### 6.4 日志配置示例（application.yml）

```yaml
logging:
  level:
    root: INFO
    com.xingzhewk.project: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/application.log
    max-size: 100MB
    max-history: 30
```

---

## 7. 安全规范

### 7.1 认证与鉴权

- 使用 JWT 或 Session 认证，Token 通过 `Authorization: Bearer <token>` 传递
- Token 有效期：短期有效（建议 2-24 小时），配合 Refresh Token 续期
- 拦截器统一校验，非法请求返回 401
- 敏感操作（改密码、提现）需二次验证

### 7.2 密码安全

- 使用 bcrypt 或 Argon2 加密存储，**禁止明文存储**
- 注册/修改密码时校验复杂度：长度 8-64、包含大小写字母+数字+特殊字符
- 错误提示统一为「账号或密码错误」，不泄露用户是否存在

### 7.3 接口限流

- 登录/注册接口限流：10-30 次/15分钟
- 其他接口限流：100-300 次/15分钟
- 限流后返回 429

### 7.4 SQL 注入防护

- 禁止字符串拼接 SQL，全部使用参数化查询（MyBatis `#{}` 占位符）
- 禁止直接拼接用户输入到 SQL 语句中

### 7.5 XSS 防护

- 前端对用户输入做转义
- 后端对富文本输入做白名单过滤（如使用 Jsoup）

### 7.6 配置安全

- **所有明文敏感信息必须写在 `.env` 文件中**（数据库连接、JWT 密钥、API Key、Redis 密码等）
- yml 配置文件中**禁止出现任何默认明文密码/密钥**，`${ENV_VAR:}` 默认值只能为空
- 引入 `dotenv-spring-boot-starter` 自动加载 `.env` 到 Spring Environment
- 创建 `.env.example` 模板，`.env` 加入 `.gitignore`
- 生产环境通过部署系统注入环境变量，禁止使用 `.env` 文件
- 生产环境禁用 Spring Boot 默认错误页

### 7.7 跨域控制

- 生产环境配置明确允许的域名白名单
- 禁止 `Access-Control-Allow-Origin: *` 配合 `allowCredentials: true`

---

## 8. 配置规范

### 8.1 `.env` 文件管理

所有明文敏感信息统一写入 `.env` 文件：

```bash
# .env
DB_HOST=116.198.226.68
DB_PORT=3306
DB_USER=root
DB_PASSWORD=your_database_password
DB_NAME=finance_db
JWT_SECRET=your-jwt-secret-key-at-least-32-characters
JWT_EXPIRATION=604800000
```

```bash
# .env.example（模板，加入版本控制）
DB_HOST=
DB_PORT=3306
DB_USER=root
DB_PASSWORD=
DB_NAME=
JWT_SECRET=
JWT_EXPIRATION=604800000
```

```
# .gitignore（.env 禁止提交）
.env
```

引入 `dotenv-spring-boot-starter`（或自定义 `EnvironmentPostProcessor`）自动加载 `.env`：

```xml
<!-- 方式一：使用第三方 starter（需 Maven 中央仓库） -->
<dependency>
    <groupId>io.github.cdimascio</groupId>
    <artifactId>dotenv-spring-boot-starter</artifactId>
    <version>3.1.0</version>
</dependency>

<!-- 方式二：自定义 EnvironmentPostProcessor（零依赖，推荐） -->
<!-- 见下方代码示例 -->
```

```java
// DotenvEnvironmentPostProcessor.java
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, Class<?> springApplicationClass) {
        Path envFile = Path.of(".env");
        if (!Files.exists(envFile)) return;
        Properties props = new Properties();
        try (InputStream is = new FileInputStream(envFile.toFile())) {
            props.load(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load .env file", e);
        }
        Map<String, Object> map = new HashMap<>();
        for (String name : props.stringPropertyNames()) {
            if (!environment.containsProperty(name)) {
                map.put(name, props.getProperty(name));
            }
        }
        if (!map.isEmpty()) {
            environment.getPropertySources().addLast(
                new MapPropertySource("dotenv", map));
        }
    }
}
```

注册（Spring Boot 3.x）：`src/main/resources/META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports`

```
com.xingzhewk.project.config.DotenvEnvironmentPostProcessor
```

### 8.2 多环境配置

```yaml
# application.yml
spring:
  profiles:
    active: dev

---
# application-dev.yml
server:
  port: 8080
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT:3306}/${DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:}
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # 开发环境打印 SQL

---
# application-prod.yml
server:
  port: 8080
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT:3306}/${DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
    username: ${DB_USER}
    password: ${DB_PASSWORD}
logging:
  level:
    root: WARN
    com.xingzhewk: INFO
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl  # 生产环境关闭 SQL 日志
```

### 8.3 配置规范

- **yml 中的 `${ENV_VAR:}` 默认值只能为空或通用值**（如端口 `3306`），禁止包含明文密码、密钥、主机地址
- 所有敏感信息通过 `.env` 或部署环境变量注入，yml 中仅使用占位符
- 不同环境差异配置使用 `application-{profile}.yml`
- 公共配置放 `application.yml`，环境特定配置放 `application-{profile}.yml`

---

## 9. 接口设计规范

### 9.1 RESTful URL

- 使用名词复数：`/api/users`、`/api/orders`
- 用 HTTP Method 区分操作：GET 查询、POST 新增、PUT 修改、DELETE 删除
- URL 不加动词：`/api/users` 而非 `/api/getUsers`
- 资源嵌套用层级：`/api/users/{id}/orders`

### 9.2 接口版本控制

- 使用 URL 版本：`/api/v1/users`
- 或通过请求头：`Accept: application/vnd.company.v1+json`

### 9.3 统一请求参数

- 分页参数：`page`（从 1 开始）、`pageSize`（默认 10，上限 100）
- 排序参数：`sort`（字段名）、`order`（asc/desc）
- 所有参数必须校验，不信任任何前端传入值

### 9.4 接口注释规范

**每个接口都必须有详细的注释说明**，包括但不限于以下内容：

```java
/**
 * 获取账单列表
 *
 * @param page      页码，从 1 开始
 * @param pageSize  每页条数，默认 50，最大 100
 * @param month     月份筛选（yyyy-MM），为空则查询全部
 * @param category_id 分类 ID 筛选
 * @param type      账单类型（EXPENSE / INCOME），为空则查询全部
 * @return          分页结果：{list: [...], total: N, page: 1, pageSize: 50}
 * @throws BusinessException 当 type 参数不合法时抛出
 */
@GetMapping
public Result<?> list(...)
```

**注释要求：**
- **每个 Controller 方法**必须包含接口功能描述、参数说明、返回值说明
- **复杂参数**（如枚举类型、状态值）必须列出所有可选值及含义
- **特殊逻辑**必须标注说明（如：`// 当分类存在关联账单时改为软归档而非物理删除`）
- **每个 Service 接口方法**必须包含功能描述、参数说明、返回值说明
- **DTO/VO 字段**必须包含行内注释说明含义，禁止字段名自解释就不写注释
- **统一返回格式**的字段含义必须在注释中明确（如 `list`/`total`/`page`/`pageSize`）
- **接口变更**时同步更新注释，禁止注释与代码不一致

---

## 10. 数据库规范

### 10.1 建表规范

- 每个表必须有主键 `id`（BIGINT 自增）
- 每个表必须有 `created_at`、`updated_at`（DATETIME）
- 软删除使用 `is_deleted`（TINYINT DEFAULT 0）
- 所有表使用 `InnoDB` 引擎、`utf8mb4` 字符集
- 字段禁止使用 TEXT/BLOB，改用独立表存储

### 10.2 索引规范

- 主键索引：`PRIMARY KEY (id)`
- 唯一索引：`UNIQUE KEY uk_{字段名} ({字段名})`
- 普通索引：`INDEX idx_{字段名} ({字段名})`
- 联合索引遵循最左前缀原则
- 索引列不能包含 NULL（使用 `NOT NULL DEFAULT ''` 或 `0`）

### 10.3 查询规范

- 禁止 `SELECT *`，明确指定字段
- 禁止大表全表查询，必须带 WHERE 条件
- 禁止在索引列上使用函数：`WHERE YEAR(created_at) = 2024`
- 分页查询必须带 ORDER BY，保证结果稳定
- 批量操作使用批量接口：`saveBatch`、`removeByIds`

---

## 11. 单元测试规范

### 11.1 测试原则

- 测试 Service 层业务逻辑，不测试简单 CRUD
- 一个测试方法只验证一个行为
- 遵循 Arrange-Act-Assert（AAA）结构

### 11.2 测试命名

```java
@Test
void testCreateOrder_withValidData_returnsSuccess() {
    // given
    // when
    // then
}
```

格式：`test{方法名}_{场景}_{期望结果}`

### 11.3 Mock 规范

- 使用 `@MockBean` 模拟依赖
- 使用 `@ExtendWith(MockitoExtension.class)` 开启 Mockito 扩展
- 禁止 Mock 自己写的类，只 Mock 外部依赖（第三方 SDK、远程服务）

---

## 12. Git 提交规范

### 12.1 Commit Message 格式

```
<type>: <subject>

<body>
```

### 12.2 Type 枚举

| 类型 | 含义 |
|---|---|
| `feat` | 新功能 |
| `fix` | 修复 Bug |
| `docs` | 文档变更 |
| `style` | 代码格式（不影响功能） |
| `refactor` | 重构（非新增、非修复） |
| `perf` | 性能优化 |
| `test` | 测试相关 |
| `chore` | 构建/工具链变更 |
| `revert` | 回退提交 |

### 12.3 示例

```
feat: 新增用户注册接口

- 添加 UserController.create()
- 添加 UserService.create() 实现
- 添加参数校验和统一响应
- 集成 JWT 鉴权

fix: 修复分页查询越界问题

- 当 page 超过总页数时返回空列表而非报错
```

---

## 13. 依赖管理

### 13.1 依赖原则

- 优先使用 Spring Boot 官方 Starter
- 控制依赖数量，不引入未使用的依赖
- 定期升级依赖版本，修复安全漏洞

### 13.2 常用依赖清单

```xml
<dependencies>
    <!-- Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- MyBatis-Plus -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>${mybatis-plus.version}</version>
    </dependency>

    <!-- MySQL -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- 参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- .env 文件支持（自定义 EnvironmentPostProcessor，零依赖） -->

    <!-- Spring Security Crypto（bcrypt 密码加密） -->
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-crypto</artifactId>
    </dependency>

    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>${jjwt.version}</version>
    </dependency>

    <!-- 测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 14. 部署规范

### 14.1 打包

- 使用 `mvn clean package -DskipTests` 打包
- 输出 `target/project-name.jar`
- 禁止直接运行 `mvn spring-boot:run` 在生产环境

### 14.2 启动命令

```bash
java -jar -Dspring.profiles.active=prod project-name.jar \
    --DB_HOST=${DB_HOST} \
    --DB_USER=${DB_USER} \
    --DB_PASSWORD=${DB_PASSWORD}
```

### 14.3 健康检查

- 启用 Spring Boot Actuator：`GET /actuator/health`
- 容器化部署时配置 livenessProbe 和 readinessProbe

---

## 15. 提交与发布规范

### 15.1 代码提交流程

每次代码变更必须按以下顺序执行，缺一不可：

```
代码修改 → 文档同步 → 全面测试 → 提交本地 → 推送到 GitHub
```

### 15.2 文档同步

代码变更必须同步更新相关文档，**文档与代码不等效即为事故**：

| 代码变更类型 | 需要同步的文档 |
|---|---|
| 新增接口 | README 接口表、API 文档 |
| 修改接口参数/响应 | README 接口表、API 文档、测试脚本 |
| 新增/修改配置项 | `.env.example`、README 配置说明 |
| 数据库结构变更 | `README.md`、数据库脚本（DDL） |
| 新增依赖 | `pom.xml`、README 环境要求 |
| 项目结构变化 | 项目 README、开发规范文档 |
| 修复 Bug | 测试脚本补充对应测试用例 |

**文档同步原则：**
- 文档与代码必须同一次 Commit，不允许「代码先改，文档后补」
- 测试脚本是项目最重要的文档之一，必须随接口变更同步更新
- `.env.example` 必须与 `.env` 实际使用的变量保持同步

### 15.3 全面测试

提交前必须通过以下所有测试，**测试不通过禁止推送到远程**：

```bash
# 1. 编译通过
mvn clean compile

# 2. 单元测试
mvn test

# 3. 启动应用并执行全量接口测试
mvn spring-boot:run -Dspring.profiles.active=dev
bash test_java_full.sh
# 预期：100% 通过

# 4. 打包验证
mvn clean package -DskipTests
```

**测试覆盖要求：**
- 新功能必须有对应的测试用例
- 修复 Bug 必须补充回归测试用例
- 接口测试通过率 100% 才可提交

### 15.4 推送前最终检查

- [ ] 全量接口测试通过（100% pass rate）
- [ ] 相关文档已同步更新
- [ ] `.env.example` 与实际变量一致
- [ ] 代码编译无警告
- [ ] Commit message 符合规范

---

## 16. 规范执行清单

提交前检查：

- [ ] 无硬编码密码、密钥、数据库连接
- [ ] yml 配置文件中无明文敏感信息默认值（`${}` 只能为空）
- [ ] `.env` 在 `.gitignore` 中，`.env.example` 模板存在
- [ ] 所有异常被全局处理器捕获，不返回原始堆栈
- [ ] Service 层方法有事务控制
- [ ] Controller 方法简洁（不超过 15 行）
- [ ] 使用构造器注入，不用 @Autowired 字段注入
- [ ] 日志使用占位符，包含关键参数
- [ ] 参数校验使用 Bean Validation 注解
- [ ] Entity 不直接暴露给前端（使用 VO 转换）
- [ ] 所有接口方法有详细注释（功能描述、参数说明、返回值说明）
- [ ] DTO/VO 字段有注释说明含义
- [ ] 注释与代码保持一致，无过时注释
- [ ] Commit message 格式正确
- [ ] 单元测试通过
- [ ] **相关文档已同步更新**（README、测试脚本、.env.example）
- [ ] **全量接口测试 100% 通过**
