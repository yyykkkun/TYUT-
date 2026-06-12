<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { get, put } from '@/api/request'

const orders = ref<any[]>([])
const loading = ref(false)

async function shipOrder(id: string) {
  try { await put(`/admin/orders/${id}/ship`); await loadOrders() }
  catch (e: any) { alert(e.message || '发货失败') }
}

async function loadOrders() {
  loading.value = true
  try { const res = await get<{ list: any[]; total: number }>('/admin/orders'); orders.value = res.list }
  catch { orders.value = [] }
  finally { loading.value = false }
}

async function exportCsv() {
  const token = localStorage.getItem('mall-token') || ''
  const res = await fetch('/api/admin/export/orders', { headers: { Authorization: `Bearer ${token}` } })
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = 'orders.csv'; a.click()
  URL.revokeObjectURL(url)
}

onMounted(() => { loadOrders() })

const statusMap: Record<string, string> = {
  pending_payment: '待付款', paid: '已付款', shipping: '配送中', completed: '已完成', cancelled: '已取消',
}
const statusColor = (s: string) => s === 'completed' ? 'green' : s === 'cancelled' ? 'red' : s === 'shipping' ? 'purple' : s === 'paid' ? 'blue' : 'orange'
</script>

<template>
  <div>
    <h1 style="margin-bottom: 24px; display: flex; justify-content: space-between;">
      订单管理
      <a-button @click="exportCsv">📥 导出CSV</a-button>
    </h1>
    <a-table :columns="[
      { title: '订单号', dataIndex: 'orderNo' },
      { title: '用户ID', dataIndex: 'userId', width: 80 },
      { title: '金额', dataIndex: 'total', width: 100 },
      { title: '状态', dataIndex: 'status', width: 100 },
      { title: '退款', dataIndex: 'refundStatus', width: 100 },
      { title: '时间', dataIndex: 'createdAt', width: 160 },
      { title: '操作', dataIndex: 'action', width: 80 },
    ]" :data-source="orders" :loading="loading" row-key="id" :pagination="false" size="middle">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="statusColor(record.status)">{{ statusMap[record.status] || record.status }}</a-tag>
        </template>
        <template v-if="column.dataIndex === 'refundStatus'">
          <a-tag v-if="record.refundStatus === 'pending'" color="orange">待审核</a-tag>
          <a-tag v-else-if="record.refundStatus === 'completed'" color="green">已退款</a-tag>
          <span v-else style="color: #8c8c8c;">—</span>
        </template>
        <template v-if="column.dataIndex === 'action'">
          <a-button v-if="record.status === 'paid'" type="primary" size="small" @click="shipOrder(record.id)">发货</a-button>
          <span v-else style="color: #8c8c8c;">—</span>
        </template>
      </template>
    </a-table>
  </div>
</template>
