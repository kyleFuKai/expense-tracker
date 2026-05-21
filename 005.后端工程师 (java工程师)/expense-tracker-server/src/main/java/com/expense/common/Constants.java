package com.expense.common;

/**
 * 全局常量定义
 *
 * 包含分页、密码、字段长度等系统级常量。
 */
public final class Constants {

    private Constants() {}

    /** 默认每页条数 */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /** 最大每页条数 */
    public static final int MAX_PAGE_SIZE = 100;

    /** 密码最小长度 */
    public static final int MIN_PASSWORD_LENGTH = 6;

    /** 密码最大长度 */
    public static final int MAX_PASSWORD_LENGTH = 20;

    /** 昵称最大长度 */
    public static final int MAX_NICKNAME_LENGTH = 32;

    /** 备注最大长度 */
    public static final int MAX_REMARK_LENGTH = 200;

    /** 分类名称最大长度 */
    public static final int MAX_CATEGORY_NAME_LENGTH = 20;

    /** 默认昵称前缀（用于注册时未填昵称的情况） */
    public static final String DEFAULT_NICKNAME_PREFIX = "用户";
}
