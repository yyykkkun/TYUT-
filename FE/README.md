# 前端落地方案

前端基于 `Vue 3 + Vite + TypeScript + Pinia + Vue Router` 建设买家端电商应用。第一阶段目标是完成可演示的购物闭环，后续逐步扩展营销、会员、数据和推荐能力。

## 页面路由

| 路由 | 页面 | 说明 |
| --- | --- | --- |
| `/` | 首页 | 分类入口、搜索框、最新/热卖/特卖/推荐商品 |
| `/login` | 登录注册 | 手机号/邮箱登录注册，支持匿名购买转账号 |
| `/products` | 商品列表 | 分类、属性、库存、产地、城市、品牌、价格、销量、人气筛选排序 |
| `/products/:id` | 商品详情 | 商品图片、规格、库存、价格、立即购买、加入购物车、分享 |
| `/cart` | 购物车 | 商品勾选、数量修改、删除、结算 |
| `/checkout` | 订单确认 | 地址、配送、优惠券、积分、礼品卡、金额明细 |
| `/orders` | 订单列表 | 全部、待付款、待发货、待收货、已完成 |
| `/orders/:id` | 订单详情 | 商品明细、支付信息、物流轨迹、确认收货 |
| `/member` | 会员中心 | 个人信息、会员等级、余额、积分、优惠券、礼品卡 |
| `/member/addresses` | 地址管理 | 新增、编辑、删除、设为默认 |
| `/member/browse-history` | 浏览记录 | 近期浏览商品 |
| `/member/reviews` | 评论晒单 | 待评价、已评价、图片晒单 |
| `/promotions/seckill` | 秒杀 | 活动商品、倒计时、剩余库存 |
| `/promotions/group-buy` | 团购 | 团购商品、参团人数、活动状态 |
| `/points-mall` | 积分商城 | 积分兑换优惠券或礼品 |
| `/messages` | 站内信 | 降价通知、活动通知 |

## 目录结构建议

```text
src/
  api/                 # 接口封装
  assets/              # 静态资源
  components/          # 通用组件
  composables/         # 组合式逻辑
  layouts/             # 页面布局
  router/              # 路由配置
  stores/              # Pinia 状态
  styles/              # 全局样式
  types/               # TypeScript 类型
  utils/               # 工具函数
  views/               # 页面组件
```

## 状态管理

| Store | 职责 |
| --- | --- |
| `useAuthStore` | 登录态、用户信息、Token、退出登录 |
| `useProductStore` | 分类、筛选条件、商品列表缓存、商品详情 |
| `useCartStore` | 购物车数量、选中项、价格汇总 |
| `useOrderStore` | 订单确认信息、订单列表、订单详情 |
| `useMemberStore` | 会员等级、余额、积分、优惠券、礼品卡 |
| `useMessageStore` | 站内信未读数、消息列表 |

## 接口封装

建议在 `src/api` 中按领域拆分：

```text
api/
  request.ts
  auth.ts
  products.ts
  cart.ts
  orders.ts
  payments.ts
  member.ts
  promotions.ts
  logistics.ts
  analytics.ts
```

`request.ts` 负责：

- 配置 `baseURL`。
- 自动携带登录凭证。
- 统一处理 `code/message/data` 响应。
- 处理 401 跳转登录。
- 处理网络错误、业务错误、重复提交提示。

## 关键页面落地

### 首页

- 顶部搜索、分类导航、购物车入口、会员入口。
- 商品区块：最新商品、特卖商品、热卖商品、猜你喜欢。
- 未登录时猜你喜欢可使用热门商品兜底。

### 商品列表

- 左侧或顶部筛选：分类、品牌、属性、库存、产地、城市、价格区间。
- 排序：综合、价格、销量、人气、最新。
- 支持分页、空状态、加载骨架屏。
- 筛选条件同步到 URL Query，便于刷新和分享。

### 商品详情

- 展示 SPU 信息与 SKU 规格。
- 选择规格后更新价格、库存、活动价。
- 支持立即购买、加入购物车、分享、浏览记录上报。
- 推荐区展示“浏览该商品的人还浏览了”。

### 订单确认

- 地址选择/新增。
- 配送方式选择：平台物流、第三方物流、门店自提。
- 优惠券、积分、礼品卡抵扣。
- 金额明细由后端试算接口返回，前端只展示不自行决定最终价格。

### 订单中心

- 按订单状态切换。
- 待付款可继续支付或取消。
- 待收货可查看物流、确认收货。
- 已完成可评价晒单。

### 会员中心

- 个人信息、会员等级、成长值。
- 余额、积分、优惠券、礼品卡。
- 浏览记录、订单记录、评论晒单、站内信。

## 前端接口清单

| 能力 | 方法与路径 | 用途 |
| --- | --- | --- |
| 登录 | `POST /api/auth/login` | 手机/邮箱登录 |
| 注册 | `POST /api/auth/register` | 创建账号 |
| 当前用户 | `GET /api/auth/me` | 恢复登录态 |
| 商品列表 | `GET /api/products` | 搜索、筛选、排序、分页 |
| 商品详情 | `GET /api/products/:id` | 商品详情 |
| 商品分类 | `GET /api/categories` | 分类树 |
| 加入购物车 | `POST /api/cart/items` | 添加商品 |
| 购物车 | `GET /api/cart` | 获取购物车 |
| 更新购物车 | `PUT /api/cart/items/:id` | 修改数量或选中状态 |
| 地址列表 | `GET /api/member/addresses` | 获取地址 |
| 订单试算 | `POST /api/orders/preview` | 计算优惠、运费、应付金额 |
| 创建订单 | `POST /api/orders` | 提交订单 |
| 订单列表 | `GET /api/orders` | 查询订单 |
| 订单详情 | `GET /api/orders/:id` | 查询订单详情 |
| 支付订单 | `POST /api/payments` | 模拟支付 |
| 物流轨迹 | `GET /api/orders/:id/delivery` | 查看物流 |
| 确认收货 | `POST /api/orders/:id/confirm` | 完成订单 |
| 评价商品 | `POST /api/reviews` | 评价晒单 |
| 优惠券 | `GET /api/member/coupons` | 查询可用优惠券 |
| 推荐商品 | `GET /api/recommendations` | 猜你喜欢 |

## 组件拆分

- `ProductCard`：商品卡片。
- `ProductFilter`：商品筛选。
- `SkuSelector`：规格选择。
- `PriceText`：价格展示。
- `QuantityStepper`：数量选择。
- `AddressSelector`：地址选择。
- `OrderStatusTag`：订单状态。
- `PaymentPanel`：支付方式。
- `CouponPicker`：优惠券选择。
- `CountdownTimer`：秒杀/团购倒计时。
- `EmptyState`：空状态。

## 开发顺序

1. 完成路由、布局、请求封装、登录态。
2. 完成首页、商品列表、商品详情。
3. 完成购物车、订单确认、订单列表、订单详情。
4. 完成会员中心、地址、积分、优惠券。
5. 完成促销活动、评价晒单、站内信、推荐与报表展示。

## 本地运行

```bash
npm install
npm run dev
```

