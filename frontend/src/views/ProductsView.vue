<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { gsap } from 'gsap'
import EmptyState from '@/components/EmptyState.vue'
import ProductCard from '@/components/ProductCard.vue'
import { useProductStore, type ProductSort } from '@/stores/products'

const route = useRoute()
const router = useRouter()
const productStore = useProductStore()
const pageRef = ref<HTMLElement | null>(null)
let introCtx: gsap.Context | undefined
let gridCtx: gsap.Context | undefined

onMounted(async () => {
  productStore.loadCategories()
  productStore.patchFilters({
    keyword: String(route.query.keyword || ''),
    category: String(route.query.category || ''),
    sort: (route.query.sort as ProductSort) || 'composite',
  })
  await nextTick()
  runIntroMotion()
  runGridMotion()
})

onUnmounted(() => {
  introCtx?.revert()
  gridCtx?.revert()
})

// 仅同步筛选条件到 URL（避免 deep watch 触发不必要的 router.replace）
watch(
  () => ({ ...productStore.filters }),
  (filters) => {
    router.replace({
      query: Object.fromEntries(
        Object.entries(filters).filter(
          ([, value]) => value !== '' && value !== null && value !== 1,
        ),
      ),
    })
  },
)

watch(
  () => [
    productStore.pagedProducts.map((product) => product.id).join(','),
    productStore.filters.page,
    productStore.filters.sort,
  ],
  async () => {
    await nextTick()
    runGridMotion()
  },
)

function runIntroMotion() {
  if (!pageRef.value) return
  introCtx?.revert()
  introCtx = gsap.context(() => {
    gsap.set('.filter-panel, .toolbar', { autoAlpha: 1 })
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return

    gsap.from('.filter-panel, .toolbar', {
      autoAlpha: 0,
      y: 18,
      duration: 0.36,
      stagger: 0.06,
      ease: 'power3.out',
      clearProps: 'transform,opacity,visibility',
    })
  }, pageRef.value)
}

function runGridMotion() {
  if (!pageRef.value || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  gridCtx?.revert()
  gridCtx = gsap.context(() => {
    gsap.from('.catalog-content .product-card', {
      autoAlpha: 0,
      y: 16,
      scale: 0.985,
      duration: 0.32,
      stagger: 0.025,
      ease: 'power3.out',
      clearProps: 'transform,opacity,visibility',
    })
  }, pageRef.value)
}
</script>

<template>
  <main ref="pageRef" class="catalog-page">
    <aside class="filter-panel">
      <h2>商品筛选</h2>
      <label>
        关键词
        <input
          v-model="productStore.filters.keyword"
          placeholder="搜索商品"
          @input="productStore.filters.page = 1"
        />
      </label>
      <label>
        分类
        <select v-model="productStore.filters.category" @change="productStore.filters.page = 1">
          <option value="">全部分类</option>
          <option
            v-for="category in productStore.categories"
            :key="category.id"
            :value="category.id"
          >
            {{ category.name }}
          </option>
        </select>
      </label>
      <label>
        品牌
        <select v-model="productStore.filters.brand" @change="productStore.filters.page = 1">
          <option value="">全部品牌</option>
          <option v-for="brand in productStore.brands" :key="brand" :value="brand">
            {{ brand }}
          </option>
        </select>
      </label>
      <label>
        库存
        <select v-model="productStore.filters.stock" @change="productStore.filters.page = 1">
          <option value="">全部库存</option>
          <option value="in">仅看有货</option>
          <option value="out">暂时缺货</option>
        </select>
      </label>
      <label>
        城市
        <select v-model="productStore.filters.city" @change="productStore.filters.page = 1">
          <option value="">全部城市</option>
          <option v-for="city in productStore.cities" :key="city" :value="city">{{ city }}</option>
        </select>
      </label>
      <div class="split-inputs">
        <label>
          最低价
          <input
            v-model.number="productStore.filters.minPrice"
            type="number"
            min="0"
            @input="productStore.filters.page = 1"
          />
        </label>
        <label>
          最高价
          <input
            v-model.number="productStore.filters.maxPrice"
            type="number"
            min="0"
            @input="productStore.filters.page = 1"
          />
        </label>
      </div>
    </aside>

    <section class="catalog-content">
      <div class="toolbar">
        <div>
          <h1>商品列表</h1>
          <p>共 {{ productStore.filteredProducts.length }} 件商品</p>
        </div>
        <select v-model="productStore.filters.sort">
          <option value="composite">综合排序</option>
          <option value="priceAsc">价格从低到高</option>
          <option value="priceDesc">价格从高到低</option>
          <option value="sales">销量优先</option>
          <option value="popularity">人气优先</option>
          <option value="latest">最新上架</option>
        </select>
      </div>

      <div v-if="productStore.pagedProducts.length" class="product-grid">
        <ProductCard
          v-for="product in productStore.pagedProducts"
          :key="product.id"
          :product="product"
        />
      </div>
      <EmptyState
        v-else
        title="没有找到符合条件的商品"
        action-text="清空筛选"
        action-to="/products"
      />

      <div class="pagination">
        <button
          class="btn"
          :disabled="productStore.filters.page <= 1"
          @click="productStore.filters.page--"
        >
          上一页
        </button>
        <span>{{ productStore.filters.page }} / {{ productStore.totalPages }}</span>
        <button
          class="btn"
          :disabled="productStore.filters.page >= productStore.totalPages"
          @click="productStore.filters.page++"
        >
          下一页
        </button>
      </div>
    </section>
  </main>
</template>
