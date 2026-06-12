<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { get, put } from '@/api/request'

const products = ref<any[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await get<{ list: any[]; total: number }>('/admin/products')
    products.value = res.list
  } catch {
    products.value = []
  } finally {
    loading.value = false
  }
})

async function toggleStatus(p: any) {
  const newStatus = p.status === 1 ? 0 : 1
  try { await put(`/admin/products/${p.id}/status`, { status: newStatus }) } catch { return }
  p.status = newStatus
}
</script>

<template>
  <div>
    <h1 style="margin-bottom: 24px;">商品管理</h1>
    <a-table :columns="[
      { title: 'ID', dataIndex: 'id', width: 60 },
      { title: '商品', dataIndex: 'title' },
      { title: '价格', dataIndex: 'price', width: 100 },
      { title: '库存', dataIndex: 'stock', width: 80 },
      { title: '销量', dataIndex: 'sales', width: 80 },
      { title: '状态', width: 100 },
    ]" :data-source="products" :loading="loading" row-key="id" :pagination="false" size="middle">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="record.status === 1 ? 'green' : 'red'">{{ record.status === 1 ? '上架' : '下架' }}</a-tag>
          <a-button size="small" type="link" @click="toggleStatus(record)">{{ record.status === 1 ? '下架' : '上架' }}</a-button>
        </template>
      </template>
    </a-table>
  </div>
</template>
