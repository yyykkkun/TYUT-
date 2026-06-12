<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { get } from '@/api/request'

const groups = ref<any[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try { groups.value = await get<any[]>('/group-buy/active') }
  catch { groups.value = [] }
  finally { loading.value = false }
})
</script>

<template>
  <div>
    <h1 style="margin-bottom: 24px;">拼团管理</h1>
    <a-empty v-if="!groups.length && !loading" description="暂无活跃拼团" />
    <a-table :columns="[
      { title: '商品', dataIndex: 'productTitle' },
      { title: '团长', dataIndex: 'leaderName', width: 100 },
      { title: '拼团价', dataIndex: 'groupPrice', width: 100 },
      { title: '进度', width: 120 },
      { title: '状态', dataIndex: 'status', width: 100 },
    ]" :data-source="groups" :loading="loading" row-key="id" :pagination="false" size="middle">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'groupPrice'">￥{{ record.groupPrice }}</template>
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="record.status === 'active' ? 'blue' : record.status === 'completed' ? 'green' : 'red'">{{ record.status }}</a-tag>
        </template>
        <template v-if="column.dataIndex === 'progress'">
          {{ record.currentCount }} / {{ record.requiredCount }}
        </template>
      </template>
    </a-table>
  </div>
</template>
