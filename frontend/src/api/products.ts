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

function fallback<T, Args extends unknown[] = []>(
  realCall: (...args: Args) => Promise<T>,
  mockData: T,
): (...args: Args) => Promise<T> {
  return async (...args: Args) => {
    if (useMock) return mockData
    try {
      const result = await realCall(...args)
      return result
    } catch {
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
  return get<Product>(`/products/${id}`).catch(() => {
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
