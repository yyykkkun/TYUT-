<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { get, put } from '@/api/request'

const users = ref<any[]>([])
const loading = ref(false)

async function exportCsv() {
  const token = localStorage.getItem('mall-token') || ''
  const res = await fetch('/api/admin/export/users', { headers: { Authorization: `Bearer ${token}` } })
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = 'users.csv'; a.click()
  URL.revokeObjectURL(url)
}

onMounted(async () => {
  loading.value = true
  try { const res = await get<{ list: any[]; total: number }>('/admin/users'); users.value = res.list }
  catch { users.value = [] }
  finally { loading.value = false }
})

async function toggleStatus(u: any) {
  const ns = u.status === 1 ? 0 : 1
  try { await put(`/admin/users/${u.id}/status`, { status: ns }) } catch { return }
  u.status = ns
}
</script>

<template>
  <div>
    <h1 style="margin-bottom: 24px; display: flex; justify-content: space-between;">
      用户管理
      <a-button @click="exportCsv">📥 导出CSV</a-button>
    </h1>
    <a-table :columns="[
      { title: 'ID', dataIndex: 'id', width: 60 },
      { title: '用户名', dataIndex: 'username' },
      { title: '昵称', dataIndex: 'nickname' },
      { title: '等级', dataIndex: 'level', width: 100 },
      { title: '余额', dataIndex: 'balance', width: 100 },
      { title: '积分', dataIndex: 'points', width: 80 },
      { title: '状态', width: 120 },
    ]" :data-source="users" :loading="loading" row-key="id" :pagination="false" size="middle">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="record.status === 1 ? 'green' : 'red'">{{ record.status === 1 ? '正常' : '禁用' }}</a-tag>
          <a-button size="small" type="link" @click="toggleStatus(record)">{{ record.status === 1 ? '禁用' : '启用' }}</a-button>
        </template>
      </template>
    </a-table>
  </div>
</template>
