package com.xingzhewk.common;

/**
 * 全局常量定义
 *
 * 与 contracts/openapi.yaml 的 components.x-constants 段对齐。
 * 修改任何数值之前先改 yaml，再同步两端代码。
 *
 * DIFFS #10：清理过 —— 删掉了 DEFAULT_PAGE_SIZE / DEFAULT_NICKNAME_PREFIX
 * 两个完全未使用的常量；MAX_NICKNAME_LENGTH 由 32 改为 50 以匹配实际校验
 * （契约 x-constants.nickname.maxLength=50）。
 */
public final class Constants {

    private Constants() {}

    /** 最大每页条数（契约 x-constants.bills.maxPageSize） */
    public static final int MAX_PAGE_SIZE = 100;

    /** 密码最小长度（契约 x-constants.password.minLength） */
    public static final int MIN_PASSWORD_LENGTH = 6;

    /** 密码最大长度（契约 x-constants.password.maxLength） */
    public static final int MAX_PASSWORD_LENGTH = 20;

    /** 昵称最大长度（契约 x-constants.nickname.maxLength） */
    public static final int MAX_NICKNAME_LENGTH = 50;

    /** 账单备注最大长度（契约 x-constants.bills.remarkMaxLength） */
    public static final int MAX_REMARK_LENGTH = 200;

    /** 分类名称最大长度（契约 x-constants.category.nameMaxLength） */
    public static final int MAX_CATEGORY_NAME_LENGTH = 20;
}
