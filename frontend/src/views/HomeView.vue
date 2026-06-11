<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { gsap } from 'gsap'
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
    gsap.set('.hero__content > *, .category-strip a, .page-section', { autoAlpha: 1 })
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return

    const tl = gsap.timeline({ defaults: { ease: 'power3.out' } })
    tl.from('.hero__content > *', {
      autoAlpha: 0,
      y: 26,
      duration: 0.58,
      stagger: 0.075,
      clearProps: 'transform,opacity,visibility',
    })
      .from(
        '.category-strip a',
        {
          autoAlpha: 0,
          y: 18,
          duration: 0.36,
          stagger: 0.035,
          clearProps: 'transform,opacity,visibility',
        },
        '-=0.18',
      )
      .from(
        '.page-section',
        {
          autoAlpha: 0,
          y: 22,
          duration: 0.42,
          stagger: 0.09,
          clearProps: 'transform,opacity,visibility',
        },
        '-=0.08',
      )
  }, pageRef.value)
}

function runCardMotion() {
  if (!pageRef.value || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  cardCtx?.revert()
  cardCtx = gsap.context(() => {
    gsap.from('.product-grid .product-card', {
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
        <p class="eyebrow">综合电商买家端 MVP</p>
        <h1>校园优选商城</h1>
        <p>商品浏览、筛选、加购、下单、模拟支付、物流与评价的前端闭环演示。</p>
        <form class="search-bar" @submit.prevent="search">
          <input v-model="keyword" placeholder="搜索蓝莓、耳机、收纳盒" />
          <button class="btn primary" type="submit">搜索</button>
        </form>
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
        <h2>最新商品</h2>
        <RouterLink to="/products?sort=latest">查看全部</RouterLink>
      </div>
      <div class="product-grid">
        <ProductCard
          v-for="product in productStore.latestProducts"
          :key="product.id"
          :product="product"
        />
      </div>
    </section>

    <section class="page-section">
      <div class="section-heading">
        <h2>热卖与特卖</h2>
        <RouterLink to="/promotions/seckill">限时活动</RouterLink>
      </div>
      <div class="product-grid">
        <ProductCard
          v-for="product in productStore.specialProducts"
          :key="product.id"
          :product="product"
        />
      </div>
    </section>

    <section class="page-section">
      <div class="section-heading">
        <h2>猜你喜欢</h2>
        <RouterLink to="/member/browse-history">浏览记录</RouterLink>
      </div>
      <div class="product-grid">
        <ProductCard
          v-for="product in productStore.recommendedProducts"
          :key="product.id"
          :product="product"
        />
      </div>
    </section>
  </div>
</template>
