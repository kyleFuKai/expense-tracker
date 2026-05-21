-- ============================================================================
-- 每日财务管家 — V1.0 数据库建表脚本 (MySQL 8.0+)
-- 包含：用户、分类、账单、预算、标签等核心表结构
-- 字符集：utf8mb4 | 排序规则：utf8mb4_unicode_ci
-- ============================================================================

-- ============================================================================
-- 1. 用户表 (User)
--    支持手机号登录 + 微信/支付宝第三方登录
-- ============================================================================
CREATE TABLE IF NOT EXISTS `user` (
    `id`              BIGINT UNSIGNED    NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
    `nickname`        VARCHAR(32)        NOT NULL DEFAULT '' COMMENT '昵称（显示名）',
    `avatar_url`      VARCHAR(512)       NOT NULL DEFAULT '' COMMENT '头像 URL',
    `phone`           VARCHAR(20)        NOT NULL DEFAULT '' COMMENT '手机号（登录账号，可选绑定）',
    `country_code`    VARCHAR(6)         NOT NULL DEFAULT '+86' COMMENT '国家/地区区号',
    `password_hash`   VARCHAR(255)       NOT NULL DEFAULT '' COMMENT '密码哈希 (bcrypt)',
    `currency`        CHAR(3)            NOT NULL DEFAULT 'CNY' COMMENT '默认货币单位',
    `theme`           VARCHAR(16)        NOT NULL DEFAULT 'light' COMMENT '主题模式: light / dark',
    `status`          TINYINT UNSIGNED   NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用 1=正常',
    `created_at`      DATETIME           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `updated_at`      DATETIME           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';


-- ============================================================================
-- 2. 第三方账号表 (UserThirdPartyAccount)
--    绑定微信/支付宝等第三方登录账号
-- ============================================================================
CREATE TABLE IF NOT EXISTS `user_third_party_account` (
    `id`              BIGINT UNSIGNED    NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `user_id`         BIGINT UNSIGNED    NOT NULL COMMENT '关联用户 ID',
    `platform`        VARCHAR(16)        NOT NULL COMMENT '平台: wechat / alipay',
    `open_id`         VARCHAR(128)       NOT NULL COMMENT '平台 open_id',
    `union_id`        VARCHAR(128)       NOT NULL DEFAULT '' COMMENT '平台 union_id',
    `nickname`        VARCHAR(64)        NOT NULL DEFAULT '' COMMENT '第三方平台昵称',
    `avatar_url`      VARCHAR(512)       NOT NULL DEFAULT '' COMMENT '第三方平台头像',
    `created_at`      DATETIME           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_platform_openid` (`platform`, `open_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='第三方账号绑定表';


-- ============================================================================
-- 3. 分类表 (Category)
--    预设系统分类 + 用户自定义分类，支持两级分类 + 归档
-- ============================================================================
CREATE TABLE IF NOT EXISTS `category` (
    `id`              BIGINT UNSIGNED    NOT NULL AUTO_INCREMENT COMMENT '分类 ID',
    `name`            VARCHAR(32)        NOT NULL COMMENT '分类名称',
    `icon`            VARCHAR(64)        NOT NULL DEFAULT '' COMMENT 'Material Symbol 图标名',
    `type`            ENUM('EXPENSE','INCOME') NOT NULL DEFAULT 'EXPENSE' COMMENT '类型: 支出/收入',
    `parent_id`       BIGINT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '父分类 ID (0=一级分类)',
    `sort_order`      INT UNSIGNED       NOT NULL DEFAULT 0 COMMENT '排序权重 (值越小越靠前)',
    `is_preset`       TINYINT UNSIGNED   NOT NULL DEFAULT 1 COMMENT '是否系统预设: 0=用户自定义 1=预设',
    `is_archived`     TINYINT UNSIGNED   NOT NULL DEFAULT 0 COMMENT '是否已归档: 0=正常 1=归档隐藏',
    `user_id`         BIGINT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '所属用户 (0=系统预设)',
    `created_at`      DATETIME           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_type_parent` (`type`, `parent_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收支分类表';


-- ============================================================================
-- 4. 账单表 (Bill)
--    核心表：记录每笔收入/支出，支持备注、标签、周期性标记
-- ============================================================================
CREATE TABLE IF NOT EXISTS `bill` (
    `id`              BIGINT UNSIGNED    NOT NULL AUTO_INCREMENT COMMENT '账单 ID',
    `user_id`         BIGINT UNSIGNED    NOT NULL COMMENT '用户 ID',
    `type`            ENUM('EXPENSE','INCOME') NOT NULL DEFAULT 'EXPENSE' COMMENT '类型: 支出/收入',
    `amount`          DECIMAL(12,2)      NOT NULL COMMENT '金额 (正数)',
    `category_id`     BIGINT UNSIGNED    NOT NULL COMMENT '分类 ID',
    `remark`          VARCHAR(200)       NOT NULL DEFAULT '' COMMENT '备注 (≤200字)',
    `bill_time`       DATETIME           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消费/收入时间',
    `is_recurring`    TINYINT UNSIGNED   NOT NULL DEFAULT 0 COMMENT '是否周期性自动生成: 0=否 1=是',
    `created_by`      BIGINT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '记录创建者 (0=本人, >0=共享账本成员)',
    `created_at`      DATETIME           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `bill_time`),
    KEY `idx_user_category` (`user_id`, `category_id`),
    KEY `idx_bill_time` (`bill_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账单记录表';


-- ============================================================================
-- 5. 账单标签表 (BillTag)
--    标签与账单的多对多关联
-- ============================================================================
CREATE TABLE IF NOT EXISTS `bill_tag` (
    `id`              BIGINT UNSIGNED    NOT NULL AUTO_INCREMENT COMMENT '标签 ID',
    `user_id`         BIGINT UNSIGNED    NOT NULL COMMENT '用户 ID',
    `name`            VARCHAR(16)        NOT NULL COMMENT '标签名称',
    `created_at`      DATETIME           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_name` (`user_id`, `name`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账单标签表';


-- ============================================================================
-- 6. 账单-标签关联表 (BillTagRel)
--    账单与标签的多对多关系
-- ============================================================================
CREATE TABLE IF NOT EXISTS `bill_tag_rel` (
    `id`              BIGINT UNSIGNED    NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `bill_id`         BIGINT UNSIGNED    NOT NULL COMMENT '账单 ID',
    `tag_id`          BIGINT UNSIGNED    NOT NULL COMMENT '标签 ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_bill_tag` (`bill_id`, `tag_id`),
    KEY `idx_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账单-标签关联表';


-- ============================================================================
-- 7. 预算表 (Budget)
--    支持总预算 + 分类预算，支持日/周/月周期
-- ============================================================================
CREATE TABLE IF NOT EXISTS `budget` (
    `id`              BIGINT UNSIGNED    NOT NULL AUTO_INCREMENT COMMENT '预算 ID',
    `user_id`         BIGINT UNSIGNED    NOT NULL COMMENT '用户 ID',
    `category_id`     BIGINT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '分类 ID (0=总预算)',
    `amount`          DECIMAL(10,2)      NOT NULL COMMENT '预算金额',
    `period`          ENUM('DAILY','WEEKLY','MONTHLY') NOT NULL DEFAULT 'MONTHLY' COMMENT '预算周期',
    `start_date`      DATE               NOT NULL COMMENT '生效起始日期',
    `end_date`        DATE               NULL DEFAULT NULL COMMENT '生效截止日期 (NULL=无截止)',
    `is_active`       TINYINT UNSIGNED   NOT NULL DEFAULT 1 COMMENT '是否启用: 0=停用 1=启用',
    `created_at`      DATETIME           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_period` (`user_id`, `period`, `is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预算表';
