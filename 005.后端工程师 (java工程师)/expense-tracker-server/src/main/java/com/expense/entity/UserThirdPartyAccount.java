package com.expense.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户第三方账号绑定实体类
 *
 * 对应表：user_third_party_account
 */
@Data
@TableName("user_third_party_account")
public class UserThirdPartyAccount {

    /** 记录 ID（自增主键） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 ID */
    private Long userId;

    /** 第三方平台标识（如 wechat、alipay、google） */
    private String platform;

    /** 第三方平台 OpenID */
    private String openId;

    /** 第三方平台 UnionID（跨应用统一标识，可选） */
    private String unionId;

    /** 第三方平台昵称 */
    private String nickname;

    /** 第三方平台头像 URL */
    private String avatarUrl;

    /** 绑定时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
