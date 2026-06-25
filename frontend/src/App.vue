<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { MenuOutlined, MessageOutlined, ShoppingCartOutlined, UserOutlined } from '@ant-design/icons-vue'
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
  <a-config-provider :theme="{ token: { colorPrimary: '#0f9f86', borderRadius: 8 } }">
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
        <a-button class="account-button" type="primary" @click="$router.push(auth.isLoggedIn ? '/member' : '/login')">
          <template #icon><UserOutlined /></template>
          {{ accountLabel }}
        </a-button>
        <a-button class="mobile-menu-button" :aria-expanded="mobileNavOpen" @click="mobileNavOpen = !mobileNavOpen">
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
