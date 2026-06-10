<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import EmptyState from '@/components/EmptyState.vue'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import { useMemberStore } from '@/stores/member'
import { useOrderStore } from '@/stores/orders'
import type { OrderPreview } from '@/api/orders'

const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()
const member = useMemberStore()
const orders = useOrderStore()
const price = ref<OrderPreview>({ subtotal: 0, couponAmount: 0, pointsAmount: 0, giftCardAmount: 0, freight: 0, total: 0 })
const submitting = ref(false)

async function loadPreview() {
  if (!cart.selectedItems.length) return
  try {
    price.value = await orders.preview()
  } catch { /* ignore */ }
}

async function submit() {
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: '/checkout' } })
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    const orderId = await orders.submitOrder()
    if (orderId) {
      cart.clearSelected()
      router.push(`/orders/${orderId}`)
    }
  } catch (e: any) {
    alert(e.message || '下单失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await Promise.all([cart.loadCart(), member.loadAddresses(), member.loadCoupons(), member.loadProfile()])
  loadPreview()
})
</script>

<template>
  <main class="stack-page">
    <div class="page-title">
      <h1>订单确认</h1>
      <p>地址、配送、优惠券、积分和礼品卡抵扣在这里完成试算。</p>
    </div>

    <EmptyState v-if="!cart.selectedItems.length" title="没有待结算商品" action-text="返回购物车" action-to="/cart" />

    <section v-else class="checkout-grid">
      <div class="checkout-main">
        <section class="panel">
          <div class="section-heading tight">
            <h2>收货地址</h2>
            <RouterLink to="/member/addresses">管理地址</RouterLink>
          </div>
          <article v-if="member.defaultAddress" class="address-card selected">
            <strong>{{ member.defaultAddress.name }} {{ member.defaultAddress.phone }}</strong>
            <p>{{ member.defaultAddress.province }} {{ member.defaultAddress.city }} {{ member.defaultAddress.district }} {{ member.defaultAddress.detail }}</p>
          </article>
        </section>

        <section class="panel">
          <h2>配送方式</h2>
          <div class="segmented">
            <button :class="{ active: orders.deliveryMethod === 'platform' }" @click="orders.deliveryMethod = 'platform'; loadPreview()">平台物流</button>
            <button :class="{ active: orders.deliveryMethod === 'third-party' }" @click="orders.deliveryMethod = 'third-party'; loadPreview()">第三方物流</button>
            <button :class="{ active: orders.deliveryMethod === 'pickup' }" @click="orders.deliveryMethod = 'pickup'; loadPreview()">门店自提</button>
          </div>
        </section>

        <section class="panel">
          <h2>优惠抵扣</h2>
          <label>
            优惠券
            <select v-model="orders.selectedCouponId" @change="loadPreview()">
              <option value="">不使用优惠券</option>
              <option v-for="coupon in member.availableCoupons" :key="coupon.id" :value="coupon.id">
                {{ coupon.title }} · {{ coupon.expiresAt }} 到期
              </option>
            </select>
          </label>
          <label class="checkbox-line">
            <input v-model="orders.usePoints" type="checkbox" @change="loadPreview()" />
            使用积分抵扣，可用 {{ member.points }} 分
          </label>
          <label class="checkbox-line">
            <input v-model="orders.useGiftCard" type="checkbox" @change="loadPreview()" />
            使用礼品卡，可用 ￥{{ member.giftCard }}
          </label>
        </section>
      </div>

      <aside class="summary-panel">
        <h2>金额明细</h2>
        <p><span>商品金额</span><strong>￥{{ price.subtotal }}</strong></p>
        <p><span>优惠券</span><strong>-￥{{ price.couponAmount }}</strong></p>
        <p><span>积分抵扣</span><strong>-￥{{ price.pointsAmount }}</strong></p>
        <p><span>礼品卡</span><strong>-￥{{ price.giftCardAmount }}</strong></p>
        <p><span>运费</span><strong>￥{{ price.freight }}</strong></p>
        <p class="total"><span>应付</span><strong>￥{{ price.total }}</strong></p>
        <button class="btn primary block" type="button" :disabled="submitting" @click="submit">提交订单</button>
      </aside>
    </section>
  </main>
</template>
