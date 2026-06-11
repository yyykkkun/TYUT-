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
const price = ref<OrderPreview>({
  subtotal: 0,
  couponAmount: 0,
  pointsAmount: 0,
  giftCardAmount: 0,
  freight: 0,
  total: 0,
})
const submitting = ref(false)

async function loadPreview() {
  if (!cart.selectedItems.length) return
  try {
    price.value = await orders.preview()
  } catch {
    /* ignore */
  }
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
  await Promise.all([
    cart.loadCart(),
    member.loadAddresses(),
    member.loadCoupons(),
    member.loadProfile(),
  ])
  loadPreview()
})
</script>

<template>
  <main class="stack-page" style="max-width: 1180px; margin: 0 auto; padding: 40px 20px;">
    <div class="page-title" style="margin-bottom: 32px;">
      <h1 style="font-size: 2rem; margin-bottom: 8px;">订单确认</h1>
      <p style="color: #8c8c8c; margin: 0;">地址、配送、优惠券、积分和礼品卡抵扣在这里完成试算。</p>
    </div>

    <EmptyState
      v-if="!cart.selectedItems.length"
      title="没有待结算商品"
      action-text="返回购物车"
      action-to="/cart"
    />

    <section v-else class="checkout-grid" style="display: grid; grid-template-columns: 1fr 340px; gap: 24px; align-items: flex-start;">
      <div class="checkout-main" style="display: flex; flex-direction: column; gap: 24px;">
        <section class="panel" style="background: #fff; padding: 24px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05);">
          <div class="section-heading tight" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
            <h2 style="margin: 0; font-size: 1.25rem;">收货地址</h2>
            <RouterLink to="/member/addresses" style="color: var(--primary);">管理地址</RouterLink>
          </div>
          <article v-if="member.defaultAddress" class="address-card selected" style="padding: 16px; border: 2px solid var(--primary); border-radius: 8px; background: var(--primary-soft);">
            <strong style="display: block; margin-bottom: 4px; font-size: 1rem;">{{ member.defaultAddress.name }} {{ member.defaultAddress.phone }}</strong>
            <p style="margin: 0; color: #595959;">
              {{ member.defaultAddress.province }} {{ member.defaultAddress.city }}
              {{ member.defaultAddress.district }} {{ member.defaultAddress.detail }}
            </p>
          </article>
        </section>

        <section class="panel" style="background: #fff; padding: 24px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05);">
          <h2 style="margin-bottom: 16px; font-size: 1.25rem;">配送方式</h2>
          <a-radio-group v-model:value="orders.deliveryMethod" option-type="button" button-style="solid" size="large" @change="loadPreview">
            <a-radio-button value="platform">平台物流</a-radio-button>
            <a-radio-button value="third-party">第三方物流</a-radio-button>
            <a-radio-button value="pickup">门店自提</a-radio-button>
          </a-radio-group>
        </section>

        <section class="panel" style="background: #fff; padding: 24px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05);">
          <h2 style="margin-bottom: 16px; font-size: 1.25rem;">优惠抵扣</h2>
          <a-form layout="vertical">
            <a-form-item label="优惠券">
              <a-select v-model:value="orders.selectedCouponId" style="width: 100%; max-width: 400px;" size="large" @change="loadPreview">
                <a-select-option value="">不使用优惠券</a-select-option>
                <a-select-option v-for="coupon in member.availableCoupons" :key="coupon.id" :value="coupon.id">
                  {{ coupon.title }} · {{ coupon.expiresAt }} 到期
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item>
              <a-checkbox v-model:checked="orders.usePoints" @change="loadPreview">
                使用积分抵扣，可用 {{ member.points }} 分
              </a-checkbox>
            </a-form-item>
            <a-form-item>
              <a-checkbox v-model:checked="orders.useGiftCard" @change="loadPreview">
                使用礼品卡，可用 ￥{{ member.giftCard }}
              </a-checkbox>
            </a-form-item>
          </a-form>
        </section>
      </div>

      <aside class="summary-panel" style="background: #fff; padding: 24px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); position: sticky; top: 24px;">
        <h2 style="margin-bottom: 24px; font-size: 1.25rem;">金额明细</h2>
        <div style="display: flex; flex-direction: column; gap: 16px; margin-bottom: 24px;">
          <div style="display: flex; justify-content: space-between;">
            <span style="color: #8c8c8c;">商品金额</span><strong style="font-size: 1.1rem;">￥{{ price.subtotal.toFixed(2) }}</strong>
          </div>
          <div style="display: flex; justify-content: space-between;">
            <span style="color: #8c8c8c;">优惠券</span><strong style="color: var(--danger); font-size: 1.1rem;">-￥{{ price.couponAmount.toFixed(2) }}</strong>
          </div>
          <div style="display: flex; justify-content: space-between;">
            <span style="color: #8c8c8c;">积分抵扣</span><strong style="color: var(--danger); font-size: 1.1rem;">-￥{{ price.pointsAmount.toFixed(2) }}</strong>
          </div>
          <div style="display: flex; justify-content: space-between;">
            <span style="color: #8c8c8c;">礼品卡</span><strong style="color: var(--danger); font-size: 1.1rem;">-￥{{ price.giftCardAmount.toFixed(2) }}</strong>
          </div>
          <div style="display: flex; justify-content: space-between;">
            <span style="color: #8c8c8c;">运费</span><strong style="font-size: 1.1rem;">￥{{ price.freight.toFixed(2) }}</strong>
          </div>
        </div>
        <div style="border-top: 1px solid #f0f0f0; margin-bottom: 24px; padding-top: 16px;">
          <div style="display: flex; justify-content: space-between; align-items: baseline;">
            <span style="font-size: 1.1rem;">应付</span>
            <strong style="color: var(--price); font-size: 1.75rem;">￥{{ price.total.toFixed(2) }}</strong>
          </div>
        </div>
        <a-button type="primary" size="large" block :loading="submitting" @click="submit" style="height: 48px; font-size: 1.1rem; border-radius: 8px;">
          提交订单
        </a-button>
      </aside>
    </section>
  </main>
</template>
