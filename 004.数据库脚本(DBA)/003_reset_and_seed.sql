-- ============================================================================
-- 数据清理脚本 — 清空所有表数据，重新导入初始化数据
-- 执行方式：mysql -u root -p < 003_reset_and_seed.sql
-- ============================================================================

-- 关闭外键检查（避免删除顺序问题）
SET FOREIGN_KEY_CHECKS = 0;

-- 清空所有表数据
TRUNCATE TABLE `bill_tag_rel`;
TRUNCATE TABLE `bill_tag`;
TRUNCATE TABLE `bill`;
TRUNCATE TABLE `budget`;
TRUNCATE TABLE `category`;
TRUNCATE TABLE `user_third_party_account`;
TRUNCATE TABLE `user`;

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 重新导入预设分类数据
-- ============================================================================

-- 支出分类 (EXPENSE) — 一级分类
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`) VALUES
('餐饮美食', 'restaurant',          'EXPENSE', 0, 1,  1),
('交通出行', 'directions_car',      'EXPENSE', 0, 2,  1),
('购物消费', 'shopping_bag',        'EXPENSE', 0, 3,  1),
('休闲娱乐', 'movie',               'EXPENSE', 0, 4,  1),
('居住物业', 'home',                'EXPENSE', 0, 5,  1),
('医疗健康', 'medical_services',    'EXPENSE', 0, 6,  1),
('教育培训', 'school',              'EXPENSE', 0, 7,  1),
('人情往来', 'group',               'EXPENSE', 0, 8,  1),
('宠物生活', 'pets',                'EXPENSE', 0, 9,  1),
('通讯网络', 'phone_android',       'EXPENSE', 0, 10, 1),
('金融保险', 'savings',             'EXPENSE', 0, 11, 1),
('其他支出', 'more_horiz',          'EXPENSE', 0, 99, 1);

-- 支出分类 — 二级分类
-- 餐饮美食 (parent_id=1)
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`) VALUES
('早餐',        'bakery_dining', 'EXPENSE', 1, 1,  1),
('午餐',        'restaurant',    'EXPENSE', 1, 2,  1),
('晚餐',        'dining',        'EXPENSE', 1, 3,  1),
('咖啡茶饮',    'coffee',        'EXPENSE', 1, 4,  1),
('零食小吃',    'cookie',        'EXPENSE', 1, 5,  1);

-- 交通出行 (parent_id=2)
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`) VALUES
('公交地铁',    'subway',        'EXPENSE', 2, 1,  1),
('打车租车',    'local_taxi',    'EXPENSE', 2, 2,  1),
('私家车加油',  'local_gas_station', 'EXPENSE', 2, 3, 1),
('停车费',      'local_parking', 'EXPENSE', 2, 4,  1);

-- 购物消费 (parent_id=3)
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`) VALUES
('日用百货',    'cart',          'EXPENSE', 3, 1,  1),
('服饰鞋包',    'checkroom',     'EXPENSE', 3, 2,  1),
('数码产品',    'devices',       'EXPENSE', 3, 3,  1),
('美妆护肤',    'visibility',    'EXPENSE', 3, 4,  1);

-- 休闲娱乐 (parent_id=4)
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`) VALUES
('电影演出',    'theater_comedy','EXPENSE', 4, 1,  1),
('运动健身',    'fitness_center','EXPENSE', 4, 2,  1),
('旅游度假',    'flight',        'EXPENSE', 4, 3,  1),
('游戏充值',    'sports_esports','EXPENSE', 4, 4,  1);

-- 收入分类 (INCOME) — 一级分类
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`) VALUES
('工资薪酬', 'payments',      'INCOME', 0, 1,  1),
('兼职副业', 'work',          'INCOME', 0, 2,  1),
('理财收益', 'trending_up',   'INCOME', 0, 3,  1),
('红包礼物', 'redeem',        'INCOME', 0, 4,  1),
('报销退款', 'money_off',     'INCOME', 0, 5,  1),
('其他收入', 'add_circle',    'INCOME', 0, 99, 1);

-- 收入分类 — 二级分类
-- 工资薪酬 (parent_id=13)
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`) VALUES
('月薪',   'account_balance', 'INCOME', 13, 1, 1),
('年终奖', 'emoji_events',    'INCOME', 13, 2, 1),
('绩效奖', 'stars',           'INCOME', 13, 3, 1);

-- 理财收益 (parent_id=15)
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`) VALUES
('基金收益', 'show_chart',     'INCOME', 15, 1, 1),
('股票收益', 'trending_up',    'INCOME', 15, 2, 1),
('利息收入', 'savings',        'INCOME', 15, 3, 1);

-- ============================================================================
-- 完成提示
-- ============================================================================
SELECT '数据清理完成！' AS message;
SELECT COUNT(*) AS category_count FROM `category`;
SELECT '测试用户由测试脚本自动创建，无需预设' AS note;
