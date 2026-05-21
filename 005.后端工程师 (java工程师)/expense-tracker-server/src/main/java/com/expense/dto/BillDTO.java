package com.expense.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 账单请求参数
 */
@Data
public class BillDTO {

    /** 账单类型：EXPENSE（支出）/ INCOME（收入） */
    private String type;

    /** 金额（必须 > 0） */
    private BigDecimal amount;

    /** 分类 ID（前端传 category_id） */
    @JsonProperty("category_id")
    private Long categoryId;

    /** 备注（可选，最长 200 字符） */
    private String remark;

    /** 账单时间（可选，前端传 bill_time，格式：yyyy-MM-dd HH:mm:ss） */
    @JsonProperty("bill_time")
    private String billTime;
}
