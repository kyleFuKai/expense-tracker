package com.xingzhewk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送短信验证码请求参数
 */
@Data
public class SendSmsCodeDTO {

    /** 手机号（8-15 位，兼容国际格式） */
    @NotBlank(message = "手机号不能为空")
    @Size(min = 8, max = 15, message = "手机号格式不正确")
    private String phone;
}
