<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { BellOutlined, ShoppingCartOutlined, UserOutlined } from '@ant-design/icons-vue'
import { useCartStore } from '@/stores/cart'
import { useAuthStore } from '@/stores/auth'
import { useMessageStore } from '@/stores/messages'
import { isMockSession } from '@/api/request'

const cart = useCartStore()
const auth = useAuthStore()
const messages = useMessageStore()

const accountLabel = computed(() => (auth.isLoggedIn ? auth.user.name : '登录'))

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
  setInterval(() => messages.loadMessages(), 30000)
})
</script>

<template>
  <a-config-provider :theme="{ token: { colorPrimary: '#10b981', borderRadius: 8 } }">
    <header class="app-header" style="display: flex; align-items: center; justify-content: space-between; padding: 0 24px; height: 64px; background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.06); position: sticky; top: 0; z-index: 100;">
      <div style="display: flex; align-items: center; gap: 32px;">
        <RouterLink to="/" style="display: flex; align-items: center; gap: 8px; text-decoration: none; color: inherit;">
          <span style="display: flex; align-items: center; justify-content: center; width: 32px; height: 32px; background: var(--primary); color: #fff; border-radius: 8px; font-weight: bold;">校</span>
          <strong style="font-size: 1.1rem;">校园优选商城</strong>
        </RouterLink>
        <nav style="display: flex; gap: 8px;">
          <RouterLink to="/products" class="nav-link">商品</RouterLink>
          <RouterLink to="/promotions/seckill" class="nav-link">秒杀</RouterLink>
          <RouterLink to="/promotions/group-buy" class="nav-link">团购</RouterLink>
          <RouterLink to="/orders" class="nav-link">订单</RouterLink>
          <RouterLink to="/member" class="nav-link">会员</RouterLink>
          <RouterLink v-if="auth.isAdmin" to="/admin" class="nav-link" style="color: var(--primary); font-weight: 700;">管理后台</RouterLink>
        </nav>
      </div>
      <div style="display: flex; align-items: center; gap: 24px;">
        <RouterLink to="/messages" style="color: inherit; text-decoration: none;">
          <a-badge :count="messages.unreadCount" :offset="[5, -5]">
            <BellOutlined style="font-size: 20px; cursor: pointer; color: #595959;" />
          </a-badge>
        </RouterLink>
        <RouterLink to="/cart" style="color: inherit; text-decoration: none;">
          <a-badge id="cart-icon" :count="cart.totalCount" :offset="[5, -5]">
            <ShoppingCartOutlined style="font-size: 20px; cursor: pointer; color: #595959;" />
          </a-badge>
        </RouterLink>
        <a-button type="primary" shape="round" @click="$router.push(auth.isLoggedIn ? '/member' : '/login')">
          <template #icon><UserOutlined /></template>
          {{ accountLabel }}
        </a-button>
      </div>
    </header>

    <RouterView v-slot="{ Component, route }">
      <Transition name="route-fade" mode="out-in">
        <component :is="Component" :key="route.fullPath" />
      </Transition>
    </RouterView>

    <footer style="text-align: center; padding: 24px; color: #8c8c8c; background: #fafafa; margin-top: 40px;">
      <p style="margin: 0;">Vue 3 + Vite + TypeScript + Pinia + Vue Router + Ant Design Vue</p>
    </footer>
  </a-config-provider>
</template>

<style scoped>
.nav-link {
  padding: 6px 12px;
  color: #595959;
  text-decoration: none;
  border-radius: 6px;
  transition: all 0.2s;
}
.nav-link:hover {
  background: #f0f0f0;
  color: #262626;
}
.nav-link.router-link-active {
  color: var(--primary);
  background: var(--primary-soft);
  font-weight: 600;
}
</style>
