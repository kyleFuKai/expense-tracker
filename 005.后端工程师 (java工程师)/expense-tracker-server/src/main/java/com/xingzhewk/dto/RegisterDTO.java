package com.xingzhewk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求参数
 */
@Data
public class RegisterDTO {

    /** 手机号（11 位，8-15 位兼容国际格式） */
    @NotBlank(message = "手机号不能为空")
    @Size(min = 8, max = 15, message = "手机号格式不正确")
    private String phone;

    /** 密码（6-20 位，必须包含大小写字母+数字+特殊字符） */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 昵称（可选，不传则使用手机号默认昵称） */
    private String nickname;
}
