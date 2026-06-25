<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { get, put } from '@/api/request'

const orders = ref<any[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await get<{ list: any[]; total: number }>('/admin/orders')
    orders.value = res.list.filter((o: any) => o.refundStatus === 'pending')
  } catch {
    orders.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)

async function approve(id: string) {
  try {
    await put(`/orders/${id}/refund/approve`, { approved: true })
    await load()
  } catch (e: any) {
    alert(e.message)
  }
}
async function reject(id: string) {
  try {
    await put(`/orders/${id}/refund/approve`, { approved: false })
    await load()
  } catch (e: any) {
    alert(e.message)
  }
}
</script>

<template>
  <div>
    <h1 style="margin-bottom: 24px">退款审核</h1>
    <a-empty v-if="!orders.length && !loading" description="暂无待审核退款" />
    <div v-for="o in orders" :key="o.id" class="panel refund-card">
      <div>
        <strong>{{ o.orderNo }}</strong>
        <span style="color: #8c8c8c; margin-left: 12px">￥{{ o.total }}</span>
        <p style="margin: 4px 0 0; color: #595959">{{ o.refundReason || '无原因' }}</p>
      </div>
      <div style="display: flex; gap: 8px">
        <a-button type="primary" size="small" @click="approve(o.id)">通过</a-button>
        <a-button size="small" @click="reject(o.id)">拒绝</a-button>
      </div>
    </div>
  </div>
</template>
