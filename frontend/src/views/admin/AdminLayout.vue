<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  DashboardOutlined,
  ShoppingOutlined,
  OrderedListOutlined,
  UserOutlined,
  RollbackOutlined,
  HomeOutlined,
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
  <a-layout class="admin-shell">
    <a-layout-sider width="232" class="admin-sider">
      <div class="admin-brand">
        <span>管</span>
        二手交易后台
      </div>
      <a-menu
        v-model:selectedKeys="selectedKeys"
        mode="inline"
        @click="({ key }: { key: string }) => router.push(key)"
      >
        <a-menu-item v-for="m in menuItems" :key="m.key">
          <template #icon><component :is="m.icon" /></template>
          {{ m.label }}
        </a-menu-item>
      </a-menu>
      <div class="admin-back">
        <a-button type="link" @click="router.push('/')">
          <template #icon><HomeOutlined /></template>返回市场
        </a-button>
      </div>
    </a-layout-sider>
    <a-layout-content class="admin-content">
      <RouterView />
    </a-layout-content>
  </a-layout>
</template>
