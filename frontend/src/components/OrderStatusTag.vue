<script setup lang="ts">
import type { OrderStatus } from '@/types/domain'

defineProps<{
  status: OrderStatus
}>()

const labels: Record<OrderStatus, string> = {
  pending_payment: '待付款',
  paid: '待发货',
  shipping: '待收货',
  completed: '已完成',
  cancelled: '已取消',
}
</script>

<template>
  <span class="status-tag" :class="status">{{ labels[status] }}</span>
</template>
