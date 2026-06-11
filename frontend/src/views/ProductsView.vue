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
  <main ref="pageRef" class="catalog-page" style="display: flex; gap: 24px;">
    <aside class="filter-panel" style="width: 280px; flex-shrink: 0; background: #fff; padding: 24px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); align-self: flex-start; position: sticky; top: 24px;">
      <h2 style="margin-bottom: 24px; font-size: 1.25rem;">商品筛选</h2>
      <a-form layout="vertical">
        <a-form-item label="关键词">
          <a-input
            v-model:value="productStore.filters.keyword"
            placeholder="搜索商品"
            allow-clear
            @change="productStore.filters.page = 1"
          />
        </a-form-item>
        <a-form-item label="分类">
          <a-select v-model:value="productStore.filters.category" style="width: 100%" @change="productStore.filters.page = 1">
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
        <a-form-item label="品牌">
          <a-select v-model:value="productStore.filters.brand" style="width: 100%" @change="productStore.filters.page = 1">
            <a-select-option value="">全部品牌</a-select-option>
            <a-select-option v-for="brand in productStore.brands" :key="brand" :value="brand">
              {{ brand }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="库存">
          <a-select v-model:value="productStore.filters.stock" style="width: 100%" @change="productStore.filters.page = 1">
            <a-select-option value="">全部库存</a-select-option>
            <a-select-option value="in">仅看有货</a-select-option>
            <a-select-option value="out">暂时缺货</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="城市">
          <a-select v-model:value="productStore.filters.city" style="width: 100%" @change="productStore.filters.page = 1">
            <a-select-option value="">全部城市</a-select-option>
            <a-select-option v-for="city in productStore.cities" :key="city" :value="city">
              {{ city }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="价格区间">
          <div style="display: flex; align-items: center; gap: 8px;">
            <a-input-number
              v-model:value="productStore.filters.minPrice"
              :min="0"
              style="width: 100%"
              placeholder="最低价"
              @change="productStore.filters.page = 1"
            />
            <span>-</span>
            <a-input-number
              v-model:value="productStore.filters.maxPrice"
              :min="0"
              style="width: 100%"
              placeholder="最高价"
              @change="productStore.filters.page = 1"
            />
          </div>
        </a-form-item>
      </a-form>
    </aside>

    <section class="catalog-content" style="flex: 1; min-width: 0;">
      <div class="toolbar" style="display: flex; justify-content: space-between; align-items: center; background: #fff; padding: 16px 24px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); margin-bottom: 24px;">
        <div>
          <h1 style="margin: 0; font-size: 1.5rem; line-height: 1.2;">商品列表</h1>
          <p style="margin: 4px 0 0; color: #8c8c8c;">共 {{ productStore.filteredProducts.length }} 件商品</p>
        </div>
        <a-select v-model:value="productStore.filters.sort" style="width: 160px;">
          <a-select-option value="composite">综合排序</a-select-option>
          <a-select-option value="priceAsc">价格从低到高</a-select-option>
          <a-select-option value="priceDesc">价格从高到低</a-select-option>
          <a-select-option value="sales">销量优先</a-select-option>
          <a-select-option value="popularity">人气优先</a-select-option>
          <a-select-option value="latest">最新上架</a-select-option>
        </a-select>
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

      <div class="pagination" style="margin-top: 32px; display: flex; justify-content: center;">
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

<style scoped>
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 24px;
}
</style>
