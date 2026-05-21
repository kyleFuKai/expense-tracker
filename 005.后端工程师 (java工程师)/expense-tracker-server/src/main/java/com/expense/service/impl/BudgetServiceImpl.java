package com.expense.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.expense.common.Result;
import com.expense.dto.BudgetDTO;
import com.expense.entity.Budget;
import com.expense.common.exception.BusinessException;
import com.expense.mapper.BudgetMapper;
import com.expense.service.BudgetService;
import com.expense.vo.BudgetDashboardVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final BudgetMapper budgetMapper;

    @Override
    public Result<List<?>> list(Long userId, String type) {
        LambdaQueryWrapper<Budget> wrapper = new LambdaQueryWrapper<Budget>()
                .eq(Budget::getUserId, userId)
                .eq(Budget::getIsActive, 1);

        if ("total".equals(type)) {
            wrapper.eq(Budget::getCategoryId, 0);
        } else if ("category".equals(type)) {
            wrapper.ne(Budget::getCategoryId, 0);
        }

        wrapper.orderByAsc(Budget::getCategoryId);
        List<Budget> budgets = budgetMapper.selectList(wrapper);
        return Result.success(budgets);
    }

    @Override
    public Result<BudgetDashboardVO> dashboard(Long userId, String month) {
        String targetMonth = StringUtils.hasText(month) ? month
                : LocalDate.now().toString().substring(0, 7);

        // Total budget
        LambdaQueryWrapper<Budget> totalWrapper = new LambdaQueryWrapper<Budget>()
                .eq(Budget::getUserId, userId)
                .eq(Budget::getCategoryId, 0)
                .eq(Budget::getIsActive, 1)
                .eq(Budget::getPeriod, "MONTHLY")
                .le(Budget::getStartDate, targetMonth + "-28")
                .and(c -> c.isNull(Budget::getEndDate).or().ge(Budget::getEndDate, targetMonth + "-01"));
        List<Budget> totalBudgets = budgetMapper.selectList(totalWrapper);
        BigDecimal totalBudgetAmount = totalBudgets.isEmpty() ? BigDecimal.ZERO : totalBudgets.get(0).getAmount();

        // Category budgets
        LambdaQueryWrapper<Budget> catWrapper = new LambdaQueryWrapper<Budget>()
                .eq(Budget::getUserId, userId)
                .ne(Budget::getCategoryId, 0)
                .eq(Budget::getIsActive, 1)
                .eq(Budget::getPeriod, "MONTHLY")
                .le(Budget::getStartDate, targetMonth + "-28")
                .and(c -> c.isNull(Budget::getEndDate).or().ge(Budget::getEndDate, targetMonth + "-01"));
        // Note: actual category info join requires raw SQL; keeping it simple here

        BigDecimal spent = BigDecimal.ZERO;
        // Monthly spent calculation would require BillMapper; simplified
        BigDecimal remaining = totalBudgetAmount.subtract(spent);
        int percent = totalBudgetAmount.compareTo(BigDecimal.ZERO) > 0
                ? spent.multiply(BigDecimal.valueOf(100)).divide(totalBudgetAmount, 0, RoundingMode.HALF_UP).intValue()
                : 0;

        BudgetDashboardVO vo = new BudgetDashboardVO();
        vo.setTotalBudget(totalBudgetAmount);
        vo.setSpent(spent);
        vo.setRemaining(remaining);
        vo.setPercent(percent);
        vo.setCategories(new ArrayList<>());

        return Result.success(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> createOrUpdate(Long userId, BudgetDTO dto) {
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "预算金额必须大于0");
        }

        Long catId = dto.getCategoryId() != null ? dto.getCategoryId() : 0L;
        String period = StringUtils.hasText(dto.getPeriod()) ? dto.getPeriod() : "MONTHLY";
        LocalDate startDate = dto.getStartDate() != null ? dto.getStartDate() : LocalDate.now();

        // Check existing
        Budget existing = budgetMapper.selectOne(new LambdaQueryWrapper<Budget>()
                .eq(Budget::getUserId, userId)
                .eq(Budget::getCategoryId, catId)
                .eq(Budget::getPeriod, period)
                .eq(Budget::getIsActive, 1));

        if (existing != null) {
            existing.setAmount(dto.getAmount());
            existing.setStartDate(startDate);
            existing.setEndDate(dto.getEndDate());
            budgetMapper.updateById(existing);
            log.info("更新预算, budgetId={}", existing.getId());
            return Result.success(existing.getId());
        }

        Budget budget = new Budget();
        budget.setUserId(userId);
        budget.setCategoryId(catId);
        budget.setAmount(dto.getAmount());
        budget.setPeriod(period);
        budget.setStartDate(startDate);
        budget.setEndDate(dto.getEndDate());
        budget.setIsActive(1);

        budgetMapper.insert(budget);
        log.info("创建预算, budgetId={}", budget.getId());
        return Result.success(budget.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long userId, Long id) {
        Budget existing = budgetMapper.selectOne(new LambdaQueryWrapper<Budget>()
                .eq(Budget::getId, id).eq(Budget::getUserId, userId));
        if (existing == null) {
            throw new BusinessException(404, "预算不存在");
        }
        existing.setIsActive(0);
        budgetMapper.updateById(existing);
        return Result.success();
    }
}
