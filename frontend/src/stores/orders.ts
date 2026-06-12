import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { useCartStore } from '@/stores/cart'
import { useMemberStore } from '@/stores/member'
import type { DeliveryMethod, Order, OrderStatus } from '@/types/domain'
import {
  fetchOrders as apiOrders,
  fetchOrderDetail,
  previewOrder,
  createOrder,
  payOrder,
  cancelOrder,
  confirmOrder,
  reviewOrder,
  type OrderPreview,
  type OrderCreateParams,
} from '@/api/orders'

export const useOrderStore = defineStore('orders', () => {
  const orders = ref<Order[]>([])
  const statusFilter = ref<OrderStatus | 'all'>('all')
  const selectedCouponId = ref('')
  const deliveryMethod = ref<DeliveryMethod>('platform')
  const usePoints = ref(true)
  const useGiftCard = ref(false)
  const useBalance = ref(false)
  const loading = ref(false)
  const lastPreview = ref<OrderPreview | null>(null)

  const visibleOrders = computed(() =>
    statusFilter.value === 'all'
      ? orders.value
      : orders.value.filter((item) => item.status === statusFilter.value),
  )

  async function loadOrders(status = 'all', page = 1) {
    loading.value = true
    try {
      orders.value = await apiOrders(status, page)
    } catch {
      orders.value = []
    } finally {
      loading.value = false
    }
  }

  async function preview(): Promise<OrderPreview> {
    const params: OrderCreateParams = {
      deliveryMethod: deliveryMethod.value,
      usePoints: usePoints.value,
      useGiftCard: useGiftCard.value,
      useBalance: useBalance.value,
      couponId: selectedCouponId.value || null,
    }
    const result = await previewOrder(params)
    lastPreview.value = result
    return result
  }

  async function submitOrder(): Promise<string> {
    // 积分不足100时自动取消勾选
    const member = useMemberStore()
    if (usePoints.value && member.points < 100) {
      usePoints.value = false
    }
    const params: OrderCreateParams = {
      deliveryMethod: deliveryMethod.value,
      usePoints: usePoints.value,
      useGiftCard: useGiftCard.value,
      useBalance: useBalance.value,
      couponId: selectedCouponId.value || null,
    }
    const orderNo = await createOrder(params)
    await loadOrders()
    return orderNo
  }

  async function getOrder(id: string): Promise<Order | undefined> {
    try {
      return await fetchOrderDetail(id)
    } catch {
      return orders.value.find((item) => item.id === id)
    }
  }

  async function pay(id: string, paymentMethod?: string) {
    await payOrder(id, paymentMethod)
    await loadOrders()
  }

  async function cancel(id: string) {
    await cancelOrder(id)
    await loadOrders()
  }

  async function confirm(id: string) {
    await confirmOrder(id)
    await loadOrders()
  }

  async function review(id: string, text: string, ratingVal: number = 5) {
    await reviewOrder(id, text, ratingVal)
    await loadOrders()
  }

  return {
    orders,
    statusFilter,
    selectedCouponId,
    deliveryMethod,
    usePoints,
    useGiftCard,
    useBalance,
    loading,
    lastPreview,
    visibleOrders,
    loadOrders,
    preview,
    submitOrder,
    getOrder,
    pay,
    cancel,
    confirm,
    review,
  }
})
