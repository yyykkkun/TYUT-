import { get, post, put } from '@/api/request'
import type { Order, OrderItem, Address } from '@/types/domain'
import {
  addresses as mockAddresses,
  products as mockProducts,
  coupons as mockCoupons,
} from '@/data/mock'
import { loadProfile, saveProfile, addBalanceRecord, addPointsRecord } from '@/api/member'
import { addMockMessage } from '@/api/messages'

export interface OrderPreview {
  subtotal: number
  couponAmount: number
  pointsAmount: number
  giftCardAmount: number
  balanceAmount: number
  memberDiscount: number
  freight: number
  total: number
}

export interface OrderCreateParams {
  couponId?: string | null
  usePoints?: boolean
  useGiftCard?: boolean
  useBalance?: boolean
  deliveryMethod?: string
}

const STORAGE_KEY = 'mall-mock-orders'
const CART_KEY = 'mall-mock-cart'
let useMock = false

import { isBackendReachable } from '@/api/request'

function shouldSkipMock(e: unknown): boolean {
  if (!isMockSession()) return true
  if (e instanceof Error && e.message === 'Backend unreachable') return false
  if (e instanceof Error && e.message.includes('未登录')) return true
  return false
}

// ===== localStorage 持久化 =====

function loadOrders(): Order[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function saveOrders(orders: Order[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(orders))
}

function loadCartItems(): {
  productId: string
  spec: string
  quantity: number
  selected: boolean
  product?: any
}[] {
  try {
    const raw = localStorage.getItem(CART_KEY)
    const items: any[] = raw ? JSON.parse(raw) : []
    // 重新挂载 product 信息（与 cart.ts 的 loadMockCart 保持一致）
    for (const item of items) {
      item.product =
        mockProducts.find((p: any) => p.id === item.productId) ||
        mockProducts.find((p: any) => p.id === (item.productId || '').replace(/^p/, '')) ||
        mockProducts.find((p: any) => p.id === `p${item.productId}`) ||
        item.product
    }
    return items
  } catch {
    return []
  }
}

function saveCartItems(items: any[]) {
  localStorage.setItem(CART_KEY, JSON.stringify(items))
}

function now(): string {
  return new Date().toISOString().replace('T', ' ').slice(0, 19)
}

function todayStr(): string {
  return new Date().toISOString().slice(0, 10)
}

// ===== API 函数 =====

export async function fetchOrders(status = 'all', page = 1, pageSize = 20): Promise<Order[]> {
  if (useMock) {
    const all = loadOrders()
    if (status === 'all') return all
    return all.filter((o) => o.status === status)
  }
  try {
    return await get<Order[]>('/orders', { status, page, pageSize })
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    console.warn('后端不可用，降级为本地 mock 数据')
    const all = loadOrders()
    if (status === 'all') return all
    return all.filter((o) => o.status === status)
  }
}

export async function fetchOrderDetail(id: string): Promise<Order> {
  if (useMock) {
    const order = loadOrders().find((o) => o.id === id)
    if (!order) throw new Error('订单不存在')
    return order
  }
  try {
    return await get<Order>(`/orders/${id}`)
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    const order = loadOrders().find((o) => o.id === id)
    if (!order) throw new Error('订单不存在')
    return order
  }
}

export async function previewOrder(params: OrderCreateParams): Promise<OrderPreview> {
  if (useMock) {
    const cart = loadCartItems().filter((i) => i.selected)
    const subtotal = cart.reduce((sum, i) => sum + (i.product?.price || 0) * i.quantity, 0)
    const coupon = params.couponId ? mockCoupons.find((c) => c.id === params.couponId) : null
    // 检查门槛
    const couponAmount = coupon && !coupon.used && subtotal >= coupon.threshold ? coupon.amount : 0

    // 积分抵扣：100分=1元，最多抵扣20元（2000分）
    const profile = loadProfile()
    let pointsAmount = 0
    if (params.usePoints && profile.points > 0) {
      const maxYuanByPoints = Math.min(Math.floor(profile.points / 100), 20)
      const maxYuanBySubtotal = subtotal - couponAmount
      pointsAmount = Math.max(0, Math.min(maxYuanByPoints, maxYuanBySubtotal))
    }

    // 礼品卡：最多抵扣50元
    let giftCardAmount = 0
    if (params.useGiftCard && profile.giftCard > 0) {
      const remaining = subtotal - couponAmount - pointsAmount
      const maxCard = Math.min(profile.giftCard, 50)
      giftCardAmount = Math.max(0, Math.min(maxCard, remaining))
    }

    // 运费
    // 会员等级折扣
    let memberDiscount = 0
    const level = profile.level || '普通会员'
    if (level === '钻石会员') memberDiscount = Math.round(subtotal * 0.05 * 100) / 100
    else if (level === '黄金会员') memberDiscount = Math.round(subtotal * 0.03 * 100) / 100
    else if (level === '白银会员') memberDiscount = Math.round(subtotal * 0.02 * 100) / 100

    const freight = params.deliveryMethod === 'pickup' ? 0 : 8

    // 抵扣顺序: 优惠券 → 积分 → 礼品卡 → 会员折扣 → 运费 → 余额
    const beforeBalance = Math.max(0, subtotal - couponAmount - pointsAmount - giftCardAmount - memberDiscount + freight)

    let balanceAmount = 0
    if (params.useBalance && profile.balance > 0) {
      balanceAmount = Math.min(profile.balance, beforeBalance)
    }

    const total = Math.max(0, beforeBalance - balanceAmount)
    return { subtotal, couponAmount, pointsAmount, giftCardAmount, balanceAmount, memberDiscount, freight, total }
  }
  try {
    return await post<OrderPreview>('/orders/preview', params)
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    return previewOrder(params)
  }
}

export async function createOrder(params: OrderCreateParams): Promise<string> {
  if (useMock) {
    const cart = loadCartItems()
    const selected = cart.filter((i) => i.selected)
    if (!selected.length) throw new Error('没有待结算商品')

    const orderItems: OrderItem[] = selected.map((i) => ({
      productId: i.productId,
      title: i.product?.title || `商品 ${i.productId}`,
      image: i.product?.image || '',
      spec: i.spec,
      price: i.product?.price || 0,
      quantity: i.quantity,
    }))

    const address: Address = mockAddresses[0] ?? {
      id: 'fallback-address',
      name: '默认收货人',
      phone: '13800000000',
      province: '山西省',
      city: '太原市',
      district: '小店区',
      detail: '校园服务中心',
      isDefault: true,
    }

    const subtotal = orderItems.reduce((sum, i) => sum + i.price * i.quantity, 0)
    const coupon = params.couponId ? mockCoupons.find((c) => c.id === params.couponId) : null
    const couponAmount = coupon && !coupon.used && subtotal >= coupon.threshold ? coupon.amount : 0

    // 积分抵扣：100分=1元，最多抵扣20元（2000分），与后端逻辑一致
    const profile = loadProfile()
    let pointsAmount = 0
    let pointsUsed = 0
    if (params.usePoints && profile.points > 0) {
      const maxYuanByPoints = Math.min(Math.floor(profile.points / 100), 20)
      const maxYuanBySubtotal = subtotal - couponAmount
      pointsAmount = Math.max(0, Math.min(maxYuanByPoints, maxYuanBySubtotal))
      pointsUsed = pointsAmount * 100 // 积分消耗 = 抵扣金额 × 100
    }

    // 礼品卡：最多抵扣50元
    let giftCardAmount = 0
    if (params.useGiftCard && profile.giftCard > 0) {
      const remaining = subtotal - couponAmount - pointsAmount
      const maxCard = Math.min(profile.giftCard, 50)
      giftCardAmount = Math.max(0, Math.min(maxCard, remaining))
    }

    // 会员等级折扣
    let memberDiscount = 0
    const level = profile.level || '普通会员'
    if (level === '钻石会员') memberDiscount = Math.round(subtotal * 0.05 * 100) / 100
    else if (level === '黄金会员') memberDiscount = Math.round(subtotal * 0.03 * 100) / 100
    else if (level === '白银会员') memberDiscount = Math.round(subtotal * 0.02 * 100) / 100

    const freight = params.deliveryMethod === 'pickup' ? 0 : 8

    // 抵扣顺序: 优惠券 → 积分 → 礼品卡 → 会员折扣 → 运费 → 余额
    const beforeBalance = Math.max(0, subtotal - couponAmount - pointsAmount - giftCardAmount - memberDiscount + freight)
    let balanceAmount = 0
    if (params.useBalance && profile.balance > 0) {
      balanceAmount = Math.min(profile.balance, beforeBalance)
    }

    const total = Math.max(0, beforeBalance - balanceAmount)

    // 标记优惠券已使用
    if (coupon) coupon.used = true

    // 真正扣减用户积分、礼品卡和余额
    profile.points = Math.max(0, profile.points - pointsUsed)
    profile.giftCard = Math.max(0, profile.giftCard - giftCardAmount)
    profile.balance = Math.max(0, profile.balance - balanceAmount)
    saveProfile(profile)
    if (balanceAmount > 0) {
      addBalanceRecord({ userId: '1', amount: -balanceAmount, type: 'order_pay', balanceAfter: profile.balance, remark: '订单支付扣除余额' })
    }
    if (pointsUsed > 0) {
      addPointsRecord({ userId: '1', amount: -pointsUsed, type: 'order_use', pointsAfter: profile.points, remark: '下单使用积分' })
    }

    const id = `SO${todayStr().replace(/-/g, '')}${String(Date.now()).slice(-6)}`
    const order: Order = {
      id,
      status: 'pending_payment',
      createdAt: now(),
      items: orderItems,
      address,
      deliveryMethod: (params.deliveryMethod as any) || 'platform',
      couponAmount,
      pointsUsed,
      giftCardAmount,
      balanceAmount,
      freight,
      total,
      paymentMethod: balanceAmount > 0 ? 'balance' : undefined,
    }

    const orders = loadOrders()
    orders.unshift(order)
    saveOrders(orders)

    // 清除购物车中已选商品
    const remaining = cart.filter((i) => !i.selected)
    saveCartItems(remaining)

    // 发送订单创建消息
    addMockMessage('订单已创建', `订单 ${id} 已创建，共 ￥${total.toFixed(2)}，请尽快支付。`, 'order')

    return id
  }
  try {
    return await post<string>('/orders', params)
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    return createOrder(params)
  }
}

export async function payOrder(id: string, paymentMethod?: string): Promise<void> {
  if (useMock) {
    const orders = loadOrders()
    const order = orders.find((o) => o.id === id)
    if (!order) throw new Error('订单不存在')

    // 余额支付：扣减余额
    if (paymentMethod === 'balance' && order.total > 0) {
      const p = loadProfile()
      if (p.balance < order.total) throw new Error('余额不足')
      p.balance -= order.total
      saveProfile(p)
      addBalanceRecord({ userId: '1', amount: -order.total, type: 'order_pay', balanceAfter: p.balance, remark: '余额支付' })
    }

    order.status = 'paid'
    order.paidAt = now()
    order.deliveryNo = `SF${String(Date.now()).slice(-10)}`
    order.paymentMethod = paymentMethod || order.paymentMethod || 'wechat'
    saveOrders(orders)
    addMockMessage('支付成功', `订单 ${order.id} 已支付，我们将尽快发货。`, 'order')
    return
  }
  try {
    return await put<void>(`/orders/${id}/pay`, { paymentMethod })
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    return payOrder(id, paymentMethod)
  }
}

export async function cancelOrder(id: string): Promise<void> {
  if (useMock) {
    const orders = loadOrders()
    const order = orders.find((o) => o.id === id)
    if (!order) throw new Error('订单不存在')
    if (order.status !== 'pending_payment') throw new Error('只有待付款订单可以取消')
    order.status = 'cancelled'

    // 恢复积分、礼品卡和余额
    const profile = loadProfile()
    if (order.pointsUsed) {
      profile.points += order.pointsUsed
      addPointsRecord({ userId: '1', amount: order.pointsUsed, type: 'order_cancel', pointsAfter: profile.points, remark: '订单取消退回积分' })
    }
    if (order.giftCardAmount) profile.giftCard += order.giftCardAmount
    if (order.balanceAmount) {
      profile.balance += order.balanceAmount
      saveProfile(profile)
      addBalanceRecord({ userId: '1', amount: order.balanceAmount, type: 'order_cancel', orderNo: order.id, balanceAfter: profile.balance, remark: '订单取消退回余额' })
    }

    saveOrders(orders)
    return
  }
  try {
    return await put<void>(`/orders/${id}/cancel`)
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    return cancelOrder(id)
  }
}

export async function confirmOrder(id: string): Promise<void> {
  if (useMock) {
    const orders = loadOrders()
    const order = orders.find((o) => o.id === id)
    if (!order) throw new Error('订单不存在')
    order.status = 'completed'
    saveOrders(orders)
    // 确认收货送积分
    const earned = order.total
    const p = loadProfile()
    p.points += earned
    saveProfile(p)
    addPointsRecord({ userId: '1', amount: earned, type: 'points_earn', pointsAfter: p.points, remark: `确认收货赠送积分（1元=1分）` })
    return
  }
  try {
    return await put<void>(`/orders/${id}/confirm`)
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    return confirmOrder(id)
  }
}

export async function reviewOrder(id: string, review: string, ratingVal: number = 5): Promise<void> {
  if (useMock) {
    const orders = loadOrders()
    const order = orders.find((o) => o.id === id)
    if (!order) throw new Error('订单不存在')
    order.review = review
    order.rating = ratingVal
    saveOrders(orders)
    return
  }
  try {
    return await put<void>(`/orders/${id}/review`, { review, rating: ratingVal })
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    return reviewOrder(id, review, ratingVal)
  }
}
