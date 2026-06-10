export type OrderStatus = 'pending_payment' | 'paid' | 'shipping' | 'completed' | 'cancelled'

export type DeliveryMethod = 'platform' | 'third-party' | 'pickup'

export interface Category {
  id: string
  name: string
}

export interface Product {
  id: string
  title: string
  subtitle: string
  category: string
  brand: string
  origin: string
  city: string
  tags: string[]
  price: number
  originalPrice?: number
  stock: number
  sales: number
  popularity: number
  rating: number
  image: string
  specs: string[]
  createdAt: string
  promotion?: 'seckill' | 'group-buy' | 'special'
}

export interface CartItem {
  id: string
  productId: string
  spec: string
  quantity: number
  selected: boolean
}

export interface Address {
  id: string
  name: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault: boolean
}

export interface Coupon {
  id: string
  title: string
  threshold: number
  amount: number
  expiresAt: string
  used: boolean
}

export interface OrderItem {
  productId: string
  title: string
  image: string
  spec: string
  price: number
  quantity: number
}

export interface Order {
  id: string
  status: OrderStatus
  createdAt: string
  items: OrderItem[]
  address: Address
  deliveryMethod: DeliveryMethod
  couponAmount: number
  pointsUsed: number
  giftCardAmount: number
  freight: number
  total: number
  paidAt?: string
  deliveryNo?: string
  review?: string
}

export interface Message {
  id: string
  title: string
  body: string
  type: 'price' | 'promotion' | 'order'
  read: boolean
  createdAt: string
}
