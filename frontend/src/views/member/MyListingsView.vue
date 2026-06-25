<script setup lang="ts">
import { onMounted, ref } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import ProductCard from '@/components/ProductCard.vue'
import { fetchMyProducts } from '@/api/products'
import type { Product } from '@/types/domain'

const products = ref<Product[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    products.value = await fetchMyProducts()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <main class="stack-page">
    <div class="page-title row">
      <div>
        <p class="eyebrow">卖家中心</p>
        <h1>我的发布</h1>
        <p>查看当前账号发布到闲置市场的商品。</p>
      </div>
      <a-button type="primary" @click="$router.push('/products/new')">发布闲置</a-button>
    </div>

    <a-spin v-if="loading" class="center-spin" />
    <div v-else-if="products.length" class="product-grid">
      <ProductCard v-for="product in products" :key="product.id" :product="product" />
    </div>
    <EmptyState
      v-else
      title="还没有发布商品"
      action-text="去发布"
      action-to="/products/new"
    />
  </main>
</template>
