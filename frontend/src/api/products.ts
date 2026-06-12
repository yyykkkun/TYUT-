import { get } from '@/api/request'
import type { Product, Category } from '@/types/domain'
import { products as mockProducts, categories as mockCategories } from '@/data/mock'

interface ProductPage {
  products: Product[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

// 是否使用 mock（后端不可用时自动降级）
let useMock = false

import { isBackendReachable } from '@/api/request'

function shouldSkipMock(e: unknown): boolean {
  if (!isMockSession()) return true
  if (e instanceof Error && e.message === 'Backend unreachable') return false
  if (e instanceof Error && e.message.includes('未登录')) return true
  return false
}

function fallback<T, Args extends unknown[] = []>(
  realCall: (...args: Args) => Promise<T>,
  mockData: T,
): (...args: Args) => Promise<T> {
  return async (...args: Args) => {
    if (useMock) return mockData
    try {
      const result = await realCall(...args)
      return result
    } catch (e) {
      if (shouldSkipMock(e)) throw e
      useMock = true
      console.warn('后端不可用，降级为本地 mock 数据')
      return mockData
    }
  }
}

export const fetchProducts = fallback(
  (params?: Record<string, any>) => get<ProductPage>('/products', params),
  { products: mockProducts, total: mockProducts.length, page: 1, pageSize: 20, totalPages: 1 },
)

export function fetchProductDetail(id: string): Promise<Product> {
  if (useMock) {
    const p = mockProducts.find((p) => p.id === id)
    return Promise.resolve(p as Product)
  }
  return get<Product>(`/products/${id}`).catch((e) => {
    if (isAuthError(e)) throw e
    useMock = true
    const p = mockProducts.find((p) => p.id === id)
    return p as Product
  })
}

export const fetchHotProducts = fallback(
  () => get<Product[]>('/products/hot'),
  [...mockProducts].sort((a, b) => b.popularity - a.popularity).slice(0, 4),
)

export const fetchLatestProducts = fallback(
  () => get<Product[]>('/products/latest'),
  [...mockProducts]
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    .reverse()
    .slice(0, 4),
)

export const fetchSpecialProducts = fallback(
  () => get<Product[]>('/products/special'),
  mockProducts.filter((p) => p.promotion === 'special'),
)

export const fetchRecommendedProducts = fallback(
  () => get<Product[]>('/products/recommended'),
  [...mockProducts].sort((a, b) => b.popularity - a.popularity).slice(0, 6),
)

export const fetchCategories = fallback(() => get<Category[]>('/categories'), mockCategories)

export const fetchSeckillProducts = fallback(
  () => get<Product[]>('/promotions/seckill'),
  mockProducts.filter((p) => p.promotion === 'seckill'),
)

export const fetchGroupBuyProducts = fallback(
  () => get<Product[]>('/promotions/group-buy'),
  mockProducts.filter((p) => p.promotion === 'group-buy'),
)

export interface ProductReview {
  id: string
  orderId: string
  userName: string
  userAvatar?: string
  rating: number
  content: string
  images: string[]
  createdAt?: string
}

const mockReviews: Record<string, ProductReview[]> = {
  p1001: [{ id: 'r1', orderId: 'SO20260601001', userName: '李同学', rating: 5, content: '蓝莓很新鲜，包装也很用心，冷链运输到家还是冰的。', images: [], createdAt: '2026-06-02 10:30' }],
  p1002: [{ id: 'r2', orderId: 'SO20260601002', userName: '王同学', rating: 5, content: '降噪效果很好，地铁上几乎听不到噪音，续航也够用。', images: [], createdAt: '2026-06-03 14:20' }],
}

export async function fetchProductReviews(productId: string): Promise<ProductReview[]> {
  try {
    return await get<ProductReview[]>(`/products/${productId}/reviews`)
  } catch {
    return mockReviews[productId] || []
  }
}
