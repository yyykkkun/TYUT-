import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  fetchCart as apiFetchCart,
  addToCart as apiAddToCart,
  updateCartQuantity as apiUpdateQty,
  toggleCartSelect as apiToggle,
  removeCartItem as apiRemove,
  clearSelectedCart as apiClear,
  type CartItemWithProduct,
} from '@/api/cart'
import { products as mockProducts } from '@/data/mock'

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItemWithProduct[]>([])
  const loading = ref(false)

  const detailedItems = computed(() => items.value.filter((item) => item.productId))

  const selectedItems = computed(() => detailedItems.value.filter((item) => item.selected))
  const selectedCount = computed(() =>
    selectedItems.value.reduce((sum, item) => sum + item.quantity, 0),
  )
  const totalCount = computed(() => items.value.reduce((sum, item) => sum + item.quantity, 0))
  const subtotal = computed(() =>
    selectedItems.value.reduce((sum, item) => {
      const price = item.product?.price ?? item.product?.originalPrice ?? 0
      return sum + price * item.quantity
    }, 0),
  )

  async function loadCart() {
    loading.value = true
    try {
      items.value = await apiFetchCart()
    } catch {
      items.value = []
    } finally {
      loading.value = false
    }
  }

  function findProduct(productId: string) {
    return (
      items.value.find((i) => i.product?.id === productId)?.product ||
      mockProducts.find((p) => p.id === productId) ||
      mockProducts.find((p) => p.id === productId.replace(/^p/, '')) ||
      mockProducts.find((p) => p.id === `p${productId}`)
    )
  }

  async function addItem(productId: string, spec: string, quantity = 1) {
    // 乐观更新
    const existing = items.value.find((i) => i.productId === productId && i.spec === spec)
    if (existing) {
      existing.quantity += quantity
    } else {
      items.value.push({
        id: `cart${Date.now()}`,
        productId,
        spec,
        quantity,
        selected: true,
        product: findProduct(productId),
      })
    }
    // 后台同步 API（mock 模式自动写 localStorage）
    try {
      await apiAddToCart(productId, spec, quantity)
    } catch {
      /* ignore */
    }
  }

  async function updateQuantity(id: string, quantity: number) {
    if (quantity <= 0) {
      await removeItem(id)
      return
    }
    const item = items.value.find((i) => i.id === id)
    if (item) item.quantity = quantity
    try {
      await apiUpdateQty(id, quantity)
    } catch {
      /* ignore */
    }
  }

  async function toggleSelected(id: string) {
    const item = items.value.find((i) => i.id === id)
    if (item) item.selected = !item.selected
    try {
      await apiToggle(id)
    } catch {
      /* ignore */
    }
  }

  async function removeItem(id: string) {
    items.value = items.value.filter((i) => i.id !== id)
    try {
      await apiRemove(id)
    } catch {
      /* ignore */
    }
  }

  async function clearSelected() {
    items.value = items.value.filter((i) => !i.selected)
    try {
      await apiClear()
    } catch {
      /* ignore */
    }
  }

  return {
    items,
    detailedItems,
    selectedItems,
    selectedCount,
    totalCount,
    subtotal,
    loading,
    loadCart,
    addItem,
    updateQuantity,
    toggleSelected,
    removeItem,
    clearSelected,
  }
})
