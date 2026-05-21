package com.xingzhewk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重置密码请求参数
 */
@Data
public class ResetPasswordDTO {

    /** 手机号（8-15 位，兼容国际格式） */
    @NotBlank(message = "手机号不能为空")
    @Size(min = 8, max = 15, message = "手机号格式不正确")
    private String phone;

    /** 短信验证码（6 位） */
    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码格式不正确")
    private String smsCode;

    /** 新密码（6-20 位，必须包含大小写字母+数字+特殊字符） */
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
