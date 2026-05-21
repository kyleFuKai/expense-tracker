package com.expense.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分类实体类
 *
 * 对应表：category
 */
@Data
@TableName("category")
public class Category {

    /** 分类 ID（自增主键） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名称 */
    private String name;

    /** 分类图标（Material Symbol 名称） */
    private String icon;

    /** 分类类型：expense（支出）/ income（收入） */
    private String type;

    /** 父分类 ID（用于子分类，null 表示顶级分类） */
    private Long parentId;

    /** 排序权重（越大越靠前） */
    private Integer sortOrder;

    /** 是否预置：1-系统预置，0-用户自定义 */
    private Integer isPreset;

    /** 是否已归档：1-已归档，0-正常 */
    private Integer isArchived;

    /** 所属用户 ID（null 表示全局预置分类） */
    private Long userId;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
