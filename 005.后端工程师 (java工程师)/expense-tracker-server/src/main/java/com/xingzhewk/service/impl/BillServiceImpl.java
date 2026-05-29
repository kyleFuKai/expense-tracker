package com.xingzhewk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xingzhewk.common.Result;
import com.xingzhewk.dto.BillDTO;
import com.xingzhewk.entity.Bill;
import com.xingzhewk.entity.Category;
import com.xingzhewk.common.exception.BusinessException;
import com.xingzhewk.mapper.BillMapper;
import com.xingzhewk.mapper.CategoryMapper;
import com.xingzhewk.service.BillService;
import com.xingzhewk.vo.BillStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillServiceImpl implements BillService {

    private static final DateTimeFormatter BILL_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BillMapper billMapper;
    private final CategoryMapper categoryMapper;

    @Override
    public Result<?> list(Long userId, String month, Long categoryId, String type, String keyword, int page, int pageSize) {
        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bill::getUserId, userId);

        if (StringUtils.hasText(month)) {
            LocalDateTime start = LocalDateTime.parse(month + "-01T00:00:00");
            LocalDateTime end = start.plusMonths(1);
            wrapper.between(Bill::getBillTime, start, end);
        }
        if (categoryId != null) {
            wrapper.eq(Bill::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq(Bill::getType, type.toUpperCase());
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Bill::getRemark, keyword);
        }

        wrapper.orderByDesc(Bill::getBillTime);

        Page<Bill> pageParam = new Page<>(page, pageSize);
        Page<Bill> result = billMapper.selectPage(pageParam, wrapper);

        // Attach category info
        List<Map<String, Object>> list = result.getRecords().stream().map(bill -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", bill.getId());
            map.put("user_id", bill.getUserId());
            map.put("type", bill.getType());
            map.put("amount", bill.getAmount());
            map.put("category_id", bill.getCategoryId());
            map.put("remark", bill.getRemark());
            map.put("bill_time", bill.getBillTime());
            map.put("is_recurring", bill.getIsRecurring());
            map.put("created_at", bill.getCreatedAt());
            map.put("updated_at", bill.getUpdatedAt());

            Category cat = categoryMapper.selectById(bill.getCategoryId());
            map.put("category_name", cat != null ? cat.getName() : null);
            map.put("category_icon", cat != null ? cat.getIcon() : null);
            return map;
        }).collect(Collectors.toList());

        Page<Map<String, Object>> voPage = new Page<>(page, pageSize);
        voPage.setRecords(list);
        voPage.setTotal(result.getTotal());
        Map<String, Object> result2 = new LinkedHashMap<>();
        result2.put("list", list);
        result2.put("total", result.getTotal());
        result2.put("page", page);
        result2.put("pageSize", pageSize);
        return Result.success(result2);
    }

    @Override
    public Result<Object> getById(Long userId, Long id) {
        Bill bill = billMapper.selectOne(new LambdaQueryWrapper<Bill>()
                .eq(Bill::getId, id).eq(Bill::getUserId, userId));
        if (bill == null) {
            throw new BusinessException(404, "账单不存在");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", bill.getId());
        map.put("type", bill.getType());
        map.put("amount", bill.getAmount());
        map.put("category_id", bill.getCategoryId());
        map.put("remark", bill.getRemark());
        map.put("bill_time", bill.getBillTime());
        map.put("created_at", bill.getCreatedAt());
        map.put("updated_at", bill.getUpdatedAt());
        return Result.success(map);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> create(Long userId, BillDTO dto) {
        if (dto.getType() == null || dto.getAmount() == null || dto.getCategoryId() == null) {
            throw new BusinessException(400, "类型、金额、分类不能为空");
        }
        if (!"EXPENSE".equalsIgnoreCase(dto.getType()) && !"INCOME".equalsIgnoreCase(dto.getType())) {
            throw new BusinessException(400, "类型不合法");
        }
        if (dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "金额必须大于 0");
        }
        if (dto.getRemark() != null && dto.getRemark().length() > 200) {
            throw new BusinessException(400, "备注不能超过 200 个字符");
        }

        Bill bill = new Bill();
        bill.setUserId(userId);
        bill.setType(dto.getType().toUpperCase());
        bill.setAmount(dto.getAmount());
        bill.setCategoryId(dto.getCategoryId());
        bill.setRemark(dto.getRemark() != null ? dto.getRemark() : "");
        bill.setBillTime(parseBillTime(dto.getBillTime()));
        bill.setCreatedBy(0L);
        bill.setIsRecurring(0);

        billMapper.insert(bill);
        log.info("创建账单, billId={}, amount={}", bill.getId(), bill.getAmount());
        Map<String, Object> createResult = new LinkedHashMap<>();
        createResult.put("id", bill.getId());
        return Result.success(createResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(Long userId, Long id, BillDTO dto) {
        if (dto.getRemark() != null && dto.getRemark().length() > 200) {
            throw new BusinessException(400, "备注不能超过 200 个字符");
        }

        Bill existing = billMapper.selectOne(new LambdaQueryWrapper<Bill>()
                .eq(Bill::getId, id).eq(Bill::getUserId, userId));
        if (existing == null) {
            throw new BusinessException(404, "账单不存在");
        }

        Bill update = new Bill();
        update.setId(id);
        if (dto.getType() != null) {
            if (!"EXPENSE".equalsIgnoreCase(dto.getType()) && !"INCOME".equalsIgnoreCase(dto.getType())) {
                throw new BusinessException(400, "类型不合法");
            }
            update.setType(dto.getType().toUpperCase());
        }
        if (dto.getAmount() != null) {
            if (dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(400, "金额必须大于 0");
            }
            update.setAmount(dto.getAmount());
        }
        if (dto.getCategoryId() != null) update.setCategoryId(dto.getCategoryId());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());
        if (dto.getBillTime() != null) update.setBillTime(parseBillTime(dto.getBillTime()));

        billMapper.updateById(update);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long userId, Long id) {
        Bill existing = billMapper.selectOne(new LambdaQueryWrapper<Bill>()
                .eq(Bill::getId, id).eq(Bill::getUserId, userId));
        if (existing == null) {
            throw new BusinessException(404, "账单不存在");
        }
        billMapper.deleteById(id);
        return Result.success();
    }

    @Override
    public Result<BillStatsVO> monthlyStats(Long userId, String month) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        LocalDateTime start, end;
        if (month != null) {
            start = LocalDateTime.parse(month + "-01T00:00:00");
            end = start.plusMonths(1);
        } else {
            start = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            end = start.plusMonths(1);
        }

        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<Bill>()
                .eq(Bill::getUserId, userId)
                .between(Bill::getBillTime, start, end);

        List<Bill> bills = billMapper.selectList(wrapper);

        long expenseCount = bills.stream().filter(b -> "EXPENSE".equals(b.getType())).count();
        long incomeCount = bills.stream().filter(b -> "INCOME".equals(b.getType())).count();
        BigDecimal expenseTotal = bills.stream().filter(b -> "EXPENSE".equals(b.getType()))
                .map(Bill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal incomeTotal = bills.stream().filter(b -> "INCOME".equals(b.getType()))
                .map(Bill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Daily stats
        Map<String, Map<String, BigDecimal>> dailyMap = new LinkedHashMap<>();
        for (Bill b : bills) {
            String dateKey = b.getBillTime().toLocalDate().toString();
            dailyMap.computeIfAbsent(dateKey, k -> new HashMap<>());
            Map<String, BigDecimal> daily = dailyMap.get(dateKey);
            if ("EXPENSE".equals(b.getType())) {
                daily.merge("expense", b.getAmount(), BigDecimal::add);
            } else {
                daily.merge("income", b.getAmount(), BigDecimal::add);
            }
        }
        List<BillStatsVO.DailyStat> dailyStats = dailyMap.entrySet().stream().map(e -> {
            BillStatsVO.DailyStat ds = new BillStatsVO.DailyStat();
            ds.setDate(e.getKey());
            ds.setExpense(e.getValue().getOrDefault("expense", BigDecimal.ZERO));
            ds.setIncome(e.getValue().getOrDefault("income", BigDecimal.ZERO));
            return ds;
        }).sorted(Comparator.comparing(BillStatsVO.DailyStat::getDate).reversed()).limit(30).collect(Collectors.toList());

        // Category stats
        Map<Long, Map<String, Object>> catMap = new LinkedHashMap<>();
        for (Bill b : bills) {
            if (!"EXPENSE".equals(b.getType())) continue;
            Map<String, Object> cs = catMap.computeIfAbsent(b.getCategoryId(), k -> new HashMap<>());
            cs.merge("total", b.getAmount(), (a, v) -> ((BigDecimal) a).add((BigDecimal) v));
            cs.merge("count", 1L, (a, v) -> (Long) a + (Long) v);
        }
        List<BillStatsVO.CategoryStat> categoryStats = catMap.entrySet().stream().map(e -> {
            BillStatsVO.CategoryStat cs = new BillStatsVO.CategoryStat();
            cs.setId(e.getKey());
            cs.setTotal((BigDecimal) e.getValue().get("total"));
            cs.setCount((Long) e.getValue().get("count"));
            return cs;
        }).sorted(Comparator.comparing(BillStatsVO.CategoryStat::getTotal).reversed()).collect(Collectors.toList());

        BillStatsVO stats = new BillStatsVO();
        BillStatsVO.StatsItem expense = new BillStatsVO.StatsItem();
        expense.setTotal(expenseTotal); expense.setCount(expenseCount);
        BillStatsVO.StatsItem income = new BillStatsVO.StatsItem();
        income.setTotal(incomeTotal); income.setCount(incomeCount);
        stats.setExpense(expense);
        stats.setIncome(income);
        stats.setDaily(dailyStats);
        stats.setCategories(categoryStats);
        return Result.success(stats);
    }

    /** 解析前端传入的账单时间，支持 "yyyy-MM-dd HH:mm:ss" 和 "yyyy-MM-ddTHH:mm:ss" */
    private LocalDateTime parseBillTime(String billTime) {
        if (billTime == null || billTime.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(billTime.trim(), BILL_TIME_FORMATTER);
        } catch (Exception e) {
            log.warn("账单时间解析失败，使用当前时间: {}", billTime);
            return LocalDateTime.now();
        }
    }
}
