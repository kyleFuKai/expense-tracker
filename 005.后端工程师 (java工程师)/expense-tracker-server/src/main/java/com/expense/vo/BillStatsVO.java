package com.expense.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 月度账单统计响应视图
 */
@Data
public class BillStatsVO {

    /** 支出汇总（总额、笔数） */
    private StatsItem expense;

    /** 收入汇总（总额、笔数） */
    private StatsItem income;

    /** 日趋势（当月每天的收支金额） */
    private List<DailyStat> daily;

    /** 分类排行（按金额降序） */
    private List<CategoryStat> categories;

    /** 收支汇总项 */
    @Data
    public static class StatsItem {
        /** 总金额 */
        private BigDecimal total;

        /** 总笔数 */
        private long count;
    }

    /** 每日收支统计 */
    @Data
    public static class DailyStat {
        /** 日期（yyyy-MM-dd） */
        private String date;

        /** 当日支出 */
        private BigDecimal expense;

        /** 当日收入 */
        private BigDecimal income;
    }

    /** 分类统计 */
    @Data
    public static class CategoryStat {
        /** 分类 ID */
        private Long id;

        /** 分类名称 */
        private String name;

        /** 分类图标 */
        private String icon;

        /** 分类总金额 */
        private BigDecimal total;

        /** 分类笔数 */
        private long count;
    }
}
