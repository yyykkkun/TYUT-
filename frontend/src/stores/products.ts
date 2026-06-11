import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import {
  fetchProducts as apiProducts,
  fetchCategories as apiCategories,
  fetchHotProducts,
  fetchLatestProducts,
  fetchSpecialProducts,
  fetchRecommendedProducts,
  fetchProductDetail,
} from '@/api/products'
import { addBrowseHistory } from '@/api/member'
import type { Product, Category } from '@/types/domain'

export type ProductSort = 'composite' | 'priceAsc' | 'priceDesc' | 'sales' | 'popularity' | 'latest'

export interface ProductFilters {
  keyword: string
  category: string
  brand: string
  stock: string
  city: string
  minPrice: number | null
  maxPrice: number | null
  sort: ProductSort
  page: number
}

// 品牌和城市从后端获取（首次加载后缓存）
const allBrands = ref<string[]>([])
const allCities = ref<string[]>([])

export const useProductStore = defineStore('products', () => {
  const products = ref<Product[]>([])
  const categories = ref<Category[]>([])
  const browseHistory = ref<string[]>([])
  const total = ref(0)
  const totalPages = ref(1)
  const loading = ref(false)

  const filters = ref<ProductFilters>({
    keyword: '',
    category: '',
    brand: '',
    stock: '',
    city: '',
    minPrice: null,
    maxPrice: null,
    sort: 'composite',
    page: 1,
  })
  const pageSize = 6

  // 首页数据
  const latestProducts = ref<Product[]>([])
  const hotProducts = ref<Product[]>([])
  const specialProducts = ref<Product[]>([])
  const recommendedProducts = ref<Product[]>([])

  const brands = computed(() => allBrands.value)
  const cities = computed(() => allCities.value)

  // 客户端筛选（用于 mock 数据场景，后端返回时数据已筛选则为近似无操作）
  const filteredProducts = computed(() => {
    let result = [...products.value]
    const f = filters.value
    if (f.keyword) {
      const kw = f.keyword.toLowerCase()
      result = result.filter(
        (p) =>
          p.title.toLowerCase().includes(kw) ||
          p.subtitle.toLowerCase().includes(kw) ||
          p.tags.some((t) => t.toLowerCase().includes(kw)),
      )
    }
    if (f.category) result = result.filter((p) => p.category === f.category)
    if (f.brand) result = result.filter((p) => p.brand === f.brand)
    if (f.stock === 'in') result = result.filter((p) => p.stock > 0)
    else if (f.stock === 'out') result = result.filter((p) => p.stock === 0)
    if (f.city) result = result.filter((p) => p.city === f.city)
    if (f.minPrice !== null) result = result.filter((p) => p.price >= f.minPrice!)
    if (f.maxPrice !== null) result = result.filter((p) => p.price <= f.maxPrice!)

    switch (f.sort) {
      case 'priceAsc':
        result.sort((a, b) => a.price - b.price)
        break
      case 'priceDesc':
        result.sort((a, b) => b.price - a.price)
        break
      case 'sales':
        result.sort((a, b) => b.sales - a.sales)
        break
      case 'popularity':
        result.sort((a, b) => b.popularity - a.popularity)
        break
      case 'latest':
        result.sort((a, b) => b.createdAt.localeCompare(a.createdAt))
        break
      default:
        result.sort((a, b) => b.popularity - a.popularity)
        break
    }
    return result
  })

  const pagedProducts = computed(() => {
    const start = (filters.value.page - 1) * pageSize
    return filteredProducts.value.slice(start, start + pageSize)
  })

  async function loadCategories() {
    if (categories.value.length) return
    categories.value = await apiCategories()
  }

  async function loadProducts() {
    loading.value = true
    try {
      const params: Record<string, any> = {
        page: filters.value.page,
        pageSize,
        sort: filters.value.sort,
      }
      if (filters.value.keyword) params.keyword = filters.value.keyword
      if (filters.value.category) params.category = filters.value.category
      if (filters.value.brand) params.brand = filters.value.brand
      if (filters.value.stock) params.stock = filters.value.stock
      if (filters.value.city) params.city = filters.value.city
      if (filters.value.minPrice !== null) params.minPrice = filters.value.minPrice
      if (filters.value.maxPrice !== null) params.maxPrice = filters.value.maxPrice

      const page = await apiProducts(params)
      products.value = page.products
      total.value = page.total
      totalPages.value = page.totalPages
    } finally {
      loading.value = false
    }
  }

  async function loadHomeData() {
    const [hot, latest, special, rec] = await Promise.all([
      fetchHotProducts(),
      fetchLatestProducts(),
      fetchSpecialProducts(),
      fetchRecommendedProducts(),
    ])
    hotProducts.value = hot
    latestProducts.value = latest
    specialProducts.value = special
    recommendedProducts.value = rec
  }

  function getProduct(id: string): Product | undefined {
    return products.value.find((item) => item.id === id)
  }

  async function fetchDetail(id: string): Promise<Product | undefined> {
    try {
      const p = await fetchProductDetail(id)
      // 更新列表中的缓存
      const idx = products.value.findIndex((item) => item.id === id)
      if (idx >= 0) products.value[idx] = p
      // 记录浏览历史
      browseHistory.value = [id, ...browseHistory.value.filter((item) => item !== id)].slice(0, 12)
      addBrowseHistory(id).catch(() => {})
      return p
    } catch {
      return undefined
    }
  }

  function viewProduct(id: string) {
    browseHistory.value = [id, ...browseHistory.value.filter((item) => item !== id)].slice(0, 12)
    localStorage.setItem('mall-browse-history', JSON.stringify(browseHistory.value))
    addBrowseHistory(id).catch(() => {})
  }

  function patchFilters(next: Partial<ProductFilters>) {
    filters.value = { ...filters.value, ...next, page: next.page ?? 1 }
    loadProducts()
  }

  return {
    products,
    categories,
    filters,
    pageSize,
    total,
    totalPages,
    brands,
    cities,
    filteredProducts,
    pagedProducts,
    latestProducts,
    hotProducts,
    specialProducts,
    recommendedProducts,
    browseHistory,
    loading,
    loadCategories,
    loadProducts,
    loadHomeData,
    getProduct,
    fetchDetail,
    viewProduct,
    patchFilters,
  }
})
