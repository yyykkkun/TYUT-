<script setup lang="ts">
import { ref, onMounted } from 'vue'
import CountdownTimer from '@/components/CountdownTimer.vue'
import ProductCard from '@/components/ProductCard.vue'
import { fetchSeckillProducts } from '@/api/products'
import type { Product } from '@/types/domain'

const items = ref<Product[]>([])

onMounted(async () => {
  items.value = await fetchSeckillProducts()
})
</script>

<template>
  <main class="stack-page">
    <div class="page-title row">
      <div>
        <h1>限时秒杀</h1>
        <p>精选好物限时特惠。</p>
      </div>
      <CountdownTimer />
    </div>
    <div class="product-grid">
      <ProductCard v-for="item in items" :key="item.id" :product="item" />
    </div>
  </main>
</template>
