<script setup lang="ts">
import { onMounted } from 'vue'
import {
  LogoutOutlined,
  ShoppingOutlined,
  EnvironmentOutlined,
  HistoryOutlined,
  CommentOutlined,
  MailOutlined,
  GiftOutlined,
  WalletOutlined,
  TrophyOutlined,
  PropertySafetyOutlined,
  CreditCardOutlined,
  BellOutlined
} from '@ant-design/icons-vue'
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
  <main class="stack-page" style="max-width: 1000px; margin: 0 auto; padding: 40px 20px;">
    <!-- Member Header -->
    <a-card :bordered="false" style="background: linear-gradient(135deg, var(--primary) 0%, oklch(0.5 0.15 150) 100%); color: #fff; border-radius: 12px; margin-bottom: 24px;">
      <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 24px;">
        <div style="display: flex; align-items: center; gap: 24px;">
          <a-avatar :size="80" style="background-color: rgba(255, 255, 255, 0.2); font-size: 32px;">
            {{ auth.isLoggedIn ? auth.user.name.charAt(0).toUpperCase() : '客' }}
          </a-avatar>
          <div>
            <p style="margin: 0 0 8px; opacity: 0.9; font-size: 0.9rem;">会员中心</p>
            <h1 style="margin: 0 0 8px; color: #fff; font-size: 2rem;">
              {{ auth.isLoggedIn ? auth.user.name : '游客，请登录' }}
            </h1>
            <p v-if="auth.isLoggedIn" style="margin: 0; opacity: 0.9;">
              <a-tag color="gold" style="border: none;">{{ auth.user.level }}</a-tag>
              成长值 {{ member.growth }}
            </p>
          </div>
        </div>
        <div>
          <RouterLink v-if="!auth.isLoggedIn" to="/login">
            <a-button type="default" size="large" shape="round" style="color: var(--primary); font-weight: bold;">登录 / 注册</a-button>
          </RouterLink>
          <a-button v-else type="text" style="color: #fff; background: rgba(255, 255, 255, 0.15);" size="large" shape="round" @click="auth.logout">
            <template #icon><LogoutOutlined /></template>
            退出登录
          </a-button>
        </div>
      </div>
    </a-card>

    <!-- Metrics -->
    <a-row :gutter="[16, 16]" style="margin-bottom: 24px;">
      <a-col :xs="12" :sm="8" :md="4">
        <a-card :bordered="false" style="text-align: center; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05);">
          <WalletOutlined style="font-size: 24px; color: var(--primary); margin-bottom: 8px;" />
          <div style="color: #8c8c8c; font-size: 0.9rem;">余额</div>
          <strong style="font-size: 1.25rem;">￥{{ member.balance }}</strong>
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="8" :md="5">
        <a-card :bordered="false" style="text-align: center; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05);">
          <TrophyOutlined style="font-size: 24px; color: var(--primary); margin-bottom: 8px;" />
          <div style="color: #8c8c8c; font-size: 0.9rem;">积分</div>
          <strong style="font-size: 1.25rem;">{{ member.points }}</strong>
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="8" :md="5">
        <a-card :bordered="false" style="text-align: center; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05);">
          <PropertySafetyOutlined style="font-size: 24px; color: var(--primary); margin-bottom: 8px;" />
          <div style="color: #8c8c8c; font-size: 0.9rem;">优惠券</div>
          <strong style="font-size: 1.25rem;">{{ member.availableCoupons.length }} 张</strong>
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="8" :md="5">
        <a-card :bordered="false" style="text-align: center; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05);">
          <CreditCardOutlined style="font-size: 24px; color: var(--primary); margin-bottom: 8px;" />
          <div style="color: #8c8c8c; font-size: 0.9rem;">礼品卡</div>
          <strong style="font-size: 1.25rem;">￥{{ member.giftCard }}</strong>
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="8" :md="5">
        <a-card :bordered="false" style="text-align: center; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05);">
          <BellOutlined style="font-size: 24px; color: var(--primary); margin-bottom: 8px;" />
          <div style="color: #8c8c8c; font-size: 0.9rem;">未读消息</div>
          <strong style="font-size: 1.25rem;">{{ messages.unreadCount }}</strong>
        </a-card>
      </a-col>
    </a-row>

    <!-- Quick Links -->
    <a-card :bordered="false" title="我的服务" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05);">
      <a-row :gutter="[16, 16]">
        <a-col :xs="8" :sm="6" :md="4">
          <RouterLink to="/orders" class="quick-link">
            <ShoppingOutlined style="font-size: 28px; margin-bottom: 12px; color: var(--primary);" />
            <span>订单记录</span>
          </RouterLink>
        </a-col>
        <a-col :xs="8" :sm="6" :md="4">
          <RouterLink to="/member/addresses" class="quick-link">
            <EnvironmentOutlined style="font-size: 28px; margin-bottom: 12px; color: var(--primary);" />
            <span>收货地址</span>
          </RouterLink>
        </a-col>
        <a-col :xs="8" :sm="6" :md="4">
          <RouterLink to="/member/browse-history" class="quick-link">
            <HistoryOutlined style="font-size: 28px; margin-bottom: 12px; color: var(--primary);" />
            <span>浏览记录</span>
          </RouterLink>
        </a-col>
        <a-col :xs="8" :sm="6" :md="4">
          <RouterLink to="/member/reviews" class="quick-link">
            <CommentOutlined style="font-size: 28px; margin-bottom: 12px; color: var(--primary);" />
            <span>评论晒单</span>
          </RouterLink>
        </a-col>
        <a-col :xs="8" :sm="6" :md="4">
          <RouterLink to="/messages" class="quick-link">
            <MailOutlined style="font-size: 28px; margin-bottom: 12px; color: var(--primary);" />
            <span>站内信</span>
          </RouterLink>
        </a-col>
        <a-col :xs="8" :sm="6" :md="4">
          <RouterLink to="/points-mall" class="quick-link">
            <GiftOutlined style="font-size: 28px; margin-bottom: 12px; color: var(--primary);" />
            <span>积分商城</span>
          </RouterLink>
        </a-col>
      </a-row>
    </a-card>
  </main>
</template>

<style scoped>
.quick-link {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 16px;
  color: #595959;
  text-decoration: none;
  border-radius: 8px;
  transition: all 0.2s;
}
.quick-link:hover {
  background: #f5f5f5;
  color: var(--primary);
}
</style>
