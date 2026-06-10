import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { Address, Coupon } from '@/types/domain'
import {
  fetchProfile,
  fetchAddresses,
  addAddress as apiAddAddress,
  setDefaultAddress as apiSetDefault,
  removeAddress as apiRemoveAddr,
  fetchCoupons,
  exchangeCoupon as apiExchange,
  fetchBrowseHistory,
} from '@/api/member'

export const useMemberStore = defineStore('member', () => {
  const balance = ref(0)
  const points = ref(0)
  const giftCard = ref(0)
  const growth = ref(0)
  const level = ref('')
  const couponCount = ref(0)
  const unreadCount = ref(0)

  const addresses = ref<Address[]>([])
  const coupons = ref<Coupon[]>([])
  const browseHistory = ref<string[]>([])
  const loading = ref(false)

  const defaultAddress = computed(() => addresses.value.find((item) => item.isDefault) || addresses.value[0])
  const availableCoupons = computed(() => coupons.value.filter((item) => !item.used))

  async function loadProfile() {
    try {
      const p = await fetchProfile()
      balance.value = p.balance
      points.value = p.points
      giftCard.value = p.giftCard
      growth.value = p.growth
      level.value = p.level
      couponCount.value = p.couponCount
      unreadCount.value = p.unreadCount
    } catch { /* ignore */ }
  }

  async function loadAddresses() {
    try {
      addresses.value = await fetchAddresses()
    } catch { /* ignore */ }
  }

  async function addAddress(data: Omit<Address, 'id'>) {
    await apiAddAddress(data)
    await loadAddresses()
  }

  async function setDefaultAddress(id: string) {
    await apiSetDefault(id)
    await loadAddresses()
  }

  async function removeAddress(id: string) {
    await apiRemoveAddr(id)
    await loadAddresses()
  }

  async function loadCoupons() {
    try {
      coupons.value = await fetchCoupons()
    } catch { /* ignore */ }
  }

  async function exchangeCoupon(pointsCost: number): Promise<boolean> {
    await apiExchange(pointsCost)
    await loadProfile()
    await loadCoupons()
    return true
  }

  async function loadBrowseHistory() {
    try {
      browseHistory.value = await fetchBrowseHistory()
    } catch { /* ignore */ }
  }

  async function loadAll() {
    loading.value = true
    try {
      await Promise.all([loadProfile(), loadAddresses(), loadCoupons(), loadBrowseHistory()])
    } finally {
      loading.value = false
    }
  }

  return {
    balance, points, giftCard, growth, level, couponCount, unreadCount,
    addresses, coupons, browseHistory, loading,
    defaultAddress, availableCoupons,
    loadProfile, loadAddresses, addAddress, setDefaultAddress, removeAddress,
    loadCoupons, exchangeCoupon, loadBrowseHistory, loadAll,
  }
})
