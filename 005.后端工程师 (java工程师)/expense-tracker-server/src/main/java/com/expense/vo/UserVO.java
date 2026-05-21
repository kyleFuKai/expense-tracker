package com.expense.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户信息响应视图
 */
@Data
public class UserVO {

    /** 用户 ID */
    private Long id;

    /** 昵称 */
    private String nickname;

    /** 头像 URL（前端读 avatar_url） */
    @JsonProperty("avatar_url")
    private String avatarUrl;

    /** 手机号 */
    private String phone;

    /** 国家区号 */
    private String countryCode;

    /** 偏好币种：CNY / USD / EUR */
    private String currency;

    /** 偏好主题：light / dark */
    private String theme;

    /** 账号状态：1-正常，0-禁用 */
    private Integer status;

    /** 注册时间 */
    private LocalDateTime createdAt;
}
