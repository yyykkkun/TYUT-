<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import EmptyState from '@/components/EmptyState.vue'
import OrderStatusTag from '@/components/OrderStatusTag.vue'
import { useOrderStore } from '@/stores/orders'
import type { Order } from '@/types/domain'

const route = useRoute()
const orders = useOrderStore()

const order = ref<Order | undefined>(undefined)
const review = ref('商品体验不错，物流也很及时。')

// 支付弹窗
const showPayModal = ref(false)
const payMethod = ref<'wechat' | 'alipay' | 'card'>('wechat')
const paying = ref(false)

const payMethods = [
  { key: 'wechat' as const, label: '微信支付', icon: '💚', desc: '微信安全支付' },
  { key: 'alipay' as const, label: '支付宝', icon: '💙', desc: '支付宝安全支付' },
  { key: 'card' as const, label: '银行卡', icon: '💳', desc: '支持主流银行借记卡/信用卡' },
]

async function loadOrder() {
  const id = String(route.params.id)
  order.value = await orders.getOrder(id)
}

onMounted(loadOrder)
watch(() => route.params.id, loadOrder)

function openPay() {
  showPayModal.value = true
}

async function doPay() {
  if (!order.value || paying.value) return
  paying.value = true
  try {
    await orders.pay(order.value.id)
    showPayModal.value = false
    await loadOrder()
  } finally {
    paying.value = false
  }
}

async function doConfirm() {
  if (!order.value) return
  await orders.confirm(order.value.id)
  await loadOrder()
}

async function doCancel() {
  if (!order.value) return
  await orders.cancel(order.value.id)
  await loadOrder()
}

async function doReview() {
  if (!order.value) return
  await orders.review(order.value.id, review.value)
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
          <div v-else class="form-grid">
            <textarea v-model="review" rows="4" placeholder="分享你的购物体验..."></textarea>
            <button class="btn" type="button" @click="doReview">提交评价</button>
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
        <p>
          <span>运费</span><strong>￥{{ order.freight }}</strong>
        </p>
        <p class="total">
          <span>实付</span><strong>￥{{ order.total }}</strong>
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
      </aside>
    </section>

    <!-- ====== 支付弹窗 ====== -->
    <div v-if="showPayModal" class="modal-overlay" @click.self="showPayModal = false">
      <div class="modal">
        <h2>选择支付方式</h2>
        <p class="modal-amount">
          应付金额 <strong>￥{{ order.total }}</strong>
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
