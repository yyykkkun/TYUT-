import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  login as apiLogin,
  register as apiRegister,
  anonymousLogin as apiAnonymous,
  getMe,
} from '@/api/auth'
import { useMemberStore } from '@/stores/member'
import { useCartStore } from '@/stores/cart'
import { useOrderStore } from '@/stores/orders'
import { useMessageStore } from '@/stores/messages'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('mall-token') || '')
  const user = ref({
    id: '',
    name: '',
    account: '',
    level: '',
    avatar: localStorage.getItem('mall-user-avatar') || '',
    role: localStorage.getItem('mall-user-role') || '',
  })

  const isAdmin = computed(() => user.value.role === 'admin')
  const loading = ref(false)

  const isLoggedIn = computed(() => Boolean(token.value))

  /** 切换用户前先清空所有旧数据，避免闪现 */
  function clearAllStores() {
    // 清空 localStorage 中的 mock 数据（订单、购物车）
    localStorage.removeItem('mall-mock-orders')
    localStorage.removeItem('mall-mock-cart')

    // 清空会员数据
    const member = useMemberStore()
    member.balance = 0
    member.points = 0
    member.giftCard = 0
    member.growth = 0
    member.level = ''
    member.couponCount = 0
    member.unreadCount = 0
    member.addresses = []
    member.coupons = []
    member.browseHistory = []

    // 清空购物车
    const cart = useCartStore()
    cart.items = []

    // 清空订单
    const orders = useOrderStore()
    orders.orders = []

    // 清空消息
    const messages = useMessageStore()
    messages.messages = []
  }

  async function login(account: string, password: string) {
    loading.value = true
    try {
      clearAllStores()
      const res = await apiLogin({ account, password })
      token.value = res.token
      user.value = { ...res.user }
      localStorage.setItem('mall-token', token.value)
      localStorage.setItem('mall-user-name', user.value.name)
      localStorage.setItem('mall-user-account', user.value.account)
      localStorage.setItem('mall-user-role', user.value.role)
    } finally {
      loading.value = false
    }
  }

  async function anonymousLogin(phone: string) {
    loading.value = true
    try {
      clearAllStores()
      const res = await apiAnonymous(phone)
      token.value = res.token
      user.value = { ...res.user }
      localStorage.setItem('mall-token', token.value)
      localStorage.setItem('mall-user-name', user.value.name)
      localStorage.setItem('mall-user-account', user.value.account)
      localStorage.setItem('mall-user-role', user.value.role)
    } finally {
      loading.value = false
    }
  }

  async function fetchMe() {
    if (!token.value) return
    try {
      const u = await getMe()
      user.value = { ...u }
      localStorage.setItem('mall-user-role', user.value.role)
    } catch {
      logout()
    }
  }

  async function register(nickname: string, phone: string, password: string) {
    loading.value = true
    try {
      clearAllStores()
      const res = await apiRegister({ username: phone, password, phone, nickname })
      token.value = res.token
      user.value = { ...res.user }
      localStorage.setItem('mall-token', token.value)
      localStorage.setItem('mall-user-name', user.value.name)
      localStorage.setItem('mall-user-account', user.value.account)
      localStorage.setItem('mall-user-role', user.value.role)
    } finally {
      loading.value = false
    }
  }

  function logout() {
    clearAllStores()
    token.value = ''
    user.value = { id: '', name: '', account: '', level: '', avatar: '', role: '' }
    localStorage.removeItem('mall-token')
    localStorage.removeItem('mall-user-name')
    localStorage.removeItem('mall-user-account')
    localStorage.removeItem('mall-user-role')
  }

  return { token, user, loading, isLoggedIn, isAdmin, login, register, anonymousLogin, fetchMe, logout }
})
