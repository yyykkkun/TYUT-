<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import ProductCard from '@/components/ProductCard.vue'
import { useProductStore } from '@/stores/products'
import { useMemberStore } from '@/stores/member'
import type { Product } from '@/types/domain'

const productStore = useProductStore()
const member = useMemberStore()
const loading = ref(false)

// 合并后端和本地的浏览记录（去重）
const allIds = computed(() => {
  const merged = [...member.browseHistory, ...productStore.browseHistory]
  return [...new Set(merged)]
})

const historyProducts = computed(() =>
  productStore.getBrowseHistoryProducts(allIds.value),
)

onMounted(async () => {
  loading.value = true
  // 同时加载后端浏览记录和首页商品数据（补充 getProduct 的搜索范围）
  await Promise.all([
    member.loadBrowseHistory(),
    productStore.loadHomeData().catch(() => {}),
  ])
  loading.value = false
})
</script>

<template>
  <main class="stack-page">
    <div class="page-title">
      <h1>浏览记录</h1>
      <p>近期查看过的商品会用于猜你喜欢兜底推荐。</p>
    </div>

    <div v-if="loading" style="padding: 40px; text-align: center;">
      <a-spin size="large" />
    </div>

    <div v-else-if="historyProducts.length" class="product-grid">
      <ProductCard v-for="product in historyProducts" :key="product.id" :product="product" />
    </div>
    <EmptyState v-else title="还没有浏览记录" action-text="去看商品" action-to="/products" />
  </main>
</template>
