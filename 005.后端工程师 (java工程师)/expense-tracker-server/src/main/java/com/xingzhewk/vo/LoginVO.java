package com.xingzhewk.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 登录响应视图
 */
@Data
public class LoginVO {

    /** JWT Token（有效期 7 天） */
    private String token;

    /** 用户 ID */
    private Long userId;

    /** 用户昵称 */
    private String nickname;
}
