<script setup lang="ts">
import { computed } from 'vue'
import { useCartStore } from '@/stores/cart'
import { useAuthStore } from '@/stores/auth'
import { useMessageStore } from '@/stores/messages'

const cart = useCartStore()
const auth = useAuthStore()
const messages = useMessageStore()

const accountLabel = computed(() => (auth.isLoggedIn ? auth.user.name : '登录'))
</script>

<template>
  <header class="app-header">
    <RouterLink class="brand" to="/">
      <span>校</span>
      <strong>校园优选商城</strong>
    </RouterLink>
    <nav class="main-nav">
      <RouterLink to="/products">商品</RouterLink>
      <RouterLink to="/promotions/seckill">秒杀</RouterLink>
      <RouterLink to="/promotions/group-buy">团购</RouterLink>
      <RouterLink to="/orders">订单</RouterLink>
      <RouterLink to="/member">会员</RouterLink>
    </nav>
    <div class="header-actions">
      <RouterLink class="header-link" to="/messages">消息 {{ messages.unreadCount }}</RouterLink>
      <RouterLink class="header-link" to="/cart">购物车 {{ cart.totalCount }}</RouterLink>
      <RouterLink class="btn small" :to="auth.isLoggedIn ? '/member' : '/login'">{{ accountLabel }}</RouterLink>
    </div>
  </header>

  <RouterView />

  <footer class="app-footer">
    <span>Vue 3 + Vite + TypeScript + Pinia + Vue Router</span>
    <span>后端不可用时自动降级为本地 mock 数据</span>
  </footer>
</template>

<style scoped></style>
