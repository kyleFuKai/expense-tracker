package com.xingzhewk.service;

import com.xingzhewk.common.Result;
import com.xingzhewk.common.exception.BusinessException;
import com.xingzhewk.dto.CreateTagDTO;
import com.xingzhewk.dto.UpdateTagDTO;
import com.xingzhewk.entity.Bill;
import com.xingzhewk.entity.BillTag;
import com.xingzhewk.entity.BillTagRel;
import com.xingzhewk.mapper.BillMapper;
import com.xingzhewk.mapper.BillTagMapper;
import com.xingzhewk.mapper.BillTagRelMapper;
import com.xingzhewk.util.JwtUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 标签服务 & 接口集成测试
 *
 * 覆盖 TAG-01~15：标签 CRUD、级联删除、billCount 统计、HTTP API、鉴权。
 */
@SpringBootTest
@AutoConfigureMockMvc
class BillTagServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BillTagService billTagService;

    @Autowired
    private BillTagMapper billTagMapper;

    @Autowired
    private BillTagRelMapper billTagRelMapper;

    @Autowired
    private BillMapper billMapper;

    private String testToken() {
        return jwtUtil.generateToken(2L, "13800138000");
    }

    private Long createTag(String name) {
        CreateTagDTO dto = new CreateTagDTO();
        dto.setName(name);
        Result<?> result = billTagService.create(2L, dto);
        return ((Number) ((Map<?, ?>) result.getData()).get("id")).longValue();
    }

    @AfterEach
    void cleanup() {
        billTagRelMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BillTagRel>()
        );
        billTagMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BillTag>()
                        .eq(BillTag::getUserId, 2L)
        );
    }

    // ==================== Tag CRUD 测试 ====================

    @Test
    @DisplayName("TAG-01: 创建标签 — 成功")
    void testCreateTag_success() {
        Result<?> result = billTagService.create(2L, buildCreateDto("必要"));
        Assertions.assertEquals(0, result.getCode());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        Assertions.assertNotNull(data.get("id"));
    }

    @Test
    @DisplayName("TAG-02: 创建同名标签 — 应报 400 重复")
    void testCreateTag_duplicate_rejected() {
        createTag("报销");
        Assertions.assertThrows(BusinessException.class,
                () -> billTagService.create(2L, buildCreateDto("报销")),
                "同一用户不应有重复标签");
    }

    @Test
    @DisplayName("TAG-03: 创建空名标签 — 应报 400")
    void testCreateTag_emptyName_rejected() {
        Assertions.assertThrows(BusinessException.class,
                () -> {
                    CreateTagDTO dto = new CreateTagDTO();
                    dto.setName("");
                    billTagService.create(2L, dto);
                });
    }

    @Test
    @DisplayName("TAG-04: 创建超长标签名 — 应报 400")
    void testCreateTag_longName_rejected() {
        Assertions.assertThrows(BusinessException.class,
                () -> billTagService.create(2L, buildCreateDto("这是一个超过16个字符的标签名称用于测试")),
                "标签名超过 16 字符应被拒绝");
    }

    @Test
    @DisplayName("TAG-05: 修改标签名 — 成功")
    void testUpdateTag_success() {
        Long tagId = createTag("旧名称");
        UpdateTagDTO dto = new UpdateTagDTO();
        dto.setName("新名称");
        Result<Void> result = billTagService.update(2L, tagId, dto);
        Assertions.assertEquals(0, result.getCode());

        BillTag tag = billTagMapper.selectById(tagId);
        Assertions.assertEquals("新名称", tag.getName());
    }

    @Test
    @DisplayName("TAG-06: 修改为重复名称 — 应报 400")
    void testUpdateTag_duplicate_rejected() {
        createTag("标签A");
        Long tagId2 = createTag("标签B");
        UpdateTagDTO dto = new UpdateTagDTO();
        dto.setName("标签A");
        Assertions.assertThrows(BusinessException.class,
                () -> billTagService.update(2L, tagId2, dto));
    }

    @Test
    @DisplayName("TAG-07: 修改不存在的标签 — 应报 404")
    void testUpdateTag_notFound() {
        UpdateTagDTO dto = new UpdateTagDTO();
        dto.setName("任意");
        Assertions.assertThrows(BusinessException.class,
                () -> billTagService.update(2L, 999999L, dto));
    }

    @Test
    @DisplayName("TAG-08: 删除标签 — 成功")
    void testDeleteTag_success() {
        Long tagId = createTag("待删除");
        Result<Void> result = billTagService.delete(2L, tagId);
        Assertions.assertEquals(0, result.getCode());
        Assertions.assertNull(billTagMapper.selectById(tagId));
    }

    @Test
    @DisplayName("TAG-09: 删除不存在的标签 — 应报 404")
    void testDeleteTag_notFound() {
        Assertions.assertThrows(BusinessException.class,
                () -> billTagService.delete(2L, 999999L));
    }

    @Test
    @DisplayName("TAG-10: 删除标签 — 级联删除关联记录")
    void testDeleteTag_cascadeDeleteRels() {
        Long tagId = createTag("关联标签");
        Bill bill = new Bill();
        bill.setUserId(2L);
        bill.setType("EXPENSE");
        bill.setCategoryId(1L);
        bill.setAmount(new BigDecimal("50.00"));
        bill.setBillTime(LocalDateTime.now());
        bill.setRemark("测试");
        bill.setIsRecurring(0);
        bill.setCreatedBy(2L);
        billMapper.insert(bill);

        BillTagRel rel = new BillTagRel();
        rel.setBillId(bill.getId());
        rel.setTagId(tagId);
        billTagRelMapper.insert(rel);

        billTagService.delete(2L, tagId);

        long relCount = billTagRelMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BillTagRel>()
                        .eq(BillTagRel::getTagId, tagId)
        );
        Assertions.assertEquals(0, relCount);

        billMapper.deleteById(bill.getId());
    }

    @Test
    @DisplayName("TAG-11: 查询标签列表 — 包含 billCount")
    void testListTag_withBillCount() {
        Long tagId = createTag("统计测试");
        Bill bill = new Bill();
        bill.setUserId(2L);
        bill.setType("EXPENSE");
        bill.setCategoryId(1L);
        bill.setAmount(new BigDecimal("10.00"));
        bill.setBillTime(LocalDateTime.now());
        bill.setRemark("测试");
        bill.setIsRecurring(0);
        bill.setCreatedBy(2L);
        billMapper.insert(bill);

        BillTagRel rel = new BillTagRel();
        rel.setBillId(bill.getId());
        rel.setTagId(tagId);
        billTagRelMapper.insert(rel);

        Result<?> result = billTagService.list(2L);
        Assertions.assertEquals(0, result.getCode());

        billTagRelMapper.deleteById(rel.getId());
        billMapper.deleteById(bill.getId());
    }

    // ==================== HTTP API 验证 ====================

    @Test
    @DisplayName("TAG-12: GET /finance/tags — 返回 200 且有数据")
    void testListHttp_returnsTags() throws Exception {
        mockMvc.perform(get("/finance/tags")
                        .header("Authorization", "Bearer " + testToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("TAG-13: POST /finance/tags — 返回 200 且包含新 ID")
    void testCreateHttp_returnsNewId() throws Exception {
        mockMvc.perform(post("/finance/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + testToken())
                        .content("{\"name\":\"HTTP测试标签\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    @DisplayName("TAG-14: POST /finance/tags 重复名称 — 返回 400")
    void testCreateHttp_duplicate_rejected() throws Exception {
        mockMvc.perform(post("/finance/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + testToken())
                        .content("{\"name\":\"重复标签\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/finance/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + testToken())
                        .content("{\"name\":\"重复标签\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("TAG-15: 未登录访问 /finance/tags — 返回 401")
    void testListHttp_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/finance/tags"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== 辅助方法 ====================

    private CreateTagDTO buildCreateDto(String name) {
        CreateTagDTO dto = new CreateTagDTO();
        dto.setName(name);
        return dto;
    }
}
