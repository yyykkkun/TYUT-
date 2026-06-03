# 后端落地方案

后端负责电商平台的业务规则、数据一致性、接口服务、认证授权、交易流程、库存控制、营销计算、物流与数据分析。当前目录暂无后端工程，建议优先搭建单体服务，后续再按业务压力拆分服务。

## 推荐技术栈

- Java 21 / Java 17
- Spring Boot 3
- Spring Web
- Spring Security + JWT
- MyBatis-Plus 或 Spring Data JPA
- MySQL 8
- Redis
- Flyway 或 Liquibase
- springdoc-openapi
- JUnit 5

如果课程或团队要求使用其他后端技术，可保持接口、表结构和业务边界不变，仅替换实现框架。

## 分层结构建议

```text
src/main/java/com/example/mall/
  common/              # 统一响应、异常、错误码、分页、工具
  config/              # 安全、Redis、跨域、OpenAPI 配置
  auth/                # 登录注册、Token、当前用户
  user/                # 用户与会员资料
  product/             # 商品、分类、品牌、SKU、库存
  cart/                # 购物车
  order/               # 订单、订单明细、订单状态流转
  payment/             # 支付、退款预留
  promotion/           # 优惠券、秒杀、团购、满赠、套餐
  logistics/           # 配送方式、物流轨迹、门店自提
  review/              # 评论与晒单
  message/             # 站内信
  analytics/           # 浏览记录、访问日志、报表
  recommendation/      # 推荐结果读取
```

每个业务包内部建议按 `controller / service / repository / entity / dto` 拆分。

## 数据库表设计

### 用户与会员

| 表 | 用途 |
| --- | --- |
| `user` | 账号、手机号、邮箱、密码摘要、状态、注册来源 |
| `member_profile` | 昵称、头像、性别、生日、会员等级、成长值 |
| `member_account` | 余额、积分、礼品卡余额 |
| `member_account_txn` | 余额、积分变动流水 |
| `address` | 收货地址 |

### 商品

| 表 | 用途 |
| --- | --- |
| `category` | 商品分类树 |
| `brand` | 品牌 |
| `product` | SPU 主商品 |
| `sku` | SKU、规格、价格、库存、销量 |
| `product_image` | 商品图片 |
| `product_attr` | 商品属性 |
| `sku_attr` | SKU 规格属性 |

### 交易

| 表 | 用途 |
| --- | --- |
| `cart_item` | 购物车 |
| `order` | 订单主表 |
| `order_item` | 订单商品明细 |
| `payment` | 支付流水 |
| `delivery` | 配送信息 |
| `delivery_trace` | 物流轨迹 |

### 营销

| 表 | 用途 |
| --- | --- |
| `coupon` | 优惠券模板 |
| `coupon_user` | 用户优惠券 |
| `promotion` | 活动主表，包含秒杀、团购、满赠、套餐 |
| `promotion_item` | 活动商品 |
| `gift_card` | 礼品卡 |
| `gift_card_txn` | 礼品卡流水 |

### 内容与数据

| 表 | 用途 |
| --- | --- |
| `review` | 评论 |
| `review_image` | 晒单图片 |
| `browse_history` | 商品浏览记录 |
| `site_message` | 站内信 |
| `access_log` | 访问日志 |
| `recommendation` | 离线推荐结果 |

## 订单状态

建议订单状态使用明确枚举：

| 状态 | 含义 |
| --- | --- |
| `PENDING_PAYMENT` | 待付款 |
| `PAID` | 已付款 |
| `PENDING_SHIPMENT` | 待发货 |
| `SHIPPED` | 已发货 |
| `COMPLETED` | 已完成 |
| `CANCELLED` | 已取消 |
| `REFUNDED` | 已退款，预留 |

状态流转：

```text
PENDING_PAYMENT -> PAID -> PENDING_SHIPMENT -> SHIPPED -> COMPLETED
PENDING_PAYMENT -> CANCELLED
PAID -> REFUNDED
```

## 核心接口

### 认证

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/auth/register` | 手机号/邮箱注册 |
| `POST` | `/api/auth/login` | 登录 |
| `GET` | `/api/auth/me` | 当前登录用户 |
| `POST` | `/api/auth/logout` | 退出登录 |

### 商品

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/categories` | 分类树 |
| `GET` | `/api/products` | 商品搜索、筛选、排序、分页 |
| `GET` | `/api/products/{id}` | 商品详情 |
| `GET` | `/api/products/{id}/recommendations` | 看了又看 |
| `POST` | `/api/products/{id}/browse` | 记录浏览 |

### 购物车与订单

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/cart` | 购物车列表 |
| `POST` | `/api/cart/items` | 加入购物车 |
| `PUT` | `/api/cart/items/{id}` | 修改数量/选中状态 |
| `DELETE` | `/api/cart/items/{id}` | 删除购物车项 |
| `POST` | `/api/orders/preview` | 订单试算 |
| `POST` | `/api/orders` | 创建订单 |
| `GET` | `/api/orders` | 订单列表 |
| `GET` | `/api/orders/{id}` | 订单详情 |
| `POST` | `/api/orders/{id}/cancel` | 取消订单 |
| `POST` | `/api/orders/{id}/confirm` | 确认收货 |

### 支付与物流

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/payments` | 发起/模拟支付 |
| `GET` | `/api/payments/{id}` | 支付结果 |
| `GET` | `/api/orders/{id}/delivery` | 物流信息 |
| `POST` | `/api/orders/{id}/delivery` | 后台发货，MVP 可先简化 |

### 会员与营销

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/member/profile` | 会员资料 |
| `PUT` | `/api/member/profile` | 修改资料 |
| `GET` | `/api/member/account` | 余额、积分 |
| `GET` | `/api/member/coupons` | 用户优惠券 |
| `GET` | `/api/member/gift-cards` | 礼品卡 |
| `GET` | `/api/promotions/seckill` | 秒杀活动 |
| `GET` | `/api/promotions/group-buy` | 团购活动 |

### 评价、消息、数据

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/reviews` | 提交评价晒单 |
| `GET` | `/api/reviews/product/{productId}` | 商品评价 |
| `GET` | `/api/messages` | 站内信 |
| `POST` | `/api/messages/{id}/read` | 标记已读 |
| `POST` | `/api/analytics/events` | 采集访问事件 |
| `GET` | `/api/analytics/summary` | PV/UV/IP 基础报表 |
| `GET` | `/api/recommendations` | 猜你喜欢 |

## 关键业务规则

### 注册登录

- 手机号和邮箱都必须唯一。
- 密码必须加盐哈希存储，禁止明文保存。
- 匿名购买时可根据手机号创建默认账号，首次登录后引导修改密码。

### 商品与库存

- 商品详情展示 SKU 库存，库存小于等于 0 不允许购买。
- 商品列表默认只展示上架商品。
- 下单时后端重新读取 SKU 价格、库存、活动规则，前端传入价格仅作展示参考。

### 下单

- 创建订单必须校验用户、地址、商品状态、SKU 库存、优惠券有效期、积分余额。
- 订单创建与库存扣减必须在同一事务内完成。
- 重复提交通过 `idempotencyKey` 或业务请求号处理。
- 待付款订单超过超时时间应自动取消并释放库存，MVP 可通过定时任务实现。

### 支付

- MVP 阶段可实现模拟支付，支付成功后写入 `payment` 并更新订单状态。
- 平台余额支付需要校验余额并写账户流水。
- 礼品卡支付需要校验卡状态、余额和适用范围。

### 营销

- 优惠券、积分、礼品卡的抵扣结果由 `/api/orders/preview` 返回。
- 秒杀库存建议使用 Redis 预扣，数据库最终落单。
- 活动开始前可展示“即将开始”，但不允许提交活动价订单。

### 推荐与报表

- 浏览记录实时写入 `browse_history`。
- PV/UV/IP 可先基于 `access_log` 聚合查询。
- 推荐接口优先读取 `recommendation` 表或 Redis 缓存，未命中时返回热卖商品。
- README 中提到“离线推荐 20ms 内返回”，可通过预计算结果 + Redis 缓存达成。

## 缓存建议

| 数据 | 缓存策略 |
| --- | --- |
| 分类树 | Redis 缓存，后台修改后失效 |
| 商品详情 | 热点商品缓存，短 TTL |
| 秒杀库存 | Redis 原子扣减 |
| 推荐结果 | 用户/商品维度缓存 |
| 登录会话 | JWT 无状态或 Redis Session |

## 开发顺序

1. 初始化 Spring Boot 工程、统一响应、异常处理、数据库迁移。
2. 实现认证、用户、地址。
3. 实现分类、商品、SKU、商品搜索。
4. 实现购物车、订单试算、订单创建、库存扣减。
5. 实现模拟支付、订单状态、物流轨迹。
6. 实现会员账户、积分、优惠券、礼品卡。
7. 实现秒杀/团购、评价晒单、站内信。
8. 实现访问日志、报表、推荐结果读取。

## 测试重点

- 注册登录：手机号/邮箱重复、密码错误、Token 失效。
- 商品：下架商品不可购买、库存不足不可购买。
- 订单：重复提交、价格变化、优惠券过期、积分不足。
- 支付：余额不足、重复支付、支付成功后状态流转。
- 秒杀：并发扣库存不超卖。
- 数据：访问日志可正确统计 PV/UV/IP。

