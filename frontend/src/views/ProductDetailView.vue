<script setup lang="ts">
import { computed, ref, watchEffect, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { gsap } from 'gsap'
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
  
  // Animation setup
  const img = document.querySelector('.detail-media img')
  const cartIcon = document.getElementById('cart-icon')
  
  if (img && cartIcon && !goCheckout && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    const imgRect = img.getBoundingClientRect()
    const cartRect = cartIcon.getBoundingClientRect()
    
    const clone = img.cloneNode(true) as HTMLImageElement
    Object.assign(clone.style, {
      position: 'fixed',
      top: `${imgRect.top}px`,
      left: `${imgRect.left}px`,
      width: `${imgRect.width}px`,
      height: `${imgRect.height}px`,
      zIndex: '1000',
      pointerEvents: 'none',
      borderRadius: '8px',
      opacity: '0.8'
    })
    document.body.appendChild(clone)
    
    gsap.to(clone, {
      x: cartRect.left - imgRect.left + (cartRect.width / 2) - (imgRect.width / 2),
      y: cartRect.top - imgRect.top + (cartRect.height / 2) - (imgRect.height / 2),
      scale: 0.1,
      opacity: 0,
      duration: 0.75,
      ease: 'power2.inOut',
      onComplete: () => {
        document.body.removeChild(clone)
        gsap.fromTo(cartIcon, { scale: 1 }, { scale: 1.3, duration: 0.15, yoyo: true, repeat: 1 })
      }
    })
  }

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
      <div class="specs" style="margin: 24px 0;">
        <div style="margin-bottom: 8px; font-weight: bold; color: #595959;">规格</div>
        <a-radio-group v-model:value="selectedSpec" option-type="button" button-style="solid" size="large">
          <a-radio-button v-for="spec in product.specs" :key="spec" :value="spec">
            {{ spec }}
          </a-radio-button>
        </a-radio-group>
      </div>
      <div style="margin-bottom: 32px;">
        <div style="margin-bottom: 8px; font-weight: bold; color: #595959;">数量</div>
        <a-input-number v-model:value="quantity" :min="1" :max="Math.max(product.stock, 1)" size="large" />
      </div>
      <div class="action-row" style="display: flex; gap: 16px;">
        <a-button size="large" :disabled="product.stock <= 0" @click="addToCart(false)" style="flex: 1; height: 48px;">
          加入购物车
        </a-button>
        <a-button type="primary" size="large" :disabled="product.stock <= 0" @click="addToCart(true)" style="flex: 1; height: 48px;">
          立即购买
        </a-button>
        <a-button type="dashed" size="large" @click="shareProduct" style="height: 48px;">分享</a-button>
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
