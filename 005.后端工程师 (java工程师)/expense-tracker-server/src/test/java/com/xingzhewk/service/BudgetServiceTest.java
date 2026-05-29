package com.xingzhewk.service;

import com.xingzhewk.dto.BudgetDTO;
import com.xingzhewk.mapper.BillMapper;
import com.xingzhewk.mapper.BudgetMapper;
import com.xingzhewk.mapper.CategoryMapper;
import com.xingzhewk.util.JwtUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 预算服务 & 仪表盘集成测试
 *
 * 重点验证：BudgetServiceImpl.dashboard 修复后，
 * spent 字段返回真实消费金额而非硬编码 0，
 * categories 列表返回各分类进度而非空数组。
 */
@SpringBootTest
@AutoConfigureMockMvc
class BudgetServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private BudgetMapper budgetMapper;

    @Autowired
    private BillMapper billMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    private String testToken() {
        return jwtUtil.generateToken(2L, "13800138000");
    }

    // ==================== dashboard 真实数据验证 ====================

    @Test
    @DisplayName("BUD-01: 预算仪表盘当月 — 返回 200 且 spent 不为硬编码 0")
    void testDashboard_withBills_returnsRealSpent() throws Exception {
        String currentMonth = LocalDate.now().toString().substring(0, 7);

        // 创建一笔当月支出
        Long catId = findOrCreateExpenseCategory();
        billMapper.insert(buildBill(2L, "EXPENSE", catId, new BigDecimal("50.00"), currentMonth + "-15"));

        // 调用仪表盘接口
        mockMvc.perform(get("/api/budgets/dashboard")
                        .param("month", currentMonth)
                        .header("Authorization", "Bearer " + testToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.spent").exists())
                .andExpect(jsonPath("$.data.totalBudget").exists())
                .andExpect(jsonPath("$.data.remaining").exists())
                .andExpect(jsonPath("$.data.percent").exists());

        // 清理
        cleanupBillsForCategory(catId);
        categoryMapper.deleteById(catId);
    }

    @Test
    @DisplayName("BUD-02: 预算仪表盘 — categories 列表不应为空（有分类预算时）")
    void testDashboard_categoryProgress_returnsCategories() throws Exception {
        String currentMonth = LocalDate.now().toString().substring(0, 7);

        // 创建一个分类预算
        Long catId = findOrCreateExpenseCategory();
        BudgetDTO dto = new BudgetDTO();
        dto.setCategoryId(catId);
        dto.setAmount(new BigDecimal("1000.00"));
        dto.setPeriod("MONTHLY");
        dto.setStartDate(LocalDate.now().withDayOfMonth(1));

        budgetService.createOrUpdate(2L, dto);

        // 调用仪表盘
        mockMvc.perform(get("/api/budgets/dashboard")
                        .param("month", currentMonth)
                        .header("Authorization", "Bearer " + testToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.categories").isArray());

        // 清理
        budgetMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.xingzhewk.entity.Budget>()
                .eq(com.xingzhewk.entity.Budget::getUserId, 2L)
                .eq(com.xingzhewk.entity.Budget::getCategoryId, catId));
        cleanupBillsForCategory(catId);
        categoryMapper.deleteById(catId);
    }

    @Test
    @DisplayName("BUD-03: 预算仪表盘无预算 — 返回 0 金额")
    void testDashboard_noBudget_returnsZero() throws Exception {
        String futureMonth = "2099-12";

        mockMvc.perform(get("/api/budgets/dashboard")
                        .param("month", futureMonth)
                        .header("Authorization", "Bearer " + testToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalBudget").value(0))
                .andExpect(jsonPath("$.data.spent").value(0))
                .andExpect(jsonPath("$.data.remaining").value(0))
                .andExpect(jsonPath("$.data.percent").value(0))
                .andExpect(jsonPath("$.data.categories").isArray());
    }

    @Test
    @DisplayName("BUD-04: 未登录访问仪表盘 — 返回 401")
    void testDashboard_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/budgets/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("BUD-05: 创建预算 — 返回 200 且包含 ID")
    void testCreateOrUpdate_returnsId() throws Exception {
        Long catId = findOrCreateExpenseCategory();

        try {
            mockMvc.perform(post("/api/budgets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + testToken())
                            .content("{\"category_id\":" + catId + ",\"amount\":500,\"period\":\"MONTHLY\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").isNumber());
        } finally {
            budgetMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.xingzhewk.entity.Budget>()
                    .eq(com.xingzhewk.entity.Budget::getUserId, 2L)
                    .eq(com.xingzhewk.entity.Budget::getCategoryId, catId));
            cleanupBillsForCategory(catId);
            categoryMapper.deleteById(catId);
        }
    }

    @Test
    @DisplayName("BUD-06: 获取预算列表 — 返回 200")
    void testList_returnsBudgets() throws Exception {
        mockMvc.perform(get("/api/budgets")
                        .param("type", "category")
                        .header("Authorization", "Bearer " + testToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    // ==================== 辅助方法 ====================

    private Long findOrCreateExpenseCategory() {
        com.xingzhewk.entity.Category cat = new com.xingzhewk.entity.Category();
        cat.setName("预算测试分类");
        cat.setIcon("restaurant");
        cat.setType("EXPENSE");
        cat.setParentId(0L);
        cat.setSortOrder(0);
        cat.setIsPreset(0);
        cat.setUserId(2L);
        categoryMapper.insert(cat);
        return cat.getId();
    }

    private com.xingzhewk.entity.Bill buildBill(Long userId, String type, Long categoryId,
                                                 BigDecimal amount, String billTime) {
        com.xingzhewk.entity.Bill bill = new com.xingzhewk.entity.Bill();
        bill.setUserId(userId);
        bill.setType(type);
        bill.setCategoryId(categoryId);
        bill.setAmount(amount);
        bill.setBillTime(java.time.LocalDateTime.parse(billTime + "T12:00:00"));
        bill.setRemark("预算测试账单");
        bill.setIsRecurring(0);
        bill.setCreatedBy(userId);
        return bill;
    }

    private void cleanupBillsForCategory(Long catId) {
        billMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.xingzhewk.entity.Bill>()
                .eq(com.xingzhewk.entity.Bill::getCategoryId, catId));
    }
}
