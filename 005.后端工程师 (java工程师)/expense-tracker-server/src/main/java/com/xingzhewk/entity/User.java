package com.xingzhewk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * 对应表：user
 */
@Data
@TableName("user")
public class User {

    /** 用户 ID（自增主键） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatarUrl;

    /** 手机号（登录账号） */
    private String phone;

    /** 国家区号（如 +86） */
    private String countryCode;

    /** 密码哈希（bcrypt） */
    private String passwordHash;

    /** 偏好币种：CNY / USD / EUR */
    private String currency;

    /** 偏好主题：light / dark */
    private String theme;

    /** 账号状态：1-正常，0-禁用 */
    private Integer status;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（插入和更新时自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
