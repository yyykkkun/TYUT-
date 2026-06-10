<script setup lang="ts">
import { ref, onMounted } from 'vue'
import CountdownTimer from '@/components/CountdownTimer.vue'
import ProductCard from '@/components/ProductCard.vue'
import { fetchGroupBuyProducts } from '@/api/products'
import type { Product } from '@/types/domain'

const items = ref<Product[]>([])

onMounted(async () => {
  items.value = await fetchGroupBuyProducts()
})
</script>

<template>
  <main class="stack-page">
    <div class="page-title row">
      <div>
        <h1>拼团购</h1>
        <p>呼朋引伴一起拼，价格更优惠。</p>
      </div>
      <CountdownTimer />
    </div>
    <div class="product-grid">
      <ProductCard v-for="item in items" :key="item.id" :product="item" />
    </div>
  </main>
</template>
