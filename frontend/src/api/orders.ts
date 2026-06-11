import { get, post, put } from '@/api/request'
import type { Order, OrderItem, Address } from '@/types/domain'
import {
  addresses as mockAddresses,
  products as mockProducts,
  coupons as mockCoupons,
} from '@/data/mock'

export interface OrderPreview {
  subtotal: number
  couponAmount: number
  pointsAmount: number
  giftCardAmount: number
  freight: number
  total: number
}

export interface OrderCreateParams {
  couponId?: string | null
  usePoints?: boolean
  useGiftCard?: boolean
  deliveryMethod?: string
}

const STORAGE_KEY = 'mall-mock-orders'
const CART_KEY = 'mall-mock-cart'
let useMock = false

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
  } catch {
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
  } catch {
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
    const pointsAmount = params.usePoints ? 10 : 0
    const giftCardAmount = params.useGiftCard ? 20 : 0
    const freight = params.deliveryMethod === 'pickup' ? 0 : 8
    const total = Math.max(0, subtotal - couponAmount - pointsAmount - giftCardAmount + freight)
    return { subtotal, couponAmount, pointsAmount, giftCardAmount, freight, total }
  }
  try {
    return await post<OrderPreview>('/orders/preview', params)
  } catch {
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
    const pointsAmount = params.usePoints ? 10 : 0
    const giftCardAmount = params.useGiftCard ? 20 : 0
    const freight = params.deliveryMethod === 'pickup' ? 0 : 8
    const total = Math.max(0, subtotal - couponAmount - pointsAmount - giftCardAmount + freight)

    // 标记优惠券已使用
    if (coupon) coupon.used = true

    const id = `SO${todayStr().replace(/-/g, '')}${String(Date.now()).slice(-6)}`
    const order: Order = {
      id,
      status: 'pending_payment',
      createdAt: now(),
      items: orderItems,
      address,
      deliveryMethod: (params.deliveryMethod as any) || 'platform',
      couponAmount,
      pointsUsed: params.usePoints ? 10 : 0,
      giftCardAmount,
      freight,
      total,
    }

    const orders = loadOrders()
    orders.unshift(order)
    saveOrders(orders)

    // 清除购物车中已选商品
    const remaining = cart.filter((i) => !i.selected)
    saveCartItems(remaining)

    return id
  }
  try {
    return await post<string>('/orders', params)
  } catch {
    useMock = true
    return createOrder(params)
  }
}

export async function payOrder(id: string): Promise<void> {
  if (useMock) {
    const orders = loadOrders()
    const order = orders.find((o) => o.id === id)
    if (!order) throw new Error('订单不存在')
    order.status = 'paid'
    order.paidAt = now()
    order.deliveryNo = `SF${String(Date.now()).slice(-10)}`
    saveOrders(orders)
    return
  }
  try {
    return await put<void>(`/orders/${id}/pay`)
  } catch {
    useMock = true
    return payOrder(id)
  }
}

export async function cancelOrder(id: string): Promise<void> {
  if (useMock) {
    const orders = loadOrders()
    const order = orders.find((o) => o.id === id)
    if (!order) throw new Error('订单不存在')
    order.status = 'cancelled'
    saveOrders(orders)
    return
  }
  try {
    return await put<void>(`/orders/${id}/cancel`)
  } catch {
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
    return
  }
  try {
    return await put<void>(`/orders/${id}/confirm`)
  } catch {
    useMock = true
    return confirmOrder(id)
  }
}

export async function reviewOrder(id: string, review: string): Promise<void> {
  if (useMock) {
    const orders = loadOrders()
    const order = orders.find((o) => o.id === id)
    if (!order) throw new Error('订单不存在')
    order.review = review
    saveOrders(orders)
    return
  }
  try {
    return await put<void>(`/orders/${id}/review`, { review })
  } catch {
    useMock = true
    return reviewOrder(id, review)
  }
}
