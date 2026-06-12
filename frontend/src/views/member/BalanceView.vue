<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { get } from '@/api/request'
import { useMemberStore } from '@/stores/member'
import { getBalanceRecords } from '@/api/member'
import { WalletOutlined, PlusCircleOutlined, ShoppingCartOutlined, RollbackOutlined, DollarOutlined } from '@ant-design/icons-vue'

const member = useMemberStore()
const records = ref<any[]>([])
const loading = ref(false)

const typeConfig: Record<string, { label: string; color: string; sign: string; icon: any }> = {
  recharge:     { label: '充值', color: '#10b981', sign: '+', icon: PlusCircleOutlined },
  order_pay:    { label: '订单支付', color: '#ef4444', sign: '-', icon: ShoppingCartOutlined },
  order_cancel: { label: '订单取消退回', color: '#10b981', sign: '+', icon: RollbackOutlined },
  order_refund: { label: '退款退回', color: '#10b981', sign: '+', icon: DollarOutlined },
}

onMounted(async () => {
  loading.value = true
  member.syncLocal()
  await member.loadProfile()
  try { records.value = await get<any[]>('/member/balance-records') }
  catch { records.value = getBalanceRecords() }
  finally { loading.value = false }
})
</script>

<template>
  <main class="stack-page">
    <div class="page-title">
      <h1 style="display:flex; align-items:center; gap:12px;"><WalletOutlined /> 余额明细</h1>
      <p>当前余额 <strong style="color: var(--primary); font-size: 1.2rem;">￥{{ member.balance }}</strong></p>
    </div>

    <a-spin :spinning="loading">
      <a-timeline v-if="records.length">
        <a-timeline-item v-for="r in records" :key="r.id" :color="(typeConfig[r.type] || {}).color || '#8c8c8c'">
          <div style="display:flex; align-items:center; gap:8px;">
            <component :is="(typeConfig[r.type] || {}).icon" v-if="(typeConfig[r.type] || {}).icon" />
            <strong>{{ (typeConfig[r.type] || {}).label || r.type }}</strong>
            <span :style="{ color: (typeConfig[r.type] || {}).color, fontWeight: 700, marginLeft: 'auto' }">
              {{ (typeConfig[r.type] || {}).sign || '' }}￥{{ Math.abs(r.amount).toFixed(2) }}
            </span>
          </div>
          <div v-if="r.orderNo" style="color:#8c8c8c; font-size:0.8rem;">{{ r.orderNo }}</div>
          <div style="color:#8c8c8c; font-size:0.8rem;">{{ r.remark }} · {{ r.createdAt?.slice(0,16) }}</div>
          <div v-if="r.balanceAfter !== undefined" style="color:#8c8c8c; font-size:0.8rem;">剩余余额 ￥{{ r.balanceAfter }}</div>
        </a-timeline-item>
      </a-timeline>
      <a-empty v-else description="暂无余额变动记录" />
    </a-spin>
  </main>
</template>
