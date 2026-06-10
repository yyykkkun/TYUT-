<script setup lang="ts">
import { onMounted } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import { useCartStore } from '@/stores/cart'

const cart = useCartStore()

onMounted(() => {
  cart.loadCart()
})
</script>

<template>
  <main class="stack-page">
    <div class="page-title">
      <h1>购物车</h1>
      <p>勾选商品、修改数量后进入结算。</p>
    </div>

    <EmptyState v-if="!cart.detailedItems.length" title="购物车还是空的" action-text="去逛逛" action-to="/products" />

    <section v-else class="cart-layout">
      <div class="line-list">
        <article v-for="item in cart.detailedItems" :key="item.id" class="line-item">
          <input type="checkbox" :checked="item.selected" @change="cart.toggleSelected(item.id)" />
          <img :src="item.product?.image || ''" :alt="item.product?.title || item.productId" />
          <div>
            <h3>{{ item.product?.title || item.productId }}</h3>
            <p>{{ item.spec }} · ￥{{ item.product?.price ?? '--' }}</p>
          </div>
          <input class="small-input" :value="item.quantity" type="number" min="1" @input="cart.updateQuantity(item.id, Number(($event.target as HTMLInputElement).value))" />
          <strong>￥{{ ((item.product?.price ?? 0) * item.quantity) || 0 }}</strong>
          <button class="icon-btn" type="button" title="删除" @click="cart.removeItem(item.id)">×</button>
        </article>
      </div>

      <aside class="summary-panel">
        <h2>结算汇总</h2>
        <p><span>已选数量</span><strong>{{ cart.selectedCount }}</strong></p>
        <p><span>商品小计</span><strong>￥{{ cart.subtotal }}</strong></p>
        <RouterLink class="btn primary block" :class="{ disabled: !cart.selectedCount }" to="/checkout">去结算</RouterLink>
      </aside>
    </section>
  </main>
</template>
