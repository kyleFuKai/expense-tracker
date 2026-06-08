package com.xingzhewk.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 月度账单统计响应视图
 *
 * DIFFS #8：与 Node 后端对齐，change 字段嵌在 expense/income 子对象里，而非顶层。
 *   - expense: { total, count, change }
 *   - income:  { total, count, change }
 *   - change:  上月为 0 时为 null；正数=增长、负数=下降；保留 1 位小数
 */
@Data
public class BillStatsVO {

    /** 支出汇总（总额、笔数、环比%） */
    private StatsItem expense;

    /** 收入汇总（总额、笔数、环比%） */
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

        /**
         * 环比上月百分比。
         * 上月为 0 时为 null；非空时保留 1 位小数（HALF_UP）。
         */
        private BigDecimal change;
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
