package com.expense.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 预算请求参数
 */
@Data
public class BudgetDTO {

    /** 分类 ID（null 表示总预算，前端传 category_id） */
    @JsonProperty("category_id")
    private Long categoryId;

    /** 预算金额（必须 > 0） */
    private BigDecimal amount;

    /** 预算周期：MONTHLY（月度）/ CUSTOM（自定义） */
    private String period;

    /** 起始日期（period=CUSTOM 时必填） */
    private LocalDate startDate;

    /** 结束日期（period=CUSTOM 时必填） */
    private LocalDate endDate;
}
