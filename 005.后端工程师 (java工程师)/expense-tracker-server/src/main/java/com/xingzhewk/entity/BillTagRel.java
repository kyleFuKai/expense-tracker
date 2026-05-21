package com.xingzhewk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 账单标签关联实体类
 *
 * 对应表：bill_tag_rel（账单与标签的多对多关系表）
 */
@Data
@TableName("bill_tag_rel")
public class BillTagRel {

    /** 关联 ID（自增主键） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 账单 ID */
    private Long billId;

    /** 标签 ID */
    private Long tagId;
}
