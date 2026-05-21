package com.expense.dto;

import lombok.Data;

/**
 * 分类请求参数
 */
@Data
public class CategoryDTO {

    /** 分类名称 */
    private String name;

    /** 分类图标（Material Symbol 名称） */
    private String icon;

    /** 分类类型：expense（支出）/ income（收入） */
    private String type;

    /** 父分类 ID（用于子分类，可选） */
    private Long parentId;

    /** 排序权重（越大越靠前，可选） */
    private Integer sortOrder;
}
