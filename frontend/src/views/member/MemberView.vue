<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useMemberStore } from '@/stores/member'
import { useMessageStore } from '@/stores/messages'

const auth = useAuthStore()
const member = useMemberStore()
const messages = useMessageStore()

onMounted(() => {
  member.loadAll()
  messages.loadMessages()
})
</script>

<template>
  <main class="stack-page">
    <section class="member-hero">
      <div>
        <p class="eyebrow">会员中心</p>
        <h1>{{ auth.user.name }}</h1>
        <p>{{ auth.user.level }} · 成长值 {{ member.growth }}</p>
      </div>
      <RouterLink v-if="!auth.isLoggedIn" class="btn primary" to="/login">登录</RouterLink>
      <button v-else class="btn" type="button" @click="auth.logout">退出登录</button>
    </section>

    <section class="metric-grid">
      <article><span>余额</span><strong>￥{{ member.balance }}</strong></article>
      <article><span>积分</span><strong>{{ member.points }}</strong></article>
      <article><span>优惠券</span><strong>{{ member.availableCoupons.length }}</strong></article>
      <article><span>礼品卡</span><strong>￥{{ member.giftCard }}</strong></article>
      <article><span>未读消息</span><strong>{{ messages.unreadCount }}</strong></article>
    </section>

    <section class="quick-grid">
      <RouterLink to="/orders">订单记录</RouterLink>
      <RouterLink to="/member/addresses">收货地址</RouterLink>
      <RouterLink to="/member/browse-history">浏览记录</RouterLink>
      <RouterLink to="/member/reviews">评论晒单</RouterLink>
      <RouterLink to="/messages">站内信</RouterLink>
      <RouterLink to="/points-mall">积分商城</RouterLink>
    </section>
  </main>
</template>
