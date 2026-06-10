<script setup lang="ts">
import { onMounted } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import { useOrderStore } from '@/stores/orders'

const orders = useOrderStore()

onMounted(() => {
  orders.loadOrders()
})
</script>

<template>
  <main class="stack-page">
    <div class="page-title">
      <h1>评论晒单</h1>
      <p>已完成订单可进入详情提交评价。</p>
    </div>
    <div v-if="orders.orders.length" class="line-list">
      <article v-for="order in orders.orders" :key="order.id" class="order-card">
        <h2>{{ order.id }}</h2>
        <p>{{ order.review || (order.status === 'completed' ? '待评价' : '订单完成后可评价') }}</p>
        <RouterLink class="btn" :to="`/orders/${order.id}`">进入订单</RouterLink>
      </article>
    </div>
    <EmptyState v-else title="暂无可评价订单" action-text="去下单" action-to="/products" />
  </main>
</template>
