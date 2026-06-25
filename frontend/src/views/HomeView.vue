<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { gsap } from 'gsap'
import EmptyState from '@/components/EmptyState.vue'
import ProductCard from '@/components/ProductCard.vue'
import { useProductStore } from '@/stores/products'

const router = useRouter()
const productStore = useProductStore()
const keyword = ref('')
const pageRef = ref<HTMLElement | null>(null)
let pageCtx: gsap.Context | undefined
let cardCtx: gsap.Context | undefined

onMounted(async () => {
  productStore.loadCategories()
  productStore.loadHomeData()
  await nextTick()
  runPageMotion()
  runCardMotion()
})

onUnmounted(() => {
  pageCtx?.revert()
  cardCtx?.revert()
})

watch(
  () => [
    productStore.latestProducts.length,
    productStore.specialProducts.length,
    productStore.recommendedProducts.length,
  ],
  async () => {
    await nextTick()
    runCardMotion()
  },
)

function runPageMotion() {
  if (!pageRef.value) return
  pageCtx?.revert()
  pageCtx = gsap.context(() => {
    const heroItems = gsap.utils.toArray('.hero__content > *')
    const categoryItems = gsap.utils.toArray('.category-strip a')
    const sections = gsap.utils.toArray('.page-section')
    gsap.set([...heroItems, ...categoryItems, ...sections], { autoAlpha: 1 })
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return

    const tl = gsap.timeline({ defaults: { ease: 'power3.out' } })
    tl.from(heroItems, {
      autoAlpha: 0,
      y: 26,
      duration: 0.58,
      stagger: 0.075,
      clearProps: 'transform,opacity,visibility',
    })
    if (categoryItems.length) {
      tl.from(
        categoryItems,
        {
          autoAlpha: 0,
          y: 18,
          duration: 0.36,
          stagger: 0.035,
          clearProps: 'transform,opacity,visibility',
        },
        '-=0.18',
      )
    }
    if (sections.length) {
      tl.from(
        sections,
        {
          autoAlpha: 0,
          y: 22,
          duration: 0.42,
          stagger: 0.09,
          clearProps: 'transform,opacity,visibility',
        },
        '-=0.08',
      )
    }
  }, pageRef.value)
}

function runCardMotion() {
  if (!pageRef.value || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  cardCtx?.revert()
  cardCtx = gsap.context(() => {
    const cards = gsap.utils.toArray('.product-grid .product-card')
    if (!cards.length) return
    gsap.from(cards, {
      autoAlpha: 0,
      y: 18,
      duration: 0.34,
      stagger: 0.035,
      ease: 'power3.out',
      clearProps: 'transform,opacity,visibility',
    })
  }, pageRef.value)
}

function search() {
  productStore.patchFilters({ keyword: keyword.value })
  router.push({ path: '/products', query: keyword.value ? { keyword: keyword.value } : {} })
}
</script>

<template>
  <div ref="pageRef" class="home-page">
    <section class="hero">
      <div class="hero__content">
        <a-input-search
          v-model:value="keyword"
          class="hero-search"
          placeholder="搜索耳机、教材、收纳架"
          enter-button="搜索"
          size="large"
          @search="search"
        />
      </div>
    </section>

    <section class="category-strip">
      <RouterLink
        v-for="category in productStore.categories"
        :key="category.id"
        :to="`/products?category=${category.id}`"
      >
        {{ category.name }}
      </RouterLink>
    </section>

    <section class="page-section">
      <div class="section-heading">
        <h2>最新闲置</h2>
        <RouterLink to="/products?sort=latest">查看全部</RouterLink>
      </div>
      <div v-if="productStore.latestProducts.length" class="product-grid">
        <ProductCard
          v-for="product in productStore.latestProducts"
          :key="product.id"
          :product="product"
        />
      </div>
      <EmptyState v-else title="暂时没有最新闲置" action-text="去市场看看" action-to="/products" />
    </section>

    <section class="page-section">
      <div class="section-heading">
        <h2>同校好物</h2>
        <RouterLink to="/products?sort=popularity">查看市场</RouterLink>
      </div>
      <div v-if="productStore.specialProducts.length" class="product-grid">
        <ProductCard
          v-for="product in productStore.specialProducts"
          :key="product.id"
          :product="product"
        />
      </div>
      <EmptyState v-else title="同校好物正在整理中" action-text="查看市场" action-to="/products" />
    </section>

    <section class="page-section">
      <div class="section-heading">
        <h2>可能感兴趣</h2>
        <RouterLink to="/member/browse-history">浏览记录</RouterLink>
      </div>
      <div v-if="productStore.recommendedProducts.length" class="product-grid">
        <ProductCard
          v-for="product in productStore.recommendedProducts"
          :key="product.id"
          :product="product"
        />
      </div>
      <EmptyState v-else title="还没有推荐内容" action-text="浏览闲置" action-to="/products" />
    </section>
  </div>
</template>
