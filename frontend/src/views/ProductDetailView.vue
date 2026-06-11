<script setup lang="ts">
import { computed, ref, watchEffect, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ProductCard from '@/components/ProductCard.vue'
import { useCartStore } from '@/stores/cart'
import { useProductStore } from '@/stores/products'
import type { Product } from '@/types/domain'

const route = useRoute()
const router = useRouter()
const productStore = useProductStore()
const cart = useCartStore()
const quantity = ref(1)
const selectedSpec = ref('')
const detailProduct = ref<Product | undefined>(undefined)

const product = computed(
  () => detailProduct.value || productStore.getProduct(String(route.params.id)),
)

async function loadProduct() {
  const id = String(route.params.id)
  const p = await productStore.fetchDetail(id)
  if (p) detailProduct.value = p
}

onMounted(loadProduct)
watch(() => route.params.id, loadProduct)
const related = computed(() =>
  productStore.products
    .filter((item) => item.category === product.value?.category && item.id !== product.value.id)
    .slice(0, 4),
)

watchEffect(() => {
  if (product.value) {
    productStore.viewProduct(product.value.id)
    selectedSpec.value = selectedSpec.value || product.value.specs[0] || ''
  }
})

async function addToCart(goCheckout = false) {
  if (!product.value || !selectedSpec.value || product.value.stock <= 0) return
  await cart.addItem(product.value.id, selectedSpec.value, quantity.value)
  if (goCheckout) router.push('/cart')
}

function shareProduct() {
  if (!product.value) return
  if ('share' in navigator) {
    navigator
      .share({ title: product.value.title, text: product.value.subtitle })
      .catch(() => undefined)
  } else {
    window.alert('已复制分享标题：' + product.value.title)
  }
}
</script>

<template>
  <main v-if="product" class="detail-page">
    <section class="detail-media">
      <img :src="product.image" :alt="product.title" />
    </section>
    <section class="detail-info">
      <p class="eyebrow">{{ product.brand }} · {{ product.city }}</p>
      <h1>{{ product.title }}</h1>
      <p>{{ product.subtitle }}</p>
      <div class="price-line">
        <strong>￥{{ product.price }}</strong>
        <span v-if="product.originalPrice">￥{{ product.originalPrice }}</span>
      </div>
      <div class="facts">
        <span>库存 {{ product.stock }}</span>
        <span>销量 {{ product.sales }}</span>
        <span>评分 {{ product.rating }}</span>
      </div>
      <div class="specs">
        <button
          v-for="spec in product.specs"
          :key="spec"
          class="chip"
          :class="{ active: selectedSpec === spec }"
          type="button"
          @click="selectedSpec = spec"
        >
          {{ spec }}
        </button>
      </div>
      <label class="quantity-field">
        数量
        <input v-model.number="quantity" type="number" min="1" :max="Math.max(product.stock, 1)" />
      </label>
      <div class="action-row">
        <button class="btn" type="button" :disabled="product.stock <= 0" @click="addToCart(false)">
          加入购物车
        </button>
        <button
          class="btn primary"
          type="button"
          :disabled="product.stock <= 0"
          @click="addToCart(true)"
        >
          立即购买
        </button>
        <button class="btn subtle" type="button" @click="shareProduct">分享</button>
      </div>
    </section>
  </main>

  <section class="page-section">
    <div class="section-heading">
      <h2>浏览该商品的人还浏览了</h2>
    </div>
    <div class="product-grid">
      <ProductCard v-for="item in related" :key="item.id" :product="item" />
    </div>
  </section>
</template>
