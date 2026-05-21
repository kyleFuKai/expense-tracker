package com.expense.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账单实体类
 *
 * 对应表：bill
 */
@Data
@TableName("bill")
public class Bill {

    /** 账单 ID（自增主键） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 ID */
    private Long userId;

    /** 账单类型：EXPENSE（支出）/ INCOME（收入） */
    private String type;

    /** 金额 */
    private BigDecimal amount;

    /** 分类 ID */
    private Long categoryId;

    /** 备注 */
    private String remark;

    /** 账单时间 */
    private LocalDateTime billTime;

    /** 是否循环账单：0-否，1-是 */
    private Integer isRecurring;

    /** 创建人 ID */
    private Long createdBy;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（插入和更新时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
