-- ============================================================================
-- 每日财务管家 — 预设分类数据 + 测试用户
-- 执行时机：建表完成后，导入预设系统分类
-- ============================================================================

-- ============================================================================
-- 预设分类数据
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 支出分类 (EXPENSE) — 一级分类
-- ---------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------
-- 支出分类 — 二级分类
-- 使用子查询动态获取一级分类 ID，避免硬编码
-- ---------------------------------------------------------------------------
-- 餐饮美食
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`)
SELECT '早餐',        'bakery_dining', 'EXPENSE', id, 1, 1 FROM `category` WHERE name = '餐饮美食' AND type = 'EXPENSE' AND parent_id = 0;
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`)
SELECT '午餐',        'restaurant',    'EXPENSE', id, 2, 1 FROM `category` WHERE name = '餐饮美食' AND type = 'EXPENSE' AND parent_id = 0;
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`)
SELECT '晚餐',        'dining',        'EXPENSE', id, 3, 1 FROM `category` WHERE name = '餐饮美食' AND type = 'EXPENSE' AND parent_id = 0;
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`)
SELECT '咖啡茶饮',    'coffee',        'EXPENSE', id, 4, 1 FROM `category` WHERE name = '餐饮美食' AND type = 'EXPENSE' AND parent_id = 0;
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`)
SELECT '零食小吃',    'cookie',        'EXPENSE', id, 5, 1 FROM `category` WHERE name = '餐饮美食' AND type = 'EXPENSE' AND parent_id = 0;

-- 交通出行
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`)
SELECT '公交地铁',    'subway',        'EXPENSE', id, 1, 1 FROM `category` WHERE name = '交通出行' AND type = 'EXPENSE' AND parent_id = 0;
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`)
SELECT '打车租车',    'local_taxi',    'EXPENSE', id, 2, 1 FROM `category` WHERE name = '交通出行' AND type = 'EXPENSE' AND parent_id = 0;
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`)
SELECT '私家车加油',  'local_gas_station', 'EXPENSE', id, 3, 1 FROM `category` WHERE name = '交通出行' AND type = 'EXPENSE' AND parent_id = 0;
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`)
SELECT '停车费',      'local_parking', 'EXPENSE', id, 4, 1 FROM `category` WHERE name = '交通出行' AND type = 'EXPENSE' AND parent_id = 0;

-- 购物消费
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`)
SELECT '日用百货',    'cart',          'EXPENSE', id, 1, 1 FROM `category` WHERE name = '购物消费' AND type = 'EXPENSE' AND parent_id = 0;
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`)
SELECT '服饰鞋包',    'checkroom',     'EXPENSE', id, 2, 1 FROM `category` WHERE name = '购物消费' AND type = 'EXPENSE' AND parent_id = 0;
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`)
SELECT '数码产品',    'devices',       'EXPENSE', id, 3, 1 FROM `category` WHERE name = '购物消费' AND type = 'EXPENSE' AND parent_id = 0;
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`)
SELECT '美妆护肤',    'visibility',    'EXPENSE', id, 4, 1 FROM `category` WHERE name = '购物消费' AND type = 'EXPENSE' AND parent_id = 0;

-- 休闲娱乐
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`)
SELECT '电影演出',    'theater_comedy','EXPENSE', id, 1, 1 FROM `category` WHERE name = '休闲娱乐' AND type = 'EXPENSE' AND parent_id = 0;
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`)
SELECT '运动健身',    'fitness_center','EXPENSE', id, 2, 1 FROM `category` WHERE name = '休闲娱乐' AND type = 'EXPENSE' AND parent_id = 0;
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`)
SELECT '旅游度假',    'flight',        'EXPENSE', id, 3, 1 FROM `category` WHERE name = '休闲娱乐' AND type = 'EXPENSE' AND parent_id = 0;
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`)
SELECT '游戏充值',    'sports_esports','EXPENSE', id, 4, 1 FROM `category` WHERE name = '休闲娱乐' AND type = 'EXPENSE' AND parent_id = 0;

-- ---------------------------------------------------------------------------
-- 收入分类 (INCOME) — 一级分类
-- 先插入一级分类，记录其 AUTO_INCREMENT ID
-- ---------------------------------------------------------------------------
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`) VALUES
('工资薪酬', 'payments',      'INCOME', 0, 1,  1),
('兼职副业', 'work',          'INCOME', 0, 2,  1),
('理财收益', 'trending_up',   'INCOME', 0, 3,  1),
('红包礼物', 'redeem',        'INCOME', 0, 4,  1),
('报销退款', 'money_off',     'INCOME', 0, 5,  1),
('其他收入', 'add_circle',    'INCOME', 0, 99, 1);

-- ---------------------------------------------------------------------------
-- 收入分类 — 二级分类
-- 使用子查询动态获取一级分类 ID，避免硬编码
-- ---------------------------------------------------------------------------
-- 工资薪酬
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`)
SELECT '月薪',   'account_balance', 'INCOME', id, 1, 1 FROM `category` WHERE name = '工资薪酬' AND type = 'INCOME' AND parent_id = 0;
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`)
SELECT '年终奖', 'emoji_events',    'INCOME', id, 2, 1 FROM `category` WHERE name = '工资薪酬' AND type = 'INCOME' AND parent_id = 0;
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`)
SELECT '绩效奖', 'stars',           'INCOME', id, 3, 1 FROM `category` WHERE name = '工资薪酬' AND type = 'INCOME' AND parent_id = 0;

-- 理财收益
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`)
SELECT '基金收益', 'show_chart',     'INCOME', id, 1, 1 FROM `category` WHERE name = '理财收益' AND type = 'INCOME' AND parent_id = 0;
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`)
SELECT '股票收益', 'trending_up',    'INCOME', id, 2, 1 FROM `category` WHERE name = '理财收益' AND type = 'INCOME' AND parent_id = 0;
INSERT INTO `category` (`name`, `icon`, `type`, `parent_id`, `sort_order`, `is_preset`)
SELECT '利息收入', 'savings',        'INCOME', id, 3, 1 FROM `category` WHERE name = '理财收益' AND type = 'INCOME' AND parent_id = 0;


-- ============================================================================
-- 测试用户 (bcrypt 哈希为明文 '123456' 的哈希值)
-- WARNING: 此测试账户仅供本地开发使用，禁止在生产环境导入此数据！
-- ============================================================================
INSERT INTO `user` (`nickname`, `phone`, `password_hash`) VALUES
('小李', '13800138000', '$2a$10$N9qo8uLOickGH2j0iN0MteKHuqEEqMNnXNlB6gPGbG1fGqZ1c3LmG');
