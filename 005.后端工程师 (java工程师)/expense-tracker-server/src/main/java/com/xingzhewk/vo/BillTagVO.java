package com.xingzhewk.vo;

import lombok.Data;

/**
 * 标签响应对象
 */
@Data
public class BillTagVO {

    /** 标签 ID */
    private Long id;

    /** 标签名称 */
    private String name;

    /** 关联账单数量 */
    private Long billCount;
}
