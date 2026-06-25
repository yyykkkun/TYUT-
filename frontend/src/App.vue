<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  MenuOutlined,
  MessageOutlined,
  ShoppingCartOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { useCartStore } from '@/stores/cart'
import { useAuthStore } from '@/stores/auth'
import { useMessageStore } from '@/stores/messages'
import { isMockSession } from '@/api/request'

const cart = useCartStore()
const auth = useAuthStore()
const messages = useMessageStore()
const mobileNavOpen = ref(false)

const accountLabel = computed(() => (auth.isLoggedIn ? auth.user.name : '登录'))
const messageBadgeCount = computed(() => messages.unreadCount + messages.chatUnreadCount)
const navItems = computed(() => [
  { to: '/products', label: '闲置市场', show: true },
  { to: '/messages', label: '沟通', show: true },
  { to: '/orders', label: '订单', show: true },
  { to: '/member', label: '个人中心', show: true },
  { to: '/admin', label: '管理后台', show: auth.isAdmin, admin: true },
])

onMounted(async () => {
  // 真实后端模式：彻底清空所有 mock 脏数据
  if (!isMockSession()) {
    localStorage.removeItem('mall-mock-orders')
    localStorage.removeItem('mall-mock-cart')
    localStorage.removeItem('mall-mock-addresses')
    localStorage.removeItem('mall-mock-profile')
    localStorage.removeItem('mall-mock-balance-records')
    localStorage.removeItem('mall-mock-points-records')
    localStorage.removeItem('mall-mock-messages-read')
  }
  // 恢复用户信息
  if (auth.isLoggedIn) {
    auth.fetchMe()
  }
  cart.loadCart()
  messages.loadMessages()
  messages.loadConversations()
  setInterval(() => {
    messages.loadMessages()
    messages.loadConversations()
  }, 30000)
})
</script>

<template>
  <a-config-provider
    :theme="{
      token: {
        colorPrimary: '#7C3AED',
        colorSuccess: '#22C55E',
        colorInfo: '#7C3AED',
        colorWarning: '#F59E0B',
        colorError: '#EF4444',
        colorText: '#1F1638',
        colorTextSecondary: '#5E5377',
        colorBorder: '#DED6F2',
        colorBgContainer: 'rgba(255,255,255,0.82)',
        borderRadius: 14,
        controlHeight: 42,
        boxShadow: '0 18px 45px rgba(76, 29, 149, 0.10)',
      },
      components: {
        Button: {
          primaryShadow: '0 12px 28px rgba(124, 58, 237, 0.24)',
          defaultShadow: '0 8px 20px rgba(76, 29, 149, 0.08)',
        },
        Menu: {
          itemBorderRadius: 12,
          itemSelectedBg: '#F1E9FF',
          itemSelectedColor: '#6D28D9',
        },
      },
    }"
  >
    <header class="app-header">
      <div class="header-left">
        <RouterLink class="brand" to="/" @click="mobileNavOpen = false">
          <span>校</span>
          <strong>校园二手交易</strong>
        </RouterLink>
        <nav class="main-nav" aria-label="主导航">
          <RouterLink
            v-for="item in navItems.filter((nav) => nav.show)"
            :key="item.to"
            :to="item.to"
            :class="{ 'nav-admin': item.admin }"
          >
            {{ item.label }}
          </RouterLink>
        </nav>
      </div>
      <div class="header-actions">
        <RouterLink class="header-link" to="/messages" aria-label="消息">
          <a-badge :count="messageBadgeCount" :offset="[5, -5]">
            <MessageOutlined />
          </a-badge>
        </RouterLink>
        <RouterLink class="header-link" to="/cart" aria-label="购物车">
          <a-badge id="cart-icon" :count="cart.totalCount" :offset="[5, -5]">
            <ShoppingCartOutlined />
          </a-badge>
        </RouterLink>
        <a-button
          class="account-button"
          type="primary"
          @click="$router.push(auth.isLoggedIn ? '/member' : '/login')"
        >
          <template #icon><UserOutlined /></template>
          {{ accountLabel }}
        </a-button>
        <a-button
          class="mobile-menu-button"
          :aria-expanded="mobileNavOpen"
          @click="mobileNavOpen = !mobileNavOpen"
        >
          <template #icon><MenuOutlined /></template>
        </a-button>
      </div>
    </header>

    <div v-if="mobileNavOpen" class="mobile-nav-panel">
      <RouterLink
        v-for="item in navItems.filter((nav) => nav.show)"
        :key="item.to"
        :to="item.to"
        :class="{ 'nav-admin': item.admin }"
        @click="mobileNavOpen = false"
      >
        {{ item.label }}
      </RouterLink>
    </div>

    <RouterView v-slot="{ Component, route }">
      <Transition name="route-fade" mode="out-in">
        <component :is="Component" :key="route.fullPath" />
      </Transition>
    </RouterView>
  </a-config-provider>
</template>
