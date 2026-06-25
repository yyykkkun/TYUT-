<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  DashboardOutlined, ShoppingOutlined, OrderedListOutlined,
  UserOutlined, RollbackOutlined, HomeOutlined,
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()

const menuItems = [
  { key: '/admin', icon: DashboardOutlined, label: '数据看板' },
  { key: '/admin/products', icon: ShoppingOutlined, label: '商品管理' },
  { key: '/admin/orders', icon: OrderedListOutlined, label: '订单管理' },
  { key: '/admin/users', icon: UserOutlined, label: '用户管理' },
  { key: '/admin/refunds', icon: RollbackOutlined, label: '退款审核' },
]

const selectedKeys = ref([route.path])
</script>

<template>
  <a-layout style="min-height: 100vh;">
    <a-layout-sider width="220" style="background: #fff; border-right: 1px solid #f0f0f0;">
      <div style="padding: 20px 16px; font-weight: 800; font-size: 1.1rem; color: var(--primary); display: flex; align-items: center; gap: 8px;">
        <span style="display: inline-flex; align-items: center; justify-content: center; width: 32px; height: 32px; background: var(--primary); color: #fff; border-radius: 8px;">管</span>
        二手交易后台
      </div>
      <a-menu v-model:selectedKeys="selectedKeys" mode="inline" @click="({ key }: { key: string }) => router.push(key)">
        <a-menu-item v-for="m in menuItems" :key="m.key">
          <template #icon><component :is="m.icon" /></template>
          {{ m.label }}
        </a-menu-item>
      </a-menu>
      <div style="position: absolute; bottom: 16px; left: 16px;">
        <a-button type="link" @click="router.push('/')">
          <template #icon><HomeOutlined /></template>返回市场
        </a-button>
      </div>
    </a-layout-sider>
    <a-layout-content style="padding: 24px; background: #f5f5f5;">
      <RouterView />
    </a-layout-content>
  </a-layout>
</template>
