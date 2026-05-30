package com.xingzhewk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建标签请求参数
 */
@Data
public class CreateTagDTO {

    /** 标签名称（1-16 字符） */
    @NotBlank(message = "标签名不能为空")
    @Size(max = 16, message = "标签名不能超过 16 个字符")
    private String name;
}
