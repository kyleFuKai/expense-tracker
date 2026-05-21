package com.xingzhewk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预算实体类
 *
 * 对应表：budget
 */
@Data
@TableName("budget")
public class Budget {

    /** 预算 ID（自增主键） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 ID */
    private Long userId;

    /** 分类 ID（null 表示总预算） */
    private Long categoryId;

    /** 预算金额 */
    private BigDecimal amount;

    /** 预算周期：MONTHLY（月度）/ CUSTOM（自定义） */
    private String period;

    /** 起始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;

    /** 是否生效：0-已停用，1-生效中 */
    private Integer isActive;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（插入和更新时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
