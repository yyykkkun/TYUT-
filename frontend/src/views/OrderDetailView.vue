<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import EmptyState from '@/components/EmptyState.vue'
import OrderStatusTag from '@/components/OrderStatusTag.vue'
import { useOrderStore } from '@/stores/orders'
import { useMemberStore } from '@/stores/member'
import { uploadFiles } from '@/api/upload'
import { put } from '@/api/request'
import type { Order } from '@/types/domain'

const route = useRoute()
const router = useRouter()
const orders = useOrderStore()
const member = useMemberStore()

const order = ref<Order | undefined>(undefined)
const loading = ref(false)
const review = ref('商品体验不错，物流也很及时。')
const rating = ref(5)

// 评论图片
const reviewImages = ref<string[]>([])
const uploading = ref(false)

// 支付弹窗
const showPayModal = ref(false)
const payMethod = ref<'wechat' | 'alipay' | 'card' | 'balance'>('wechat')
const paying = ref(false)
const payError = ref('')

// 30分钟倒计时
const PAY_TIMEOUT_MIN = 30
const remainingSeconds = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const countdownText = computed(() => {
  if (remainingSeconds.value <= 0) return ''
  const m = Math.floor(remainingSeconds.value / 60)
  const s = remainingSeconds.value % 60
  return `${m}分${s}秒`
})

function startCountdown() {
  stopCountdown()
  if (!order.value || order.value.status !== 'pending_payment') return
  const created = new Date(order.value.createdAt.replace(' ', 'T'))
  if (isNaN(created.getTime())) return
  const deadline = created.getTime() + PAY_TIMEOUT_MIN * 60 * 1000

  const tick = () => {
    const left = Math.max(0, Math.floor((deadline - Date.now()) / 1000))
    remainingSeconds.value = left
    if (left <= 0) {
      stopCountdown()
      doCancel()
    }
  }
  tick()
  countdownTimer = setInterval(tick, 1000)
}

function stopCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

onUnmounted(stopCountdown)

const payMethods = [
  { key: 'wechat' as const, label: '微信支付', icon: '微', desc: '微信安全支付' },
  { key: 'alipay' as const, label: '支付宝', icon: '支', desc: '支付宝安全支付' },
  { key: 'card' as const, label: '银行卡', icon: '卡', desc: '支持主流银行借记卡/信用卡' },
  { key: 'balance' as const, label: '余额支付', icon: '余', desc: '使用账户余额直接支付' },
]

function paymentLabel(method?: string) {
  const map: Record<string, string> = {
    wechat: '微信支付',
    alipay: '支付宝',
    card: '银行卡',
    balance: '余额支付',
  }
  return method ? map[method] || method : '—'
}

async function loadOrder() {
  const id = String(route.params.id)
  loading.value = true
  try {
    await orders.loadOrders()
    let result = orders.orders.find((item) => item.id === id)
    if (!result) {
      result = await orders.getOrder(id)
    }
    if (result) {
      // 已过期未支付订单自动取消
      if (result.status === 'pending_payment') {
        const created = new Date(result.createdAt.replace(' ', 'T'))
        if (
          !isNaN(created.getTime()) &&
          Date.now() - created.getTime() > PAY_TIMEOUT_MIN * 60 * 1000
        ) {
          result.status = 'cancelled'
          await orders.cancel(id).catch(() => {})
        }
      }
      order.value = result
      // 自动打开支付弹窗
      if (route.query.pay === '1' && result.status === 'pending_payment') {
        router.replace({ path: `/orders/${id}` })
        setTimeout(() => openPay(), 300)
      }
      startCountdown()
    }
  } finally {
    loading.value = false
  }
}

let autoRefreshTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  loadOrder()
  // paid/shipping 状态每 10 秒自动刷新，等待卖家发货
  autoRefreshTimer = setInterval(async () => {
    if (order.value && (order.value.status === 'paid' || order.value.status === 'shipping')) {
      await loadOrder()
    }
  }, 10000)
})

watch(
  () => route.params.id,
  () => {
    loadOrder()
    if (autoRefreshTimer) clearInterval(autoRefreshTimer)
  },
)

onUnmounted(() => {
  stopCountdown()
  if (autoRefreshTimer) clearInterval(autoRefreshTimer)
})

async function openPay() {
  payError.value = ''
  member.syncLocal()
  await member.loadProfile()
  showPayModal.value = true
}

async function doPay() {
  if (!order.value || paying.value) return
  payError.value = ''

  if (payMethod.value === 'balance' && member.balance < order.value.total) {
    payError.value = `余额不足（当前 ￥${member.balance}，需支付 ￥${order.value.total}）`
    return
  }

  paying.value = true
  try {
    await orders.pay(order.value.id, payMethod.value)
    showPayModal.value = false
    stopCountdown()
    // 乐观更新 + 同步数据源
    order.value.status = 'paid'
    order.value.paidAt = new Date().toISOString().replace('T', ' ').slice(0, 19)
    order.value.paymentMethod = payMethod.value
    await member.syncFromServer()
    await loadOrder()
  } finally {
    paying.value = false
  }
}

async function doConfirm() {
  if (!order.value) return
  order.value.status = 'completed'
  await orders.confirm(order.value.id)
  member.syncLocal()
  await loadOrder()
}

async function doCancel() {
  if (!order.value) return
  order.value.status = 'cancelled'
  stopCountdown()
  await orders.cancel(order.value.id)
  member.syncLocal()
  await loadOrder()
}

async function handleUpload(e: Event) {
  const input = e.target as HTMLInputElement
  if (!input.files?.length) return
  uploading.value = true
  try {
    const urls = await uploadFiles(Array.from(input.files))
    reviewImages.value.push(...urls)
  } finally {
    uploading.value = false
    input.value = ''
  }
}

function removeImage(idx: number) {
  reviewImages.value.splice(idx, 1)
}

async function doRefund() {
  if (!order.value) return
  const reason = prompt('请输入退款原因：')
  if (!reason) return
  try {
    await put(`/orders/${order.value.id}/refund`, { reason })
    order.value.refundStatus = 'pending'
    order.value.refundReason = reason
    alert('退款申请已提交，请等待审核')
  } catch (e: any) {
    alert(e.message || '申请失败')
  }
}

async function doReview() {
  if (!order.value) return
  let text = review.value
  if (reviewImages.value.length) {
    text += '\n[图片]' + reviewImages.value.join(',')
  }
  await orders.review(order.value.id, text, rating.value)
  reviewImages.value = []
  await loadOrder()
}
</script>

<template>
  <main v-if="order" class="stack-page">
    <div class="page-title row">
      <div>
        <h1>订单详情</h1>
        <p>{{ order.id }} · {{ order.createdAt }}</p>
      </div>
      <OrderStatusTag :status="order.status" />
    </div>

    <section class="checkout-grid">
      <div class="checkout-main">
        <!-- 商品明细 -->
        <section class="panel">
          <h2>商品明细</h2>
          <article v-for="item in order.items" :key="item.productId" class="line-item compact">
            <img :src="item.image" :alt="item.title" />
            <div>
              <h3>{{ item.title }}</h3>
              <p>{{ item.spec }} × {{ item.quantity }}</p>
            </div>
            <strong>￥{{ item.price * item.quantity }}</strong>
          </article>
        </section>

        <!-- 收货信息 -->
        <section class="panel">
          <h2>收货信息</h2>
          <p>
            <strong>{{ order.address.name }}</strong> {{ order.address.phone }}
          </p>
          <p>
            {{ order.address.province }} {{ order.address.city }} {{ order.address.district }}
            {{ order.address.detail }}
          </p>
          <p>
            配送方式：{{
              order.deliveryMethod === 'pickup'
                ? '门店自提'
                : order.deliveryMethod === 'third-party'
                  ? '第三方物流'
                  : '平台物流'
            }}
          </p>
        </section>

        <!-- 物流轨迹 -->
        <section v-if="order.status !== 'cancelled'" class="panel">
          <h2>物流状态</h2>
          <ol class="timeline">
            <li>订单已创建{{ order.status === 'pending_payment' ? '，等待付款' : '' }}</li>
            <li v-if="order.status !== 'pending_payment'">
              支付成功 · 运单号 {{ order.deliveryNo || '---' }}
            </li>
            <li v-if="order.status === 'shipping' || order.status === 'completed'">
              包裹已发出，正在配送
            </li>
            <li v-if="order.status === 'completed'">已确认收货，交易完成</li>
          </ol>
        </section>

        <!-- 评价 -->
        <section v-if="order.status === 'completed'" class="panel">
          <h2>评价晒单</h2>
          <blockquote v-if="order.review">{{ order.review }}</blockquote>
          <div v-else>
            <div style="margin-bottom: 12px; display: flex; align-items: center; gap: 8px">
              <span style="font-weight: 700">评分：</span>
              <a-rate v-model:value="rating" :count="5" />
            </div>
            <textarea
              v-model="review"
              rows="4"
              placeholder="分享你的购物体验..."
              style="width: 100%; margin-bottom: 12px"
            ></textarea>
            <!-- 已选图片预览 -->
            <div
              v-if="reviewImages.length"
              style="display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 12px"
            >
              <div
                v-for="(url, idx) in reviewImages"
                :key="idx"
                style="position: relative; width: 80px; height: 80px"
              >
                <img
                  :src="url"
                  style="width: 80px; height: 80px; object-fit: cover; border-radius: 6px"
                />
                <button
                  @click="removeImage(idx)"
                  style="
                    position: absolute;
                    top: -6px;
                    right: -6px;
                    width: 20px;
                    height: 20px;
                    border-radius: 50%;
                    border: none;
                    background: var(--danger);
                    color: #fff;
                    font-size: 12px;
                    cursor: pointer;
                    line-height: 20px;
                  "
                >
                  ×
                </button>
              </div>
            </div>
            <!-- 上传按钮 -->
            <label
              style="
                display: inline-flex;
                align-items: center;
                gap: 6px;
                cursor: pointer;
                color: var(--primary);
                font-weight: 700;
                margin-bottom: 12px;
              "
            >
              📷 {{ uploading ? '上传中...' : '添加图片' }}
              <input
                type="file"
                multiple
                accept="image/*"
                style="display: none"
                @change="handleUpload"
                :disabled="uploading"
              />
            </label>
            <br />
            <button class="btn primary" type="button" @click="doReview" :disabled="uploading">
              提交评价
            </button>
          </div>
        </section>
      </div>

      <!-- 侧边栏 - 金额与操作 -->
      <aside class="summary-panel">
        <h2>金额明细</h2>
        <p>
          <span>优惠券</span><strong>-￥{{ order.couponAmount }}</strong>
        </p>
        <p>
          <span>积分</span><strong>-{{ order.pointsUsed }} 分</strong>
        </p>
        <p>
          <span>礼品卡</span><strong>-￥{{ order.giftCardAmount }}</strong>
        </p>
        <p v-if="order.balanceAmount > 0">
          <span>余额支付</span><strong>-￥{{ order.balanceAmount }}</strong>
        </p>
        <p>
          <span>运费</span><strong>￥{{ order.freight }}</strong>
        </p>
        <p class="total">
          <span>实付</span><strong>￥{{ order.total }}</strong>
        </p>
        <p
          v-if="order.paymentMethod"
          style="margin-top: 8px; padding-top: 8px; border-top: 1px solid #f0f0f0"
        >
          <span>支付方式</span><strong>{{ paymentLabel(order.paymentMethod) }}</strong>
        </p>

        <!-- 倒计时 -->
        <p
          v-if="order.status === 'pending_payment' && remainingSeconds > 0"
          style="text-align: center; color: var(--warning); font-weight: 700; margin-bottom: 8px"
        >
          限时支付：请在 {{ countdownText }} 内完成支付，超时自动取消
        </p>

        <!-- 操作按钮 -->
        <button
          v-if="order.status === 'pending_payment'"
          class="btn primary block"
          @click="openPay"
        >
          去支付
        </button>
        <button
          v-if="order.status === 'paid' || order.status === 'shipping'"
          class="btn primary block"
          @click="doConfirm"
        >
          确认收货
        </button>
        <button
          v-if="order.status === 'pending_payment'"
          class="btn subtle block"
          @click="doCancel"
        >
          取消订单
        </button>
        <!-- 退款按钮 -->
        <button
          v-if="
            (order.status === 'completed' || order.status === 'paid') &&
            (!order.refundStatus || order.refundStatus === 'rejected')
          "
          class="btn subtle block"
          style="color: var(--danger)"
          @click="doRefund"
        >
          申请退款
        </button>
        <p
          v-if="order.refundStatus === 'pending'"
          style="color: var(--warning); text-align: center; font-weight: 700"
        >
          退款审核中
        </p>
        <p
          v-if="order.refundStatus === 'completed'"
          style="color: var(--success); text-align: center; font-weight: 700"
        >
          已退款
        </p>
      </aside>
    </section>

    <!-- ====== 支付弹窗 ====== -->
    <div v-if="showPayModal" class="modal-overlay" @click.self="showPayModal = false">
      <div class="modal">
        <h2>选择支付方式</h2>
        <p class="modal-amount">
          应付金额 <strong>￥{{ order.total }}</strong>
        </p>

        <p
          v-if="payError"
          style="
            color: var(--danger);
            background: var(--danger-soft);
            padding: 8px 12px;
            border-radius: 6px;
            margin-bottom: 12px;
            font-size: 0.9rem;
          "
        >
          {{ payError }}
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
              <small>{{ m.desc }}</small>
            </div>
          </label>
        </div>

        <div class="modal-actions">
          <button class="btn" @click="showPayModal = false">取消</button>
          <button class="btn primary" :disabled="paying" @click="doPay">
            {{ paying ? '支付中...' : `确认支付 ￥${order.total}` }}
          </button>
        </div>
      </div>
    </div>
  </main>

  <div v-else-if="loading" style="padding: 40px; text-align: center">
    <a-spin size="large" />
  </div>
  <EmptyState v-else title="订单不存在" action-text="返回订单中心" action-to="/orders" />
</template>

<style scoped>
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
  transition:
    background-color 0.15s ease,
    border-color 0.15s ease;
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
.pay-option small {
  color: var(--ink-muted);
  font-size: 0.8rem;
}
.modal-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
}
</style>
