<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { get } from '@/api/request'
import { useMemberStore } from '@/stores/member'
import { getPointsRecords } from '@/api/member'
import { TrophyOutlined, ShoppingCartOutlined, RollbackOutlined, GiftOutlined, StarOutlined } from '@ant-design/icons-vue'

const member = useMemberStore()
const records = ref<any[]>([])
const loading = ref(false)

const typeConfig: Record<string, { label: string; color: string; sign: string; icon: any }> = {
  exchange:     { label: '兑换优惠券', color: '#ef4444', sign: '-', icon: GiftOutlined },
  order_use:    { label: '下单抵扣', color: '#ef4444', sign: '-', icon: ShoppingCartOutlined },
  order_cancel: { label: '订单取消退回', color: '#10b981', sign: '+', icon: RollbackOutlined },
  points_earn:  { label: '确认收货赠送', color: '#10b981', sign: '+', icon: TrophyOutlined },
}

onMounted(async () => {
  loading.value = true
  member.syncLocal()
  await member.loadProfile()
  try { records.value = await get<any[]>('/member/points-records') }
  catch { records.value = getPointsRecords() }
  finally { loading.value = false }
})
</script>

<template>
  <main class="stack-page">
    <div class="page-title">
      <h1 style="display:flex; align-items:center; gap:12px;">
        <TrophyOutlined /> 积分明细
      </h1>
      <p>当前积分 <strong style="color: var(--primary); font-size: 1.2rem;">{{ member.points }}</strong></p>
    </div>

    <a-spin :spinning="loading">
      <a-timeline v-if="records.length">
        <a-timeline-item v-for="r in records" :key="r.id" :color="(typeConfig[r.type] || {}).color || '#8c8c8c'">
          <div style="display:flex; align-items:center; gap:8px;">
            <component :is="(typeConfig[r.type] || {}).icon" v-if="(typeConfig[r.type] || {}).icon" />
            <strong>{{ (typeConfig[r.type] || {}).label || r.type }}</strong>
            <span :style="{ color: (typeConfig[r.type] || {}).color, fontWeight: 700, marginLeft: 'auto' }">
              {{ (typeConfig[r.type] || {}).sign || '' }}{{ Math.abs(r.amount) }} 分
            </span>
          </div>
          <div v-if="r.orderNo" style="color:#8c8c8c; font-size:0.8rem;">{{ r.orderNo }}</div>
          <div style="color:#8c8c8c; font-size:0.8rem;">{{ r.remark }} · {{ r.createdAt?.slice(0,16) }}</div>
        </a-timeline-item>
      </a-timeline>
      <a-empty v-else description="暂无积分变动记录" />
    </a-spin>
  </main>
</template>
