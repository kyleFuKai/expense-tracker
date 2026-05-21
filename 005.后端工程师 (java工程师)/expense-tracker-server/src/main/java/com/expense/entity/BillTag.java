package com.expense.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账单标签实体类
 *
 * 对应表：bill_tag
 */
@Data
@TableName("bill_tag")
public class BillTag {

    /** 标签 ID（自增主键） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 ID */
    private Long userId;

    /** 标签名称 */
    private String name;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
