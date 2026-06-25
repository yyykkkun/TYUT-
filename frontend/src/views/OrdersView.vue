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
  <main class="stack-page orders-page">
    <div class="page-title">
      <h1 style="font-size: 2rem; margin-bottom: 8px">订单中心</h1>
    </div>

    <a-tabs v-model:activeKey="orders.statusFilter" size="large" style="margin-bottom: 24px">
      <a-tab-pane key="all" tab="全部" />
      <a-tab-pane key="pending_payment" tab="待付款" />
      <a-tab-pane key="paid" tab="待发货" />
      <a-tab-pane key="shipping" tab="待收货" />
      <a-tab-pane key="completed" tab="已完成" />
    </a-tabs>

    <EmptyState
      v-if="!orders.visibleOrders.length"
      title="当前没有订单"
      action-text="去选商品"
      action-to="/products"
    />

    <div v-else class="line-list">
      <article v-for="order in orders.visibleOrders" :key="order.id" class="order-card">
        <div
          class="order-card__top"
          style="
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            margin-bottom: 16px;
            padding-bottom: 16px;
            border-bottom: 1px solid #f0f0f0;
          "
        >
          <div>
            <h2 style="margin: 0 0 4px; font-size: 1.1rem">订单号：{{ order.id }}</h2>
            <p style="margin: 0; color: #8c8c8c; font-size: 0.9rem">
              下单时间：{{ order.createdAt }}
            </p>
          </div>
          <OrderStatusTag :status="order.status" />
        </div>
        <div
          class="mini-products"
          style="display: flex; gap: 12px; overflow-x: auto; padding-bottom: 12px"
        >
          <img
            v-for="item in order.items"
            :key="`${order.id}-${item.productId}`"
            :src="item.image"
            :alt="item.title"
            style="width: 80px; height: 80px; border-radius: 8px; object-fit: cover"
          />
        </div>
        <div
          class="order-card__bottom"
          style="
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-top: 16px;
            padding-top: 16px;
            border-top: 1px solid #f0f0f0;
          "
        >
          <div style="color: #8c8c8c">
            总计
            <strong style="color: var(--price); font-size: 1.25rem"
              >￥{{ order.total.toFixed(2) }}</strong
            >
          </div>
          <a-button @click="$router.push(`/orders/${order.id}`)" type="default">查看详情</a-button>
        </div>
      </article>
    </div>
  </main>
</template>
