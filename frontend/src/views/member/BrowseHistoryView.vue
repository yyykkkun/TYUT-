<script setup lang="ts">
import { computed, onMounted } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import ProductCard from '@/components/ProductCard.vue'
import { useProductStore } from '@/stores/products'
import { useMemberStore } from '@/stores/member'
import type { Product } from '@/types/domain'

const productStore = useProductStore()
const member = useMemberStore()
const historyProducts = computed(() =>
  productStore.browseHistory
    .map((id) => productStore.getProduct(id))
    .filter((product): product is Product => Boolean(product)),
)
</script>

<template>
  <main class="stack-page">
    <div class="page-title">
      <h1>浏览记录</h1>
      <p>近期查看过的商品会用于猜你喜欢兜底推荐。</p>
    </div>
    <div v-if="historyProducts.length" class="product-grid">
      <ProductCard v-for="product in historyProducts" :key="product.id" :product="product" />
    </div>
    <EmptyState v-else title="还没有浏览记录" action-text="去看商品" action-to="/products" />
  </main>
</template>
