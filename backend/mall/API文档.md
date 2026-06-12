# 校园优选商城 - 后端 API 文档

> **技术栈**：Spring Boot 3.5.14 + Spring Security + JPA + MySQL + Redis + JWT  
> **基础路径**：`http://localhost:8080`  
> **响应格式**：`{ "code": 0, "message": "ok", "data": ... }`（code=0 表示成功）

---

## 目录

- [1. 认证说明](#1-认证说明)
- [2. 认证模块 `/api/auth`](#2-认证模块-apiauth)
- [3. 商品模块 `/api/products`](#3-商品模块-apiproducts)
- [4. 分类模块 `/api/categories`](#4-分类模块-apicategories)
- [5. 购物车模块 `/api/cart`](#5-购物车模块-apicart)
- [6. 订单模块 `/api/orders`](#6-订单模块-apiorders)
- [7. 会员模块 `/api/member`](#7-会员模块-apimember)
- [8. 消息模块 `/api/messages`](#8-消息模块-apimessages)
- [9. 促销模块 `/api/promotions`](#9-促销模块-apipromotions)
- [10. 数据库表结构](#10-数据库表结构)
- [11. 业务规则](#11-业务规则)
- [12. 配置说明](#12-配置说明)

---

## 1. 认证说明

### 鉴权方式

所有非公开接口需要在请求头携带 JWT Token：

```
Authorization: Bearer <token>
```

### 公开接口（无需 Token）

| 路径前缀 | 说明 |
|----------|------|
| `/api/auth/**` | 登录/注册 |
| `/api/products/**` | 商品相关 |
| `/api/categories/**` | 分类 |
| `/api/promotions/**` | 促销活动 |

### 获取 Token

调用登录/注册接口后，响应中的 `data.token` 即为 JWT，有效期 **24 小时**。

---

## 2. 认证模块 `/api/auth`

### 2.1 登录

```
POST /api/auth/login
```

**请求体：**
```json
{
  "account": "demo@mall.test",   // 用户名或手机号
  "password": "123456"
}
```

**响应 data：**
```json
{
  "token": "eyJhbGciOi...",
  "user": {
    "id": "1",
    "name": "演示会员",
    "account": "demo@mall.test",
    "level": "黄金会员"
  }
}
```

### 2.2 注册

```
POST /api/auth/register
```

**请求体：**
```json
{
  "username": "newuser",         // 必填，2-32字符
  "password": "123456",          // 必填，6-32字符
  "phone": "13800000001",        // 选填
  "nickname": "新用户"            // 选填
}
```

> 新用户默认：余额 100、积分 500、普通会员

**响应 data：** 同登录，返回 token + user

### 2.3 匿名登录

```
POST /api/auth/login/anonymous
```

**请求体：**
```json
{
  "phone": "13800008888"
}
```

> 手机号已存在则直接登录，不存在则自动注册（昵称=匿名用户XXXX，余额688，积分2680，礼品卡120）

### 2.4 获取当前用户

```
GET /api/auth/me
```

**响应 data：**
```json
{
  "id": "1",
  "name": "演示会员",
  "account": "demo@mall.test",
  "level": "黄金会员"
}
```

---

## 3. 商品模块 `/api/products`

### 3.1 商品搜索/列表

```
GET /api/products
```

**查询参数（全部可选）：**

| 参数 | 类型 | 说明 |
|------|------|------|
| `keyword` | String | 搜索关键词（标题/副标题/品牌/标签） |
| `category` | String | 分类ID |
| `brand` | String | 品牌名 |
| `stock` | String | `in`=有货, `out`=无货 |
| `city` | String | 城市 |
| `minPrice` | Double | 最低价 |
| `maxPrice` | Double | 最高价 |
| `sort` | String | 排序：`composite`(默认)/`priceAsc`/`priceDesc`/`sales`/`popularity`/`latest` |
| `page` | Integer | 页码，默认 1 |
| `pageSize` | Integer | 每页条数，默认 6 |

**响应 data：**
```json
{
  "products": [
    {
      "id": "1",
      "title": "松谷有机蓝莓礼盒",
      "subtitle": "冷链直达，颗颗饱满",
      "category": "1",
      "brand": "松谷",
      "origin": "中国",
      "city": "大连",
      "tags": ["新品", "冷链", "会员价"],
      "price": 89.00,
      "originalPrice": 119.00,
      "stock": 126,
      "sales": 3820,
      "popularity": 96,
      "rating": 4.8,
      "image": "https://...",
      "specs": ["500g", "1kg", "家庭装"],
      "createdAt": "2026-05-30",
      "promotion": "special"
    }
  ],
  "total": 6,
  "page": 1,
  "pageSize": 6,
  "totalPages": 1
}
```

### 3.2 商品详情

```
GET /api/products/{id}
```

> 若用户已登录，自动记录浏览历史

### 3.3 热卖商品

```
GET /api/products/hot
```

返回销量前 8 的商品。

### 3.4 最新商品

```
GET /api/products/latest
```

返回最新上架的 8 个商品。

### 3.5 特卖商品

```
GET /api/products/special
```

返回有原价（折扣价）的 8 个商品。

### 3.6 推荐商品（猜你喜欢）

```
GET /api/products/recommended
```

> 根据用户最近浏览商品的分类推荐同分类商品，无浏览记录则返回热卖商品。

---

## 4. 分类模块 `/api/categories`

### 4.1 分类列表

```
GET /api/categories
```

**响应 data：**
```json
[
  { "id": "1", "name": "生鲜食品" },
  { "id": "2", "name": "数码家电" },
  { "id": "3", "name": "美妆个护" },
  { "id": "4", "name": "家居日用" },
  { "id": "5", "name": "运动户外" },
  { "id": "6", "name": "服饰鞋包" }
]
```

---

## 5. 购物车模块 `/api/cart`

> **全部需要认证**

### 5.1 购物车列表

```
GET /api/cart
```

**响应 data：**
```json
[
  {
    "id": "1",
    "productId": "1",
    "spec": "500g",
    "quantity": 2,
    "selected": true,
    "product": { /* 完整的 ProductVO */ }
  }
]
```

### 5.2 加入购物车

```
POST /api/cart
```

**请求体：**
```json
{
  "productId": 1,
  "spec": "500g",
  "quantity": 1          // 默认 1，最小 1
}
```

> 同一商品+同一规格已存在时，累加数量而非新增条目。

### 5.3 修改数量

```
PUT /api/cart/{id}
```

**请求体：**
```json
{
  "quantity": 5          // 最小 1
}
```

### 5.4 切换选中状态

```
PATCH /api/cart/{id}/select
```

> 在选中/未选中之间切换

### 5.5 删除单个商品

```
DELETE /api/cart/{id}
```

### 5.6 清空已选商品

```
DELETE /api/cart/selected
```

---

## 6. 订单模块 `/api/orders`

> **全部需要认证**

### 6.1 订单列表

```
GET /api/orders
```

**查询参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| `status` | String | `all`(默认)/`pending_payment`/`paid`/`shipping`/`completed`/`cancelled` |
| `page` | Integer | 页码，默认 1 |
| `pageSize` | Integer | 每页条数，默认 10 |

**响应 data 中单个订单结构：**
```json
{
  "id": "1",
  "status": "pending_payment",
  "createdAt": "2026-06-11 10:30:00",
  "items": [
    {
      "productId": "1",
      "title": "松谷有机蓝莓礼盒",
      "image": "https://...",
      "spec": "500g",
      "price": 89.00,
      "quantity": 2
    }
  ],
  "address": {
    "id": "1",
    "name": "陈同学",
    "phone": "13800008888",
    "province": "上海市",
    "city": "上海市",
    "district": "浦东新区",
    "detail": "世纪大道 100 号 8 栋 1201",
    "isDefault": true
  },
  "deliveryMethod": "platform",
  "couponAmount": 0,
  "pointsUsed": 2000,
  "giftCardAmount": 0,
  "freight": 12.00,
  "total": 190.00,
  "paidAt": null,
  "deliveryNo": null,
  "review": null
}
```

### 6.2 订单详情

```
GET /api/orders/{id}
```

### 6.3 订单试算（金额预览）

```
POST /api/orders/preview
```

**请求体：**
```json
{
  "couponId": 1,          // 选填，优惠券ID
  "usePoints": true,      // 默认 true，是否使用积分
  "useGiftCard": false,   // 默认 false，是否使用礼品卡
  "useBalance": false,    // 默认 false，是否使用余额
  "deliveryMethod": "platform"  // platform / third-party / pickup
}
```

**响应 data：**
```json
{
  "subtotal": 178.00,       // 商品小计
  "couponAmount": 30.00,    // 优惠券抵扣
  "pointsAmount": 20.00,    // 积分抵扣（元）
  "giftCardAmount": 0,      // 礼品卡抵扣
  "balanceAmount": 128.00,  // 余额支付
  "freight": 12.00,         // 运费
  "total": 0                // 应付金额（余额全覆盖则为0）
}
```

### 6.4 创建订单

```
POST /api/orders
```

**请求体：** 同试算接口

**响应 data：** 订单号字符串，如 `"SO1718123456789"`

> 创建订单时执行：验证库存 → 计算价格 → 生成订单号 → 保存订单和订单项 → 扣减库存 → 使用优惠券 → 扣减积分/礼品卡 → 清空购物车已选项

### 6.5 支付订单

```
PUT /api/orders/{id}/pay
```

> 状态变更为 `paid`，生成物流单号（YT + 8位随机数）

### 6.6 取消订单

```
PUT /api/orders/{id}/cancel
```

> 仅 `pending_payment` 状态可取消。恢复库存、积分、礼品卡。

### 6.7 确认收货

```
PUT /api/orders/{id}/confirm
```

> 状态变更为 `completed`

### 6.8 评价订单

```
PUT /api/orders/{id}/review
```

**请求体：**
```json
{
  "review": "商品体验不错，物流也很及时。"
}
```

> 仅 `completed` 状态可评价

---

## 7. 会员模块 `/api/member`

> **全部需要认证**

### 7.1 会员信息

```
GET /api/member/profile
```

**响应 data：**
```json
{
  "balance": 688.00,
  "points": 2680,
  "giftCard": 120.00,
  "growth": 7420,
  "level": "黄金会员",
  "couponCount": 3,
  "unreadCount": 2
}
```

### 7.2 地址列表

```
GET /api/member/addresses
```

### 7.3 新增地址

```
POST /api/member/addresses
```

**请求体：**
```json
{
  "name": "陈同学",          // 必填
  "phone": "13800008888",    // 必填
  "province": "上海市",
  "city": "上海市",
  "district": "浦东新区",
  "detail": "世纪大道 100 号", // 必填
  "isDefault": true
}
```

> 若设为默认地址，自动取消其他地址的默认状态。

### 7.4 设为默认地址

```
PUT /api/member/addresses/{id}/default
```

### 7.5 删除地址

```
DELETE /api/member/addresses/{id}
```

> 若删除的是默认地址，自动将第一个剩余地址设为默认。

### 7.6 优惠券列表

```
GET /api/member/coupons
```

**响应 data：**
```json
[
  {
    "id": "1",
    "title": "满 199 减 30",
    "threshold": 199.00,
    "amount": 30.00,
    "expiresAt": "2026-07-01",
    "used": false
  }
]
```

### 7.7 余额充值

```
POST /api/member/recharge
```

**请求体：**
```json
{
  "amount": 100.00
}
```

**响应 data：** 返回最新的 `MemberProfileVO`

### 7.8 积分兑换优惠券

```
POST /api/member/coupons/exchange
```

**请求体：**
```json
{
  "points": 500
}
```

> 消耗 500 积分兑换 10 元券（满 60 元可用，30 天有效）

### 7.9 浏览记录

```
GET /api/member/browse-history
```

**响应 data：** `["1", "3", "5"]`（最近 12 条商品 ID，去重）

### 7.10 记录浏览

```
POST /api/member/browse-history
```

**请求体：**
```json
{
  "productId": 1
}
```

> 同一商品重复浏览会删除旧记录后新增（移到最新位置）。

---

## 8. 消息模块 `/api/messages`

> **全部需要认证**

### 8.1 消息列表

```
GET /api/messages
```

**响应 data：**
```json
[
  {
    "id": "1",
    "title": "你关注的耳机正在秒杀",
    "body": "Auralite 主动降噪耳机今日限时价 399 元。",
    "type": "promotion",
    "read": false,
    "createdAt": "2026-06-03 09:20"
  }
]
```

> 消息类型：`price` 降价通知 / `promotion` 活动通知 / `order` 订单消息

### 8.2 标为已读

```
PUT /api/messages/{id}/read
```

---

## 9. 促销模块 `/api/promotions`

### 9.1 秒杀商品

```
GET /api/promotions/seckill
```

返回 `promotion = "seckill"` 且 `status = 1`（在线）的商品。

### 9.2 团购商品

```
GET /api/promotions/group-buy
```

返回 `promotion = "group-buy"` 且 `status = 1`（在线）的商品。

---

## 10. 数据库表结构

### 10.1 表一览

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| `mall_user` | 用户 | id, username, password, phone, nickname, level, balance, points, gift_card, growth, status |
| `mall_product` | 商品 | id, title, subtitle, category_id, brand, origin, city, tags, price, original_price, stock, sales, popularity, rating, image, specs, promotion, status |
| `mall_category` | 分类 | id, name, code, sort_order |
| `mall_cart_item` | 购物车 | id, user_id, product_id, spec, quantity, selected |
| `mall_order` | 订单 | id, order_no, user_id, status, delivery_method, address_snapshot, coupon_amount, points_used, gift_card_amount, freight, total, paid_at, delivery_no, review |
| `mall_order_item` | 订单项 | id, order_id, product_id, title, image, spec, price, quantity |
| `mall_address` | 收货地址 | id, user_id, name, phone, province, city, district, detail, is_default |
| `mall_coupon` | 优惠券模板 | id, title, threshold, amount, total_count, valid_days |
| `mall_user_coupon` | 用户优惠券 | id, user_id, coupon_id, used, expires_at, used_at |
| `mall_message` | 站内信 | id, user_id, title, body, type, read |
| `mall_browse_history` | 浏览记录 | id, user_id, product_id, created_at |

### 10.2 ER 关系

```
User ──1:N── CartItem ──N:1── Product
User ──1:N── Order ──1:N── OrderItem ──N:1── Product
User ──1:N── Address
User ──1:N── Message
User ──1:N── BrowseHistory ──N:1── Product
User ──1:N── UserCoupon ──N:1── Coupon
Product ──N:1── Category
```

---

## 11. 业务规则

### 11.1 订单金额计算规则

```
应付金额 = 商品小计 - 优惠券抵扣 - 积分抵扣 - 礼品卡抵扣 - 余额支付 + 运费
```

| 项目 | 规则 |
|------|------|
| **优惠券** | 满 threshold 元减 amount 元，单笔订单限用 1 张 |
| **积分抵扣** | 100 积分 = 1 元，单笔最多抵 20 元（2000 积分） |
| **礼品卡** | 单笔最多抵 50 元 |
| **余额支付** | 抵扣所有优惠后的待付金额，不超过账户余额 |
| **运费** | 满 199 元免运费，否则 12 元；门店自提免运费 |

### 11.2 订单状态流转

```
pending_payment ──支付──▶ paid ──确认收货──▶ completed
       │                    │
       └──取消──▶ cancelled  └──确认收货──▶ completed
```

| 状态 | 可执行操作 |
|------|-----------|
| `pending_payment` | 支付、取消 |
| `paid` | 确认收货 |
| `shipping` | 确认收货 |
| `completed` | 评价 |
| `cancelled` | 无 |

### 11.3 库存扣减时机

- **创建订单时**：立即扣减库存（防止超卖）
- **取消订单时**：恢复库存
- **支付时**：不操作库存

### 11.4 积分/余额规则

| 场景 | 积分变动 | 余额变动 |
|------|---------|---------|
| 注册 | +500 | 0 |
| 匿名登录 | +2680 | +688 |
| 下单使用积分 | 按 100:1 扣除（单笔上限 2000） | - |
| 下单使用余额 | - | 扣除相应金额 |
| 取消订单 | 恢复已扣积分 | 恢复已扣余额 |
| 兑换优惠券 | -500 分 → 得 10 元券 | - |
| 余额充值 | - | +充值金额 |

### 11.5 浏览记录去重

同一用户浏览同一商品时，删除旧记录后新增（始终保持最新在前，每用户仅保留最近 12 条）。

---

## 12. 配置说明

### application.yml 核心配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | 8080 | 服务端口 |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/mall` | 数据库连接 |
| `spring.datasource.username` | root | 数据库用户 |
| `spring.datasource.password` | 123456 | 数据库密码 |
| `spring.jpa.hibernate.ddl-auto` | update | 自动建表/更新表结构 |
| `spring.data.redis.host` | localhost | Redis 地址 |
| `spring.data.redis.port` | 6379 | Redis 端口 |
| `jwt.secret` | tyut-mall-secret-key-... | JWT 签名密钥 |
| `jwt.expiration` | 86400000 | Token 有效期（24h） |

### 启动命令

```bash
# 1. 确保 MySQL 和 Redis 已启动
# 2. 创建数据库（应用也会自动创建）
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS mall DEFAULT CHARACTER SET utf8mb4;"

# 3. 启动后端
cd backend/mall
mvn spring-boot:run
```

### 演示账号

启动后自动初始化：

| 字段 | 值 |
|------|-----|
| 用户名 | `demo@mall.test` |
| 密码 | `123456` |
| 昵称 | 演示会员 |
| 等级 | 黄金会员 |
| 余额 | 688.00 |
| 积分 | 2680 |
| 礼品卡 | 120.00 |
