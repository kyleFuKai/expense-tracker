package com.expense.controller;

import com.expense.common.Result;
import com.expense.dto.BudgetDTO;
import com.expense.service.BudgetService;
import com.expense.vo.BudgetDashboardVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 预算控制器
 *
 * 处理预算的 CRUD、仪表盘查询等接口。所有接口均需 JWT 认证。
 */
@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    /**
     * 获取预算列表
     *
     * @param type 预算类型筛选（如 MONTHLY），为空则查询全部
     * @return 预算列表
     */
    @GetMapping
    public Result<?> list(HttpServletRequest request, @RequestParam(required = false) String type) {
        Long userId = (Long) request.getAttribute("userId");
        return budgetService.list(userId, type);
    }

    /**
     * 预算仪表盘（当月总预算、已用、剩余、分类进度）
     *
     * @param month 月份（yyyy-MM），为空则使用当前月
     * @return {totalBudget, spent, remaining, percent, categories: [...]}
     */
    @GetMapping("/dashboard")
    public Result<BudgetDashboardVO> dashboard(HttpServletRequest request,
                                               @RequestParam(required = false) String month) {
        Long userId = (Long) request.getAttribute("userId");
        return budgetService.dashboard(userId, month);
    }

    /**
     * 创建或更新预算
     *
     * @param dto 预算参数，包含 categoryId、amount（金额，必须 > 0）、period（预算周期，默认 MONTHLY）、startDate、endDate
     * @return {id: 预算ID}
     */
    @PostMapping
    public Result<Long> createOrUpdate(HttpServletRequest request, @Valid @RequestBody BudgetDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        return budgetService.createOrUpdate(userId, dto);
    }

    /**
     * 停用预算（软删除）
     *
     * @param id 预算 ID
     * @return 空，code=404 表示预算不存在
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        return budgetService.delete(userId, id);
    }
}
