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
  await productStore.loadCategories()
  await productStore.patchFilters({
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

// 仅同步筛选条件到 URL
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
    const cards = gsap.utils.toArray('.catalog-content .product-card')
    if (!cards.length) return
    gsap.from(cards, {
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
      <div class="filter-panel__head">
        <p class="eyebrow">筛选</p>
        <h2>找到合适的闲置</h2>
      </div>
      <a-form layout="vertical">
        <a-form-item label="关键词">
          <a-input
            v-model:value="productStore.filters.keyword"
            placeholder="搜索闲置"
            allow-clear
            @change="productStore.filters.page = 1"
          />
        </a-form-item>
        <a-form-item label="分类">
          <a-select v-model:value="productStore.filters.category" @change="productStore.filters.page = 1">
            <a-select-option value="">全部分类</a-select-option>
            <a-select-option
              v-for="category in productStore.categories"
              :key="category.id"
              :value="category.id"
            >
              {{ category.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="卖家类型">
          <a-select v-model:value="productStore.filters.brand" @change="productStore.filters.page = 1">
            <a-select-option value="">全部类型</a-select-option>
            <a-select-option v-for="brand in productStore.brands" :key="brand" :value="brand">
              {{ brand }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="productStore.filters.stock" @change="productStore.filters.page = 1">
            <a-select-option value="">全部状态</a-select-option>
            <a-select-option value="in">可交易</a-select-option>
            <a-select-option value="out">已转出</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="校区">
          <a-select v-model:value="productStore.filters.city" @change="productStore.filters.page = 1">
            <a-select-option value="">全部城市</a-select-option>
            <a-select-option v-for="city in productStore.cities" :key="city" :value="city">
              {{ city }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="价格区间">
          <div class="price-range">
            <a-input-number
              v-model:value="productStore.filters.minPrice"
              :min="0"
              placeholder="最低价"
              @change="productStore.filters.page = 1"
            />
            <span></span>
            <a-input-number
              v-model:value="productStore.filters.maxPrice"
              :min="0"
              placeholder="最高价"
              @change="productStore.filters.page = 1"
            />
          </div>
        </a-form-item>
      </a-form>
    </aside>

    <section class="catalog-content">
      <div class="toolbar catalog-toolbar">
        <div>
          <p class="eyebrow">市场</p>
          <h1>闲置市场</h1>
          <p>共 {{ productStore.filteredProducts.length }} 件闲置，按你的筛选实时更新</p>
        </div>
        <a-select v-model:value="productStore.filters.sort" class="sort-select">
          <a-select-option value="composite">综合排序</a-select-option>
          <a-select-option value="priceAsc">价格从低到高</a-select-option>
          <a-select-option value="priceDesc">价格从高到低</a-select-option>
          <a-select-option value="sales">成交优先</a-select-option>
          <a-select-option value="popularity">人气优先</a-select-option>
          <a-select-option value="latest">最新上架</a-select-option>
        </a-select>
      </div>

      <a-spin v-if="productStore.loading" class="center-spin" size="large" />

      <div v-else-if="productStore.pagedProducts.length" class="product-grid">
        <ProductCard
          v-for="product in productStore.pagedProducts"
          :key="product.id"
          :product="product"
        />
      </div>
      <EmptyState
        v-else
        title="没有找到符合条件的闲置"
        action-text="清空筛选"
        action-to="/products"
      />

      <div class="pagination">
        <a-pagination
          v-model:current="productStore.filters.page"
          :total="productStore.filteredProducts.length"
          :page-size="12"
          show-less-items
        />
      </div>
    </section>
  </main>
</template>
