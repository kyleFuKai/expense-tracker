# PRD: 账单数据导出功能 (CSV/Excel)

## 1. 需求概述

用户在设置页面看到"导出账单"标记为"V2.0 即将上线"，需要将此功能实现。支持将用户的账单数据导出为 CSV 或 Excel 文件格式下载。

## 2. 功能设计

### 2.1 导出格式

| 格式 | 说明 | 优先级 |
|------|------|--------|
| CSV | 轻量通用格式，UTF-8 BOM 编码（兼容 Excel 打开中文不乱码） | 高 |
| Excel (.xlsx) | 支持表头样式、金额格式化，适合办公场景 | 高 |

### 2.2 导出字段

| 列名 | 对应字段 | 说明 |
|------|----------|------|
| 账单时间 | `bill_time` | 格式：yyyy-MM-dd HH:mm:ss |
| 类型 | `type` | 显示为"支出"/"收入"（非 EXPENSE/INCOME） |
| 分类 | `category.name` | LEFT JOIN 获取，已删除分类显示为空 |
| 金额 | `amount` | 保留2位小数 |
| 备注 | `remark` | 原样导出 |
| 创建时间 | `created_at` | 格式：yyyy-MM-dd HH:mm:ss |

### 2.3 筛选条件（与列表接口保持一致）

| 参数 | 类型 | 说明 |
|------|------|------|
| `format` | String | `csv` 或 `xlsx`，默认 `csv` |
| `month` | String | 月份筛选 yyyy-MM |
| `type` | String | EXPENSE / INCOME |
| `category_id` | Long | 分类筛选 |
| `keyword` | String | 备注关键词搜索 |
| `start_date` | String | 起始日期 yyyy-MM-dd |
| `end_date` | String | 结束日期 yyyy-MM-dd |

### 2.4 文件名规则

- 按月导出：`bills_2026-05.csv` / `bills_2026-05.xlsx`
- 按日期范围：`bills_2026-01-01_to_2026-03-31.xlsx`
- 无筛选条件：`bills_all_20260522.xlsx`（带当前日期）

### 2.5 安全限制

- 仅导出当前登录用户的账单（JWT 鉴权）
- 单次导出最多 50,000 行，超出返回错误提示缩小范围

## 3. API 设计

```
GET /api/bills/export
Content-Disposition: attachment; filename="bills_2026-05.csv"
```

**响应：** 文件下载（非 JSON）
- CSV: `Content-Type: text/csv; charset=utf-8`
- Excel: `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`

## 4. 技术实现

### 4.1 Java 后端

**新增依赖 (pom.xml):**
```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

**新增文件:**
- `mapper/BillMapper.java` — 新增 `selectForExport()` 方法（`@Select` 注解，带 LEFT JOIN）
- `service/BillExportService.java` — 导出服务接口
- `service/impl/BillExportServiceImpl.java` — 实现 CSV 和 Excel 写入

**修改文件:**
- `controller/BillController.java` — 新增 `GET /export` 端点

**关键实现细节:**
- CSV 使用 `java.io.BufferedWriter` + UTF-8 BOM 前缀
- Excel 使用 Apache POI `XSSFWorkbook`，表头加粗，金额列 `#,##0.00` 格式
- SQL: `SELECT b.bill_time, b.type, c.name, b.amount, b.remark, b.created_at FROM bill b LEFT JOIN category c ON b.category_id = c.id WHERE b.user_id = ? ...`
- 动态条件使用 MyBatis `<script>` 标签

### 4.2 Node.js 后端

**修改文件:**
- `backend/routes/bills.js` — 新增 `GET /export` 路由（放在 `/:id` 路由之前）

**关键实现细节:**
- CSV 零依赖：字符串拼接 + `﻿` BOM 前缀
- Excel 使用 `exceljs` 包（`npm install exceljs`）
- SQL 复用现有列表查询的 conditions 数组模式，去掉 LIMIT/OFFSET

### 4.3 前端

**修改文件:**
- `finance/pages/settings.html` — 将"导出账单"从 `onclick="showV2Tip()"` 改为跳转到新页面
- `finance/pages/export-bills.html` — 新建导出页面（月份选择 + 格式选择 + 导出按钮）

## 5. 测试用例

### 5.1 Java 后端（JUnit 单元测试）

使用 `@SpringBootTest` + `MockMvc` 编写测试类，测试文件位于 `src/test/java/com/xingzhewk/`。

| 用例 | 说明 | 期望 |
|------|------|------|
| EXPORT-01 | 导出当月账单 CSV | 返回码 200，Content-Type 为 text/csv，内容含表头"账单时间" |
| EXPORT-02 | 导出当月账单 Excel | 返回码 200，Content-Type 为 xlsx，文件大小 > 100 bytes |
| EXPORT-03 | 导出指定月份 | 仅返回该月份数据 |
| EXPORT-04 | 按类型筛选导出 | 仅返回 EXPENSE 或 INCOME |
| EXPORT-05 | 空数据导出 | 文件包含表头，无数据行 |
| EXPORT-06 | 导出全部数据（无筛选） | 返回所有账单 |
| EXPORT-07 | 超 50000 行限制 | 返回错误提示 |
| EXPORT-08 | 未登录访问 | 返回 401 |
| EXPORT-09 | 中文备注 CSV | Excel 打开不乱码（BOM 生效） |
| EXPORT-10 | 已删除分类的账单 | 分类列为空，不报错 |

### 5.2 Node.js 后端（Shell 脚本 + curl）

复用现有 `test_full_187.sh` 脚本风格，新增导出相关用例，用 curl 请求 + JSON 断言。

## 6. 实施步骤

1. Java 后端：添加 POI 依赖 → 新增 Mapper 方法 → 新增 Service → 新增 Controller 端点
2. Node.js 后端：安装 exceljs → 新增 export 路由
3. 前端：创建 export-bills.html → 修改 settings.html 入口
4. 测试：
   - Java：编写 JUnit 单元测试（`@SpringBootTest` + `MockMvc`），运行 `mvn test`
   - Node.js：shell 脚本 curl 测试
5. 文档：更新 README API 表
6. 提交：按规范提交代码
