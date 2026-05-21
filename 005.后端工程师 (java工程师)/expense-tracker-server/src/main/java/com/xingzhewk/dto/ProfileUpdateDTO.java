package com.xingzhewk.dto;

import lombok.Data;

/**
 * 用户信息更新请求参数
 */
@Data
public class ProfileUpdateDTO {

    /** 昵称（可选） */
    private String nickname;

    /** 头像 URL（可选） */
    private String avatarUrl;

    /** 币种：CNY / USD / EUR（可选） */
    private String currency;

    /** 主题：light（浅色）/ dark（深色）（可选） */
    private String theme;
}
