<script setup lang="ts">
import { onMounted } from 'vue'
import { DeleteOutlined } from '@ant-design/icons-vue'
import EmptyState from '@/components/EmptyState.vue'
import { useCartStore } from '@/stores/cart'

const cart = useCartStore()

onMounted(() => {
  cart.loadCart()
})
</script>

<template>
  <main class="stack-page cart-page">
    <div class="page-title">
      <h1 style="font-size: 2rem; margin-bottom: 8px">购物车</h1>
      <p style="color: #8c8c8c; margin: 0">勾选商品、修改数量后进入结算。</p>
    </div>

    <EmptyState
      v-if="!cart.detailedItems.length"
      title="购物车还是空的"
      action-text="去逛逛"
      action-to="/products"
    />

    <section v-else class="cart-layout">
      <div class="line-list">
        <article v-for="item in cart.detailedItems" :key="item.id" class="line-item">
          <a-checkbox :checked="item.selected" @change="cart.toggleSelected(item.id)" />
          <img
            :src="item.product?.image || ''"
            :alt="item.product?.title || item.productId"
            style="width: 80px; height: 80px; border-radius: 8px; object-fit: cover"
          />
          <div style="flex: 1; min-width: 0">
            <h3
              style="
                margin: 0 0 8px;
                font-size: 1.1rem;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
              "
            >
              {{ item.product?.title || item.productId }}
            </h3>
            <p style="margin: 0; color: #8c8c8c">
              {{ item.spec }} ·
              <span style="color: var(--price)">￥{{ item.product?.price ?? '--' }}</span>
            </p>
          </div>
          <a-input-number
            :value="item.quantity"
            :min="1"
            style="width: 80px"
            @change="(val: any) => cart.updateQuantity(item.id, Number(val))"
          />
          <strong style="width: 100px; text-align: right; color: var(--price); font-size: 1.1rem"
            >￥{{ ((item.product?.price ?? 0) * item.quantity).toFixed(2) }}</strong
          >
          <a-button type="text" danger @click="cart.removeItem(item.id)">
            <template #icon><DeleteOutlined /></template>
          </a-button>
        </article>
      </div>

      <aside class="summary-panel">
        <h2 style="margin-bottom: 24px; font-size: 1.25rem">结算汇总</h2>
        <div style="display: flex; flex-direction: column; gap: 16px; margin-bottom: 24px">
          <div style="display: flex; justify-content: space-between">
            <span style="color: #8c8c8c">已选数量</span
            ><strong style="font-size: 1.1rem">{{ cart.selectedCount }}</strong>
          </div>
          <div style="display: flex; justify-content: space-between">
            <span style="color: #8c8c8c">商品小计</span
            ><strong style="color: var(--price); font-size: 1.5rem"
              >￥{{ cart.subtotal.toFixed(2) }}</strong
            >
          </div>
        </div>
        <a-button
          type="primary"
          size="large"
          block
          :disabled="!cart.selectedCount"
          @click="$router.push('/checkout')"
          style="height: 48px; font-size: 1.1rem; border-radius: 8px"
        >
          去结算
        </a-button>
      </aside>
    </section>
  </main>
</template>
