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

const colors: Record<OrderStatus, string> = {
  pending_payment: 'warning',
  paid: 'processing',
  shipping: 'processing',
  completed: 'success',
  cancelled: 'default',
}
</script>

<template>
  <a-tag :color="colors[status]">{{ labels[status] }}</a-tag>
</template>
