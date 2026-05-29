package com.xingzhewk.service;

import com.xingzhewk.common.Result;
import com.xingzhewk.common.exception.BusinessException;
import com.xingzhewk.dto.CategoryDTO;
import com.xingzhewk.entity.Bill;
import com.xingzhewk.entity.Category;
import com.xingzhewk.mapper.BillMapper;
import com.xingzhewk.mapper.CategoryMapper;
import com.xingzhewk.util.JwtUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 分类服务 & 接口集成测试
 *
 * 重点验证：CategoryServiceImpl.delete 修复后，
 * 有账单引用的分类应归档（不可删除），无账单的分类可正常删除。
 */
@SpringBootTest
@AutoConfigureMockMvc
class CategoryServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private BillMapper billMapper;

    private String testToken() {
        return jwtUtil.generateToken(2L, "13800138000");
    }

    // ==================== CategoryService.delete 修复验证 ====================

    @Test
    @DisplayName("CAT-01: 删除有账单引用的分类 — 应归档而非删除")
    void testDelete_withBills_archivesCategory() {
        // 先创建一个分类
        Result<?> createResult = categoryService.create(2L, buildDto("餐饮-测试"));
        Long catIdLong = ((Number) ((java.util.Map<?, ?>) createResult.getData()).get("id")).longValue();

        // 直接用 Mapper 创建账单（避免 HTTP 层 DTO 字段名差异）
        try {
            Bill bill = new Bill();
            bill.setUserId(2L);
            bill.setType("EXPENSE");
            bill.setCategoryId(catIdLong);
            bill.setAmount(new BigDecimal("100.00"));
            bill.setBillTime(LocalDateTime.now());
            bill.setRemark("测试账单");
            bill.setIsRecurring(0);
            bill.setCreatedBy(2L);
            billMapper.insert(bill);

            // 删除分类
            Result<Void> deleteResult = categoryService.delete(2L, catIdLong);
            // 应成功（归档）
            Assertions.assertEquals(0, deleteResult.getCode());

            // 验证分类仍存在（只是归档了），未被真正删除
            Category cat = categoryMapper.selectById(catIdLong);
            Assertions.assertNotNull(cat, "分类应仍存在（已归档）");
            Assertions.assertEquals(1, cat.getIsArchived(), "分类应被归档");
        } finally {
            // 清理测试数据
            billMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Bill>()
                    .eq(Bill::getCategoryId, catIdLong));
            categoryMapper.deleteById(catIdLong);
        }
    }

    @Test
    @DisplayName("CAT-02: 删除无账单引用的分类 — 应真正删除")
    void testDelete_withoutBills_deletesCategory() {
        Result<?> createResult = categoryService.create(2L, buildDto("无用分类"));
        Long catId = ((Number) ((java.util.Map<?, ?>) createResult.getData()).get("id")).longValue();

        // 确认无账单关联
        long billCount = billMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Bill>()
                .eq(Bill::getCategoryId, catId));
        Assertions.assertEquals(0, billCount);

        // 删除分类
        Result<Void> deleteResult = categoryService.delete(2L, catId);
        Assertions.assertEquals(0, deleteResult.getCode());

        // 验证分类已被真正删除
        Category cat = categoryMapper.selectById(catId);
        Assertions.assertNull(cat, "分类应已被删除");
    }

    @Test
    @DisplayName("CAT-03: 删除系统预设分类 — 应抛出 BusinessException")
    void testDelete_presetCategory_rejected() {
        // 查询一个系统预设分类（userId = 0）
        Category preset = categoryMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Category>()
                        .eq(Category::getUserId, 0).last("LIMIT 1"));
        if (preset != null) {
            Assertions.assertThrows(BusinessException.class,
                    () -> categoryService.delete(2L, preset.getId()),
                    "系统预设分类不可删除");
        }
    }

    @Test
    @DisplayName("CAT-04: 删除不属于自己的分类 — 应抛出 BusinessException")
    void testDelete_otherUserCategory_rejected() {
        // 先用自己的账号创建一个分类
        Result<?> createResult = categoryService.create(2L, buildDto("我的分类"));
        Long catId = ((Number) ((java.util.Map<?, ?>) createResult.getData()).get("id")).longValue();

        try {
            // 用另一个 userId 尝试删除
            Assertions.assertThrows(BusinessException.class,
                    () -> categoryService.delete(999999L, catId),
                    "不应能删除其他用户的分类");
        } finally {
            categoryMapper.deleteById(catId);
        }
    }

    // ==================== HTTP API 验证 ====================

    @Test
    @DisplayName("CAT-05: 获取分类列表 — 返回 200 且有数据")
    void testList_returnsCategories() throws Exception {
        mockMvc.perform(get("/api/categories")
                        .param("type", "EXPENSE")
                        .header("Authorization", "Bearer " + testToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("CAT-06: 创建分类 — 返回 200 且包含新 ID")
    void testCreate_returnsNewId() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + testToken())
                        .content("{\"name\":\"API测试分类\",\"icon\":\"restaurant\",\"type\":\"EXPENSE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    @DisplayName("CAT-07: 未登录访问分类接口 — 返回 401")
    void testList_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== 辅助方法 ====================

    private CategoryDTO buildDto(String name) {
        CategoryDTO dto = new CategoryDTO();
        dto.setName(name);
        dto.setIcon("more_horiz");
        dto.setType("EXPENSE");
        dto.setParentId(0L);
        dto.setSortOrder(0);
        return dto;
    }
}
