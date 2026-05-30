package com.xingzhewk.service;

import com.xingzhewk.common.Result;
import com.xingzhewk.dto.BillDTO;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 账单标签关联测试
 */
@SpringBootTest
@AutoConfigureMockMvc
class BillServiceTagTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BillService billService;

    @Autowired
    private BillMapper billMapper;

    @Autowired
    private BillTagMapper billTagMapper;

    @Autowired
    private BillTagRelMapper billTagRelMapper;

    private String testToken() {
        return jwtUtil.generateToken(2L, "13800138000");
    }

    private Long createTag(String name) {
        BillTag tag = new BillTag();
        tag.setUserId(2L);
        tag.setName(name);
        billTagMapper.insert(tag);
        return tag.getId();
    }

    @AfterEach
    void cleanup() {
        billTagRelMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BillTagRel>()
        );
        billMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Bill>()
                        .eq(Bill::getUserId, 2L)
        );
        billTagMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BillTag>()
                        .eq(BillTag::getUserId, 2L)
        );
    }

    @Test
    @DisplayName("BILL-TAG-01: 创建账单并关联标签")
    void testCreateBill_withTags() {
        Long tag1 = createTag("必要");
        Long tag2 = createTag("报销");

        BillDTO dto = new BillDTO();
        dto.setType("EXPENSE");
        dto.setAmount(new BigDecimal("100.00"));
        dto.setCategoryId(1L);
        dto.setRemark("测试");
        dto.setBillTime(LocalDateTime.now().toString());
        dto.setTagIds(Arrays.asList(tag1, tag2));

        Result<?> result = billService.create(2L, dto);
        Assertions.assertEquals(0, result.getCode());
        Long billId = ((Number) ((Map<?, ?>) result.getData()).get("id")).longValue();

        long count = billTagRelMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BillTagRel>()
                        .eq(BillTagRel::getBillId, billId)
        );
        Assertions.assertEquals(2, count);
    }

    @Test
    @DisplayName("BILL-TAG-02: 创建账单时超过 10 个标签 — 应报 400")
    void testCreateBill_tooManyTags_rejected() {
        Long[] tagIds = new Long[11];
        for (int i = 0; i < 11; i++) {
            tagIds[i] = createTag("标签" + i);
        }

        BillDTO dto = new BillDTO();
        dto.setType("EXPENSE");
        dto.setAmount(new BigDecimal("10.00"));
        dto.setCategoryId(1L);
        dto.setRemark("测试");
        dto.setTagIds(Arrays.asList(tagIds));

        Assertions.assertThrows(com.xingzhewk.common.exception.BusinessException.class,
                () -> billService.create(2L, dto));
    }

    @Test
    @DisplayName("BILL-TAG-03: 创建账单使用不存在的标签 — 应报 400")
    void testCreateBill_nonexistentTag_rejected() {
        BillDTO dto = new BillDTO();
        dto.setType("EXPENSE");
        dto.setAmount(new BigDecimal("10.00"));
        dto.setCategoryId(1L);
        dto.setRemark("测试");
        dto.setTagIds(Arrays.asList(999999L));

        Assertions.assertThrows(com.xingzhewk.common.exception.BusinessException.class,
                () -> billService.create(2L, dto));
    }

    @Test
    @DisplayName("BILL-TAG-04: 更新账单 tag_ids 为空数组 — 清空关联")
    void testUpdateBill_clearTags() {
        Long tag1 = createTag("清空测试");
        Long billId = createBillWithTags(2L, tag1);

        BillDTO dto = new BillDTO();
        dto.setTagIds(Arrays.asList());
        Result<Void> result = billService.update(2L, billId, dto);
        Assertions.assertEquals(0, result.getCode());

        long count = billTagRelMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BillTagRel>()
                        .eq(BillTagRel::getBillId, billId)
        );
        Assertions.assertEquals(0, count);
    }

    @Test
    @DisplayName("BILL-TAG-05: 更新账单不传 tag_ids — 关联不变")
    void testUpdateBill_noTagIds_unchanged() {
        Long tag1 = createTag("不变测试");
        Long billId = createBillWithTags(2L, tag1);

        BillDTO dto = new BillDTO();
        dto.setRemark("只改备注");
        Result<Void> result = billService.update(2L, billId, dto);
        Assertions.assertEquals(0, result.getCode());

        long count = billTagRelMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BillTagRel>()
                        .eq(BillTagRel::getBillId, billId)
        );
        Assertions.assertEquals(1, count);
    }

    @Test
    @DisplayName("BILL-TAG-06: 更新账单 tag_ids 全量替换")
    void testUpdateBill_replaceTags() {
        Long tag1 = createTag("旧标签1");
        Long tag2 = createTag("新标签");
        Long billId = createBillWithTags(2L, tag1);

        BillDTO dto = new BillDTO();
        dto.setTagIds(Arrays.asList(tag2));
        Result<Void> result = billService.update(2L, billId, dto);
        Assertions.assertEquals(0, result.getCode());

        long count = billTagRelMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BillTagRel>()
                        .eq(BillTagRel::getBillId, billId)
                        .eq(BillTagRel::getTagId, tag1)
        );
        Assertions.assertEquals(0, count);

        count = billTagRelMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BillTagRel>()
                        .eq(BillTagRel::getBillId, billId)
                        .eq(BillTagRel::getTagId, tag2)
        );
        Assertions.assertEquals(1, count);
    }

    @Test
    @DisplayName("BILL-TAG-07: 删除账单 — 关联记录同步删除")
    void testDeleteBill_cascadeRels() {
        Long tag1 = createTag("级联测试");
        Long billId = createBillWithTags(2L, tag1);

        billService.delete(2L, billId);

        long count = billTagRelMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BillTagRel>()
                        .eq(BillTagRel::getBillId, billId)
        );
        Assertions.assertEquals(0, count);
    }

    @Test
    @DisplayName("BILL-TAG-08: 账单列表返回 tags 字段")
    void testListBill_returnsTags() {
        Long tag1 = createTag("列表测试");
        createBillWithTags(2L, tag1);

        Result<?> result = billService.list(2L, null, null, null, null, null, 1, 10);
        Assertions.assertEquals(0, result.getCode());

        Map<?, ?> data = (Map<?, ?>) result.getData();
        List<?> list = (List<?>) data.get("list");
        Assertions.assertFalse(list.isEmpty());

        Map<?, ?> firstBill = (Map<?, ?>) list.get(0);
        Assertions.assertNotNull(firstBill.get("tags"));
    }

    @Test
    @DisplayName("BILL-TAG-09: 按 tag_id 筛选账单")
    void testListBill_filterByTagId() {
        Long tag1 = createTag("筛选A");
        Long tag2 = createTag("筛选B");
        Long bill1 = createBillWithTags(2L, tag1);
        createBillWithTags(2L, tag2);

        Result<?> result = billService.list(2L, null, null, null, null, tag1, 1, 10);
        Assertions.assertEquals(0, result.getCode());

        Map<?, ?> data = (Map<?, ?>) result.getData();
        List<?> list = (List<?>) data.get("list");
        Assertions.assertEquals(1, list.size());

        Map<?, ?> returnedBill = (Map<?, ?>) list.get(0);
        Assertions.assertEquals(bill1, returnedBill.get("id"));
    }

    @Test
    @DisplayName("BILL-TAG-10: POST /api/bills 带 tag_ids — 成功")
    void testCreateHttp_withTags() throws Exception {
        Long tagId = createTag("HTTP标签");

        mockMvc.perform(post("/api/bills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + testToken())
                        .content("{\"type\":\"EXPENSE\",\"amount\":50,\"category_id\":1,\"remark\":\"http测试\",\"tag_ids\":[" + tagId + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private Long createBillWithTags(Long userId, Long... tagIds) {
        Bill bill = new Bill();
        bill.setUserId(userId);
        bill.setType("EXPENSE");
        bill.setCategoryId(1L);
        bill.setAmount(new BigDecimal("25.00"));
        bill.setBillTime(LocalDateTime.now());
        bill.setRemark("辅助账单");
        bill.setIsRecurring(0);
        bill.setCreatedBy(userId);
        billMapper.insert(bill);

        for (Long tagId : tagIds) {
            BillTagRel rel = new BillTagRel();
            rel.setBillId(bill.getId());
            rel.setTagId(tagId);
            billTagRelMapper.insert(rel);
        }

        return bill.getId();
    }
}
