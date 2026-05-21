package com.expense.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 预算仪表盘响应视图
 */
@Data
public class BudgetDashboardVO {

    /** 总预算金额 */
    private BigDecimal totalBudget;

    /** 已花费金额 */
    private BigDecimal spent;

    /** 剩余金额 */
    private BigDecimal remaining;

    /** 已用百分比（0-100） */
    private int percent;

    /** 各分类预算进度 */
    private List<CategoryProgress> categories;

    /** 单个分类的预算进度 */
    @Data
    public static class CategoryProgress {
        /** 预算 ID */
        private Long budgetId;

        /** 分类 ID */
        private Long catId;

        /** 分类名称 */
        private String categoryName;

        /** 分类图标 */
        private String categoryIcon;

        /** 预算金额 */
        private BigDecimal budgetAmount;

        /** 已花费金额 */
        private BigDecimal spent;

        /** 已用百分比（0-100） */
        private int percent;

        /** 剩余金额 */
        private BigDecimal remaining;
    }
}
