import { get, post, put, patch, del } from '@/api/request'
import type { CartItem, Product } from '@/types/domain'
import { products as mockProducts } from '@/data/mock'

export interface CartItemWithProduct extends CartItem {
  product?: Product
}

const STORAGE_KEY = 'mall-mock-cart'
let useMock = false

import { isBackendReachable } from '@/api/request'

function shouldSkipMock(e: unknown): boolean {
  if (!isMockSession()) return true
  if (e instanceof Error && e.message === 'Backend unreachable') return false
  if (e instanceof Error && e.message.includes('未登录')) return true
  return false
}

// ===== localStorage 作为 mock 数据的唯一数据源 =====

function loadMockCart(): CartItemWithProduct[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    const items: CartItemWithProduct[] = raw ? JSON.parse(raw) : []
    // 始终重新挂载 product 信息（确保 price/title/image 等字段完整）
    for (const item of items) {
      item.product =
        mockProducts.find((p) => p.id === item.productId) ||
        mockProducts.find((p) => p.id === item.productId.replace(/^p/, '')) ||
        mockProducts.find((p) => p.id === `p${item.productId}`) ||
        item.product // 保留原有（可能来自后端）
    }
    return items
  } catch {
    return []
  }
}

function saveMockCart(items: CartItemWithProduct[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(items))
}

// ===== API 函数 =====

export async function fetchCart(): Promise<CartItemWithProduct[]> {
  if (useMock) return loadMockCart()
  try {
    return await get<CartItemWithProduct[]>('/cart')
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    console.warn('后端不可用，降级为本地 mock 数据')
    return loadMockCart()
  }
}

export async function addToCart(productId: string, spec: string, quantity = 1): Promise<void> {
  if (useMock) {
    const cart = loadMockCart()
    const existing = cart.find((i) => i.productId === productId && i.spec === spec)
    if (existing) {
      existing.quantity += quantity
    } else {
      cart.push({
        id: `cart${Date.now()}`,
        productId,
        spec,
        quantity,
        selected: true,
        product: mockProducts.find((p) => p.id === productId),
      })
    }
    saveMockCart(cart)
    return
  }
  try {
    return await post<void>('/cart', { productId: Number(productId), spec, quantity })
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    return addToCart(productId, spec, quantity)
  }
}

export async function updateCartQuantity(id: string, quantity: number): Promise<void> {
  if (useMock) {
    const cart = loadMockCart()
    const item = cart.find((i) => i.id === id)
    if (item) item.quantity = quantity
    saveMockCart(cart)
    return
  }
  try {
    return await put<void>(`/cart/${id}`, { quantity })
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    return updateCartQuantity(id, quantity)
  }
}

export async function toggleCartSelect(id: string): Promise<void> {
  if (useMock) {
    const cart = loadMockCart()
    const item = cart.find((i) => i.id === id)
    if (item) item.selected = !item.selected
    saveMockCart(cart)
    return
  }
  try {
    return await patch<void>(`/cart/${id}/select`)
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    return toggleCartSelect(id)
  }
}

export async function removeCartItem(id: string): Promise<void> {
  if (useMock) {
    const cart = loadMockCart()
    const idx = cart.findIndex((i) => i.id === id)
    if (idx >= 0) cart.splice(idx, 1)
    saveMockCart(cart)
    return
  }
  try {
    return await del<void>(`/cart/${id}`)
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    return removeCartItem(id)
  }
}

export async function clearSelectedCart(): Promise<void> {
  if (useMock) {
    const cart = loadMockCart()
    const filtered = cart.filter((i) => !i.selected)
    saveMockCart(filtered)
    return
  }
  try {
    return await del<void>('/cart/selected')
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    return clearSelectedCart()
  }
}
