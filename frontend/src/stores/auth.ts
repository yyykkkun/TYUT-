import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  login as apiLogin,
  register as apiRegister,
  anonymousLogin as apiAnonymous,
  getMe,
} from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('mall-token') || '')
  const user = ref({
    id: '',
    name: '',
    account: '',
    level: '',
  })
  const loading = ref(false)

  const isLoggedIn = computed(() => Boolean(token.value))

  async function login(account: string, password: string) {
    loading.value = true
    try {
      const res = await apiLogin({ account, password })
      token.value = res.token
      user.value = { ...res.user }
      localStorage.setItem('mall-token', token.value)
      localStorage.setItem('mall-user-name', user.value.name)
      localStorage.setItem('mall-user-account', user.value.account)
    } finally {
      loading.value = false
    }
  }

  async function anonymousLogin(phone: string) {
    loading.value = true
    try {
      const res = await apiAnonymous(phone)
      token.value = res.token
      user.value = { ...res.user }
      localStorage.setItem('mall-token', token.value)
      localStorage.setItem('mall-user-name', user.value.name)
      localStorage.setItem('mall-user-account', user.value.account)
    } finally {
      loading.value = false
    }
  }

  async function fetchMe() {
    if (!token.value) return
    try {
      const u = await getMe()
      user.value = { ...u }
    } catch {
      // token 过期
      logout()
    }
  }

  async function register(nickname: string, phone: string, password: string) {
    loading.value = true
    try {
      const res = await apiRegister({ username: phone, password, phone, nickname })
      token.value = res.token
      user.value = { ...res.user }
      localStorage.setItem('mall-token', token.value)
      localStorage.setItem('mall-user-name', user.value.name)
      localStorage.setItem('mall-user-account', user.value.account)
    } finally {
      loading.value = false
    }
  }

  function logout() {
    token.value = ''
    user.value = { id: '', name: '', account: '', level: '' }
    localStorage.removeItem('mall-token')
    localStorage.removeItem('mall-user-name')
    localStorage.removeItem('mall-user-account')
  }

  return { token, user, loading, isLoggedIn, login, register, anonymousLogin, fetchMe, logout }
})
