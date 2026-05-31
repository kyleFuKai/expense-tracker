# 统计页环比对比 (V1.6) 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在统计页总支出/收入卡片中增加本月 vs 上月环比百分比显示。

**Architecture:** 后端扩展现有 `monthlyStats` 接口，查询上月数据并计算环比，前端在支出/收入卡片中展示环比箭头和百分比。

**Tech Stack:** Java 21, Spring Boot 3.2.5, MyBatis-Plus 3.5.7, Node.js/Express, MySQL 8.0, HTML/Tailwind

---

## 文件结构总览

### 新增文件
| 文件 | 说明 |
|------|------|
| `BillStatsVO.java` 中的字段 | `expenseChange`, `incomeChange` (BigDecimal) |
| `BillMapper.java` 中的方法 | `selectSumIncome` (汇总收入) |

### 修改文件
| 文件 | 变更 |
|------|------|
| `.../java/com/xingzhewk/vo/BillStatsVO.java` | 新增 `expenseChange`, `incomeChange` 字段 |
| `.../java/com/xingzhewk/mapper/BillMapper.java` | 新增 `selectSumIncome` 方法 |
| `.../java/com/xingzhewk/service/impl/BillServiceImpl.java` | `monthlyStats()` 增加上月查询和环比计算 |
| `003.前端代码/backend/routes/bills.js` | stats 接口增加上月环比计算 |
| `003.前端代码/finance/pages/statistics.html` | 总支出/收入卡片增加环比显示 |

---

### Task 1: Java BillStatsVO 新增环比字段

**Files:**
- Modify: `005.后端工程师 (java工程师)/expense-tracker-server/src/main/java/com/xingzhewk/vo/BillStatsVO.java`

- [ ] **Step 1: 添加字段**

在 `private List<CategoryStat> categories;` 之后、`@Data public static class StatsItem` 之前，添加：

```java
    /** 支出环比（%），正数=增长，负数=下降，null=上月为0无环比 */
    private BigDecimal expenseChange;

    /** 收入环比（%），正数=增长，负数=下降，null=上月为0无环比 */
    private BigDecimal incomeChange;
```

在文件顶部确保有 `import java.math.BigDecimal;`（已有）。

- [ ] **Step 2: 编译验证**

```bash
cd "005.后端工程师 (java工程师)/expense-tracker-server"
mvn compile -q
```

预期: 无错误

- [ ] **Step 3: 提交**

```bash
git add "005.后端工程师 (java工程师)/expense-tracker-server/src/main/java/com/xingzhewk/vo/BillStatsVO.java"
git commit -m "feat(compare): BillStatsVO 新增 expenseChange/incomeChange 环比字段"
```

---

### Task 2: Java BillMapper 新增 selectSumIncome 方法

**Files:**
- Modify: `005.后端工程师 (java工程师)/expense-tracker-server/src/main/java/com/xingzhewk/mapper/BillMapper.java`

- [ ] **Step 1: 添加方法**

在 `selectSumAmountByCategory` 方法之后添加：

```java
    /**
     * 汇总某用户某段时间内的收入金额
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM bill " +
            "WHERE user_id = #{userId} AND type = 'INCOME' " +
            "AND bill_time >= #{startDate} AND bill_time <= #{endDate}")
    BigDecimal selectSumIncome(
            @Param("userId") Long userId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );
```

- [ ] **Step 2: 编译验证**

```bash
cd "005.后端工程师 (java工程师)/expense-tracker-server"
mvn compile -q
```

- [ ] **Step 3: 提交**

```bash
git add "005.后端工程师 (java工程师)/expense-tracker-server/src/main/java/com/xingzhewk/mapper/BillMapper.java"
git commit -m "feat(compare): BillMapper 新增 selectSumIncome 方法"
```

---

### Task 3: Java BillServiceImpl 增加环比计算

**Files:**
- Modify: `005.后端工程师 (java工程师)/expense-tracker-server/src/main/java/com/xingzhewk/service/impl/BillServiceImpl.java`

- [ ] **Step 1: 修改 monthlyStats() 方法**

在 `monthlyStats()` 方法中，找到 `stats.setCategories(categoryStats);` 之后、`return Result.success(stats);` 之前，添加：

```java
        // 计算上月环比
        LocalDateTime prevMonthStart = start.minusMonths(1);
        LocalDateTime prevMonthEnd = start;

        BigDecimal prevExpense = billMapper.selectSumAmount(userId,
                prevMonthStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 00:00:00",
                prevMonthEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 00:00:00");
        BigDecimal prevIncome = billMapper.selectSumIncome(userId,
                prevMonthStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 00:00:00",
                prevMonthEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 00:00:00");

        stats.setExpenseChange(calculateChange(expenseTotal, prevExpense));
        stats.setIncomeChange(calculateChange(incomeTotal, prevIncome));
```

在文件末尾（`parseBillTime` 方法之后）添加：

```java
    /** 计算环比百分比，上月为 0 时返回 null */
    private BigDecimal calculateChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
    }
```

确保 import 中有 `java.math.RoundingMode;`（已有）。

- [ ] **Step 2: 编译验证**

```bash
cd "005.后端工程师 (java工程师)/expense-tracker-server"
mvn compile -q
```

- [ ] **Step 3: 提交**

```bash
git add "005.后端工程师 (java工程师)/expense-tracker-server/src/main/java/com/xingzhewk/service/impl/BillServiceImpl.java"
git commit -m "feat(compare): 月度统计增加环比计算"
```

---

### Task 4: Node.js bills.js stats 接口增加环比

**Files:**
- Modify: `003.前端代码/backend/routes/bills.js`

- [ ] **Step 1: 修改 GET /api/bills/stats/month 接口**

读取 `routes/bills.js`，找到 stats 接口（`router.get('/stats/month', ...)` ）。

在现有的 `res.json({ code: 0, data: { ... } })` 处，修改为：

```javascript
    // 计算上月日期
    const prevMonth = month ? moment(month, 'YYYY-MM').subtract(1, 'month').format('YYYY-MM')
                             : moment().subtract(1, 'month').format('YYYY-MM');

    // 查询上月数据
    const [[{ expense: prevExpenseTotal }]] = await pool.query(
        "SELECT COALESCE(SUM(amount), 0) AS expense FROM bill WHERE user_id = ? AND type = 'EXPENSE' AND DATE_FORMAT(bill_time, '%Y-%m') = ?",
        [user.id, prevMonth]
    );
    const [[{ income: prevIncomeTotal }]] = await pool.query(
        "SELECT COALESCE(SUM(amount), 0) AS income FROM bill WHERE user_id = ? AND type = 'INCOME' AND DATE_FORMAT(bill_time, '%Y-%m') = ?",
        [user.id, prevMonth]
    );

    const calcChange = (curr, prev) => {
        const c = parseFloat(curr);
        const p = parseFloat(prev);
        return p > 0 ? parseFloat(((c - p) / p * 100).toFixed(1)) : null;
    };

    res.json({
        code: 0,
        data: {
            expense: { total: parseFloat(expense.total), count: expense.count, change: calcChange(expense.total, prevExpenseTotal) },
            income: { total: parseFloat(income.total), count: income.count, change: calcChange(income.total, prevIncomeTotal) },
            daily: dailyStats.map(function (d) { return { date: d.date, expense: parseFloat(d.expense), income: parseFloat(d.income) }; }),
            categories: categoryStats.map(function (c) { return { id: c.id, name: c.name, icon: c.icon, total: parseFloat(c.total), count: c.count }; })
        }
    });
```

在文件顶部，找到现有的 `require` 语句区域，添加 `getPrevMonth` 辅助函数（放在 stats 接口定义之前）：

```javascript
// 计算上月日期 YYYY-MM
function getPrevMonth(m) {
    var parts = m.split('-');
    var y = parseInt(parts[0]);
    var mo = parseInt(parts[1]) - 1;
    if (mo < 1) { mo = 12; y--; }
    return y + '-' + (mo < 10 ? '0' + mo : mo);
}
```

在 stats 接口函数体内，在 `const month = req.query.month;` 之后使用：

```javascript
    const prevMonth = month ? getPrevMonth(month) : getPrevMonth(new Date().getFullYear() + '-' + String(new Date().getMonth() + 1).padStart(2, '0'));
```

- [ ] **Step 2: 语法检查**

```bash
cd "003.前端代码/backend" && node -c routes/bills.js
```

- [ ] **Step 3: 提交**

```bash
git add "003.前端代码/backend/routes/bills.js"
git commit -m "feat(compare): stats 接口增加环比计算"
```

---

### Task 5: 前端 statistics.html 增加环比显示

**Files:**
- Modify: `003.前端代码/finance/pages/statistics.html`

- [ ] **Step 1: 在总支出卡片中添加环比显示容器**

在 `stat-expense-count` div 之后，添加：

```html
<div id="stat-expense-change" class="mt-1 text-body-sm"></div>
```

- [ ] **Step 2: 在收入卡片中添加环比显示容器**

在 `stat-income-count` div 之后，添加：

```html
<div id="stat-income-change" class="mt-1 text-body-sm"></div>
```

- [ ] **Step 3: 在 loadStats() 中添加环比渲染逻辑**

在 `loadStats()` 中，找到 `// 总支出` 部分（`document.getElementById('stat-expense-count')...` 之后），添加：

```javascript
                // 环比 - 支出
                var expChangeEl = document.getElementById('stat-expense-change');
                if (data.expense.change !== null && data.expense.change !== undefined) {
                    var ch = data.expense.change;
                    var arrow = ch >= 0 ? '↑' : '↓';
                    var colorClass = ch >= 0 ? 'text-danger-expense' : 'text-success-growth';
                    expChangeEl.className = 'mt-1 text-body-sm font-medium ' + colorClass;
                    expChangeEl.textContent = arrow + ' ' + Math.abs(ch) + '% 较上月';
                } else {
                    expChangeEl.textContent = '-- 较上月';
                    expChangeEl.className = 'mt-1 text-body-sm text-on-surface-variant';
                }
```

在收入部分之后添加：

```javascript
                // 环比 - 收入
                var incChangeEl = document.getElementById('stat-income-change');
                if (data.income.change !== null && data.income.change !== undefined) {
                    var ch = data.income.change;
                    var arrow = ch >= 0 ? '↑' : '↓';
                    var colorClass = ch >= 0 ? 'text-success-growth' : 'text-danger-expense';
                    incChangeEl.className = 'mt-1 text-body-sm font-medium ' + colorClass;
                    incChangeEl.textContent = arrow + ' ' + Math.abs(ch) + '% 较上月';
                } else {
                    incChangeEl.textContent = '-- 较上月';
                    incChangeEl.className = 'mt-1 text-body-sm text-on-surface-variant';
                }
```

- [ ] **Step 4: 验证 HTML**

```bash
node -e "const fs=require('fs'); const h=fs.readFileSync('003.前端代码/finance/pages/statistics.html','utf8'); console.log('Has expense-change:', h.includes('stat-expense-change')); console.log('Has income-change:', h.includes('stat-income-change')); console.log('Has </html>:', h.includes('</html>'));"
```

- [ ] **Step 5: 提交**

```bash
git add "003.前端代码/finance/pages/statistics.html"
git commit -m "feat(compare): 统计页增加环比显示"
```

---

### Task 6: 验证

- [ ] **Step 1: Java 全量测试**

```bash
cd "005.后端工程师 (java工程师)/expense-tracker-server"
mvn test -q 2>&1 | tail -10
```

- [ ] **Step 2: Java 编译打包**

```bash
cd "005.后端工程师 (java工程师)/expense-tracker-server"
mvn package -DskipTests -q
```

- [ ] **Step 3: Node.js 语法检查**

```bash
cd "003.前端代码/backend" && node -c routes/bills.js && echo "OK"
```

- [ ] **Step 4: 最终提交**

```bash
cd "d:/Java/workspace/2026/claude_my_product"
git add .
git commit -m "feat(compare): V1.6 统计页环比对比功能完整实现"
```

---

## 实施顺序

1. **Task 1-3** → Java 后端（VO → Mapper → Service）
2. **Task 4** → Node.js 后端
3. **Task 5** → 前端 UI
4. **Task 6** → 验证

每个 Task 完成后立即提交。
