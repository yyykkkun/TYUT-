<script setup lang="ts">
import { onMounted } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import OrderStatusTag from '@/components/OrderStatusTag.vue'
import { useOrderStore } from '@/stores/orders'

const orders = useOrderStore()

onMounted(() => {
  orders.loadOrders()
})
</script>

<template>
  <main class="stack-page">
    <div class="page-title">
      <h1>订单中心</h1>
      <p>查看全部、待付款、待发货、待收货、已完成订单。</p>
    </div>

    <div class="tabs">
      <button :class="{ active: orders.statusFilter === 'all' }" @click="orders.statusFilter = 'all'">全部</button>
      <button :class="{ active: orders.statusFilter === 'pending_payment' }" @click="orders.statusFilter = 'pending_payment'">待付款</button>
      <button :class="{ active: orders.statusFilter === 'paid' }" @click="orders.statusFilter = 'paid'">待发货</button>
      <button :class="{ active: orders.statusFilter === 'shipping' }" @click="orders.statusFilter = 'shipping'">待收货</button>
      <button :class="{ active: orders.statusFilter === 'completed' }" @click="orders.statusFilter = 'completed'">已完成</button>
    </div>

    <EmptyState v-if="!orders.visibleOrders.length" title="当前没有订单" action-text="去选商品" action-to="/products" />

    <div v-else class="line-list">
      <article v-for="order in orders.visibleOrders" :key="order.id" class="order-card">
        <div class="order-card__top">
          <div>
            <h2>{{ order.id }}</h2>
            <p>{{ order.createdAt }}</p>
          </div>
          <OrderStatusTag :status="order.status" />
        </div>
        <div class="mini-products">
          <img v-for="item in order.items" :key="`${order.id}-${item.productId}`" :src="item.image" :alt="item.title" />
        </div>
        <div class="order-card__bottom">
          <strong>￥{{ order.total }}</strong>
          <RouterLink class="btn" :to="`/orders/${order.id}`">查看详情</RouterLink>
        </div>
      </article>
    </div>
  </main>
</template>
