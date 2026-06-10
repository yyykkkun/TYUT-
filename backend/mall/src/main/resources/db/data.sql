-- ============================================
-- TYUT Mall 种子数据
-- 与前端 mock.ts 保持一致
-- ============================================

-- 分类（匹配前端 categories）
INSERT IGNORE INTO mall_category (id, name, code, sort_order, created_at) VALUES
(1, '生鲜食品', 'fresh', 1, NOW()),
(2, '数码家电', 'digital', 2, NOW()),
(3, '美妆个护', 'beauty', 3, NOW()),
(4, '家居日用', 'home', 4, NOW()),
(5, '运动户外', 'sports', 5, NOW()),
(6, '服饰鞋包', 'fashion', 6, NOW());

-- 商品（匹配前端 products，使用数字 ID 便于 JPA）
INSERT IGNORE INTO mall_product (id, title, subtitle, category_id, brand, origin, city, tags, price, original_price, stock, sales, popularity, rating, image, specs, promotion, status, created_at, updated_at) VALUES
(1001, '松谷有机蓝莓礼盒', '冷链直达，颗颗饱满，适合早餐与烘焙', 1, '松谷', '中国', '大连', '["新品","冷链","会员价"]', 89.00, 119.00, 126, 3820, 96, 4.8, 'https://images.unsplash.com/photo-1498557850523-fd3d118b962e?auto=format&fit=crop&w=900&q=80', '["500g","1kg","家庭装"]', 'special', 1, '2026-05-30', NOW()),

(1002, 'Auralite 主动降噪耳机', '40 小时续航，通勤降噪，低延迟游戏模式', 2, 'Auralite', '中国', '深圳', '["热卖","免息","包邮"]', 399.00, 529.00, 78, 9140, 99, 4.9, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=900&q=80', '["曜石黑","月光银","海盐蓝"]', 'seckill', 1, '2026-05-22', NOW()),

(1003, 'Lumiere 玻尿酸精华', '清爽不黏腻，早晚保湿修护', 3, 'Lumiere', '法国', '巴黎', '["买赠","敏感肌"]', 168.00, 219.00, 0, 2310, 84, 4.7, 'https://images.unsplash.com/photo-1620916566398-39f1143ab7be?auto=format&fit=crop&w=900&q=80', '["30ml","50ml"]', NULL, 1, '2026-05-15', NOW()),

(1004, '北欧可叠收纳盒套装', '厨房、书桌、衣柜多场景收纳', 4, 'Nordi', '中国', '杭州', '["组合套餐","满减"]', 59.00, 79.00, 312, 5120, 88, 4.6, 'https://images.unsplash.com/photo-1519710164239-da123dc03ef4?auto=format&fit=crop&w=900&q=80', '["三件套","六件套","十件套"]', 'group-buy', 1, '2026-05-28', NOW()),

(1005, 'TrailPro 轻量徒步鞋', '抓地耐磨，城市通勤与周末徒步都合适', 5, 'TrailPro', '越南', '胡志明', '["人气","会员专场"]', 459.00, 699.00, 44, 2760, 91, 4.8, 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80', '["38","39","40","41","42","43"]', NULL, 1, '2026-05-20', NOW()),

(1006, '织云真丝方巾', '轻盈柔软，通勤造型点睛配饰', 6, '织云', '中国', '苏州', '["新品","礼品"]', 129.00, 169.00, 88, 980, 76, 4.5, 'https://images.unsplash.com/photo-1520903920243-00d872a2d1c9?auto=format&fit=crop&w=900&q=80', '["晨雾蓝","山茶白","松墨绿"]', NULL, 1, '2026-06-01', NOW());

-- 优惠券模板
INSERT IGNORE INTO mall_coupon (id, title, threshold, amount, total_count, valid_days, created_at) VALUES
(1, '满 199 减 30', 199.00, 30.00, 100, 30, NOW()),
(2, '满 99 减 12', 99.00, 12.00, 100, 18, NOW()),
(3, '生鲜专享 8 元券', 59.00, 8.00, 200, 30, NOW());

-- 演示用户（密码: 123456）
-- BCrypt: $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh 简化版
INSERT IGNORE INTO mall_user (id, username, password, phone, nickname, avatar, level, balance, points, gift_card, growth, status, created_at, updated_at) VALUES
(1, 'demo@mall.test', '$2a$10$Eqj5vK1I3Kq9CKfR0qMPpO6INd7f5E6qJdTCO0hGJ5rIljQkXt3Sy', '13800008888', '演示会员', NULL, '黄金会员', 688.00, 2680, 120.00, 7420, 1, NOW(), NOW());

-- 演示用户收货地址
INSERT IGNORE INTO mall_address (id, user_id, name, phone, province, city, district, detail, is_default, created_at) VALUES
(1, 1, '陈同学', '13800008888', '上海市', '上海市', '浦东新区', '世纪大道 100 号 8 栋 1201', 1, NOW()),
(2, 1, '课程演示', '13900006666', '江苏省', '南京市', '玄武区', '校园路 6 号创新楼', 0, NOW());

-- 演示用户优惠券
INSERT IGNORE INTO mall_user_coupon (id, user_id, coupon_id, used, expires_at, used_at) VALUES
(1, 1, 1, 0, '2026-07-01 00:00:00', NULL),
(2, 1, 2, 0, '2026-06-18 00:00:00', NULL),
(3, 1, 3, 0, '2026-06-30 00:00:00', NULL);

-- 默认消息
INSERT IGNORE INTO mall_message (id, user_id, title, body, type, `read`, created_at) VALUES
(1, 1, '你关注的耳机正在秒杀', 'Auralite 主动降噪耳机今日限时价 399 元。', 'promotion', 0, '2026-06-03 09:20:00'),
(2, 1, '蓝莓礼盒降价提醒', '松谷有机蓝莓礼盒已降至 89 元。', 'price', 0, '2026-06-02 18:10:00');
