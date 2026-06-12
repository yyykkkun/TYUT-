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
  recharge as apiRecharge,
  loadProfile as loadLocal,
} from '@/api/member'
import { isMockSession } from '@/api/request'

export const useMemberStore = defineStore('member', () => {
  // ===== 余额/积分/礼品卡 —— 唯一数据源 =====
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

  const defaultAddress = computed(() => addresses.value.find((a) => a.isDefault) || addresses.value[0])
  const availableCoupons = computed(() => coupons.value.filter((c) => !c.used))

  // ===== 核心：从数据库同步余额/积分 =====
  /** 仅 mock 模式读 localStorage */
  function syncLocal() {
    if (!isMockSession()) return
    try {
      const p = loadLocal()
      balance.value = p.balance
      points.value = p.points
      giftCard.value = p.giftCard
      growth.value = p.growth
      level.value = p.level
      couponCount.value = p.couponCount
      unreadCount.value = p.unreadCount
    } catch { /* ignore */ }
  }

  /** 从正确数据源同步：mock→localStorage，真实→API */
  async function syncFromServer() {
    if (isMockSession()) {
      syncLocal()
    } else {
      await loadProfile()
    }
  }

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
    try { addresses.value = await fetchAddresses() } catch { /* ignore */ }
  }

  async function loadCoupons() {
    try { coupons.value = await fetchCoupons() } catch { /* ignore */ }
  }

  async function loadBrowseHistory() {
    try { browseHistory.value = await fetchBrowseHistory() } catch { /* ignore */ }
  }

  async function loadAll() {
    loading.value = true
    try {
      await syncFromServer()
      await loadAddresses()
      await loadCoupons()
      await loadBrowseHistory()
    } finally { loading.value = false }
  }

  // ===== 地址操作 =====
  async function addAddress(data: Omit<Address, 'id'>) { await apiAddAddress(data); await loadAddresses() }
  async function setDefaultAddress(id: string) { await apiSetDefault(id); await loadAddresses() }
  async function removeAddress(id: string) { await apiRemoveAddr(id); await loadAddresses() }

  // ===== 积分/余额操作 —— 全部走数据库 =====
  async function exchangeCoupon(pointsCost: number): Promise<boolean> {
    try {
      await apiExchange(pointsCost)
      await syncFromServer()
      await loadCoupons()
      return true
    } catch { return false }
  }

  async function recharge(amount: number): Promise<boolean> {
    try {
      await apiRecharge(amount)
      await syncFromServer()
      return true
    } catch { return false }
  }

  return {
    balance, points, giftCard, growth, level, couponCount, unreadCount,
    addresses, coupons, browseHistory, loading,
    defaultAddress, availableCoupons,
    syncFromServer, syncLocal, loadProfile, loadAddresses, loadCoupons, loadBrowseHistory, loadAll,
    addAddress, setDefaultAddress, removeAddress,
    exchangeCoupon, recharge,
  }
})
