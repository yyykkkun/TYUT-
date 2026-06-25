<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  LogoutOutlined,
  ShoppingOutlined,
  EnvironmentOutlined,
  HistoryOutlined,
  CommentOutlined,
  MailOutlined,
  WalletOutlined,
  BellOutlined,
  PlusOutlined,
} from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useMemberStore } from '@/stores/member'
import { useMessageStore } from '@/stores/messages'
import { uploadFile } from '@/api/upload'
import { put } from '@/api/request'

const auth = useAuthStore()
const member = useMemberStore()
const messages = useMessageStore()
const uploadingAvatar = ref(false)

async function handleAvatarUpload(e: Event) {
  const input = e.target as HTMLInputElement
  if (!input.files?.length) return
  const file = input.files[0]
  if (!file) return
  uploadingAvatar.value = true
  try {
    const url = await uploadFile(file)
    try { await put('/member/avatar', { url }) } catch { /* mock */ }
    auth.user.avatar = url
    localStorage.setItem('mall-user-avatar', url)
  } finally {
    uploadingAvatar.value = false
    input.value = ''
  }
}

// 充值弹窗 - 两步流程
const showRecharge = ref(false)       // 第一步：选金额
const showPayModal = ref(false)       // 第二步：选支付方式
const rechargeAmount = ref(100)
const recharging = ref(false)
const rechargePresets = [50, 100, 200, 500, 1000]
const payMethod = ref<'wechat' | 'alipay' | 'card'>('wechat')

const payMethods = [
  { key: 'wechat' as const, label: '微信支付', icon: '💚' },
  { key: 'alipay' as const, label: '支付宝', icon: '💙' },
  { key: 'card' as const, label: '银行卡', icon: '💳' },
]

function openRecharge(amount: number) {
  rechargeAmount.value = amount
  showRecharge.value = true
}

function goPay() {
  if (rechargeAmount.value <= 0) return
  showRecharge.value = false
  showPayModal.value = true
}

async function doRecharge() {
  if (recharging.value || rechargeAmount.value <= 0) return
  recharging.value = true
  try {
    await member.recharge(rechargeAmount.value)
    showPayModal.value = false
  } finally {
    recharging.value = false
  }
}

onMounted(async () => {
  member.syncLocal()
  await member.loadAll()
  messages.loadMessages()
  messages.loadConversations()
})
</script>

<template>
  <main class="stack-page" style="max-width: 1000px; margin: 0 auto; padding: 40px 20px;">
    <!-- Member Header -->
    <a-card :bordered="false" style="background: linear-gradient(135deg, var(--primary) 0%, oklch(0.5 0.15 150) 100%); color: #fff; border-radius: 12px; margin-bottom: 24px;">
      <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 24px;">
        <div style="display: flex; align-items: center; gap: 24px;">
          <label style="cursor:pointer; position:relative;">
            <a-avatar :size="80" :src="auth.user.avatar" style="background-color: rgba(255, 255, 255, 0.2); font-size: 32px;">
              {{ !auth.user.avatar && auth.isLoggedIn ? auth.user.name.charAt(0).toUpperCase() : '客' }}
            </a-avatar>
            <span v-if="uploadingAvatar" style="position:absolute; inset:0; display:flex; align-items:center; justify-content:center; background:rgba(0,0,0,0.4); border-radius:50%;">
              <a-spin size="small" />
            </span>
            <input type="file" accept="image/*" style="display:none;" @change="handleAvatarUpload" />
          </label>
          <div>
            <p style="margin: 0 0 8px; opacity: 0.9; font-size: 0.9rem;">个人买卖中心</p>
            <h1 style="margin: 0 0 8px; color: #fff; font-size: 2rem;">
              {{ auth.isLoggedIn ? auth.user.name : '游客，请登录' }}
            </h1>
            <p v-if="auth.isLoggedIn" style="margin: 0; opacity: 0.9;">
              买家和卖家为同一身份，可浏览、购买、沟通和发布闲置
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
      <a-col :xs="12" :sm="8" :md="6">
        <a-card :bordered="false" style="text-align: center; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); cursor: pointer;" @click="openRecharge(100)">
          <WalletOutlined style="font-size: 24px; color: var(--primary); margin-bottom: 8px;" />
          <div style="color: #8c8c8c; font-size: 0.9rem;">余额 <PlusOutlined style="font-size: 12px;" /></div>
          <strong style="font-size: 1.25rem;">￥{{ member.balance }}</strong>
          <div style="margin-top: 4px;">
            <RouterLink to="/member/balance" style="font-size: 0.8rem; color: var(--primary);" @click.stop>明细 ›</RouterLink>
          </div>
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="8" :md="6">
        <a-card :bordered="false" style="text-align: center; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05);">
          <ShoppingOutlined style="font-size: 24px; color: var(--primary); margin-bottom: 8px;" />
          <div style="color: #8c8c8c; font-size: 0.9rem;">交易订单</div>
          <strong style="font-size: 1.25rem;">买卖记录</strong>
          <div style="margin-top: 4px;">
            <RouterLink to="/orders" style="font-size: 0.8rem; color: var(--primary);">查看 ›</RouterLink>
          </div>
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="8" :md="6">
        <a-card :bordered="false" style="text-align: center; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05);">
          <CommentOutlined style="font-size: 24px; color: var(--primary); margin-bottom: 8px;" />
          <div style="color: #8c8c8c; font-size: 0.9rem;">用户评分</div>
          <strong style="font-size: 1.25rem;">4.8 分</strong>
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="8" :md="6">
        <a-card :bordered="false" style="text-align: center; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05);">
          <BellOutlined style="font-size: 24px; color: var(--primary); margin-bottom: 8px;" />
          <div style="color: #8c8c8c; font-size: 0.9rem;">未读沟通</div>
          <strong style="font-size: 1.25rem;">{{ messages.chatUnreadCount + messages.unreadCount }}</strong>
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
            <span>用户评价</span>
          </RouterLink>
        </a-col>
        <a-col :xs="8" :sm="6" :md="4">
          <RouterLink to="/messages" class="quick-link">
            <MailOutlined style="font-size: 28px; margin-bottom: 12px; color: var(--primary);" />
            <span>沟通消息</span>
          </RouterLink>
        </a-col>
        <a-col :xs="8" :sm="6" :md="4">
          <RouterLink to="/messages/notifications" class="quick-link">
            <BellOutlined style="font-size: 28px; margin-bottom: 12px; color: var(--primary);" />
            <span>消息通知</span>
          </RouterLink>
        </a-col>
      </a-row>
    </a-card>

    <!-- ====== 第一步：选金额 ====== -->
    <div v-if="showRecharge" class="modal-overlay" @click.self="showRecharge = false">
      <div class="modal">
        <h2>余额充值</h2>
        <p style="color: #8c8c8c; margin: 0 0 16px;">当前余额 ￥{{ member.balance }}</p>

        <div style="display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 16px;">
          <a-button
            v-for="amt in rechargePresets"
            :key="amt"
            :type="rechargeAmount === amt ? 'primary' : 'default'"
            @click="rechargeAmount = amt"
          >
            ￥{{ amt }}
          </a-button>
        </div>

        <a-input-number
          v-model:value="rechargeAmount"
          :min="1"
          :max="99999"
          :step="10"
          style="width: 100%; margin-bottom: 20px;"
          size="large"
          placeholder="输入充值金额"
        />

        <div class="modal-actions">
          <a-button @click="showRecharge = false">取消</a-button>
          <a-button type="primary" @click="goPay">
            下一步
          </a-button>
        </div>
      </div>
    </div>

    <!-- ====== 第二步：选支付方式 ====== -->
    <div v-if="showPayModal" class="modal-overlay" @click.self="showPayModal = false">
      <div class="modal">
        <h2>选择支付方式</h2>
        <p class="modal-amount">
          充值金额 <strong>￥{{ rechargeAmount }}</strong>
        </p>

        <div class="pay-methods">
          <label
            v-for="m in payMethods"
            :key="m.key"
            class="pay-option"
            :class="{ active: payMethod === m.key }"
          >
            <input v-model="payMethod" type="radio" :value="m.key" />
            <span class="pay-icon">{{ m.icon }}</span>
            <div>
              <strong>{{ m.label }}</strong>
            </div>
          </label>
        </div>

        <div class="modal-actions">
          <a-button @click="showPayModal = false">取消</a-button>
          <a-button type="primary" :loading="recharging" @click="doRecharge">
            {{ recharging ? '支付中...' : `确认支付 ￥${rechargeAmount}` }}
          </a-button>
        </div>
      </div>
    </div>
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

.modal-overlay {
  position: fixed;
  inset: 0;
  background: oklch(0.18 0.03 245 / 0.52);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 40;
  padding: 1rem;
}
.modal {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: 0 18px 42px oklch(0.18 0.03 245 / 0.24);
  padding: 1.5rem;
  width: 400px;
  max-width: 90vw;
}
.modal h2 {
  margin: 0 0 0.5rem;
}
.modal-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
}
.modal-amount {
  color: var(--ink-muted);
  margin: 0 0 1.25rem;
}
.modal-amount strong {
  color: var(--price);
  font-size: 1.2rem;
}
.pay-methods {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  margin-bottom: 1.5rem;
}
.pay-option {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background-color 0.15s ease, border-color 0.15s ease;
}
.pay-option:hover {
  background: var(--surface-soft);
}
.pay-option.active {
  border-color: var(--primary);
  background: var(--primary-soft);
}
.pay-option input {
  display: none;
}
.pay-icon {
  font-size: 1.5rem;
}
.pay-option strong {
  display: block;
  font-size: 0.95rem;
}
</style>
