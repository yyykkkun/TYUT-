<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useMemberStore } from '@/stores/member'

const member = useMemberStore()
const notice = ref('')

onMounted(async () => {
  member.syncLocal()
  await member.loadProfile()
})

async function exchange() {
  const ok = await member.exchangeCoupon(500)
  notice.value = ok ? '兑换成功，优惠券已加入账户。' : '积分不足，暂时无法兑换。'
}
</script>

<template>
  <main class="stack-page">
    <div class="page-title">
      <h1>积分商城</h1>
      <p>积分可兑换优惠券或礼品。</p>
    </div>
    <section class="metric-grid">
      <article>
        <span>当前积分</span><strong>{{ member.points }}</strong>
      </article>
      <article>
        <span>可用优惠券</span><strong>{{ member.availableCoupons.length }}</strong>
      </article>
    </section>
    <section class="panel reward-card">
      <div>
        <h2>500 积分兑换 10 元券</h2>
        <p>满 60 元可用，适合小额补贴订单。</p>
      </div>
      <button class="btn primary" type="button" @click="exchange">立即兑换</button>
    </section>
    <p v-if="notice" class="notice">{{ notice }}</p>
  </main>
</template>
