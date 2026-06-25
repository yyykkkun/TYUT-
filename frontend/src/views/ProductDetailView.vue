<script setup lang="ts">
import { computed, ref, watchEffect, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { gsap } from 'gsap'
import {
  HeartFilled,
  HeartOutlined,
  MessageOutlined,
  ShareAltOutlined,
  ShoppingCartOutlined,
} from '@ant-design/icons-vue'
import EmptyState from '@/components/EmptyState.vue'
import ProductCard from '@/components/ProductCard.vue'
import { useCartStore } from '@/stores/cart'
import { useMessageStore } from '@/stores/messages'
import { useProductStore } from '@/stores/products'
import { fetchProductReviews, type ProductReview } from '@/api/products'
import { get, post, del } from '@/api/request'
import type { Product } from '@/types/domain'

const route = useRoute()
const router = useRouter()
const productStore = useProductStore()
const cart = useCartStore()
const messageStore = useMessageStore()
const quantity = ref(1)
const selectedSpec = ref('')
const detailProduct = ref<Product | undefined>(undefined)
const reviews = ref<ProductReview[]>([])
const reviewsLoading = ref(false)
const favorited = ref(false)
const favLoading = ref(false)
const startingChat = ref(false)

// SKU 列表和联动
const skus = ref<any[]>([])
const currentSkuPrice = ref(0)

const product = computed(
  () => detailProduct.value || productStore.getProduct(String(route.params.id)),
)

async function loadProduct() {
  const id = String(route.params.id)
  const p = await productStore.fetchDetail(id)
  if (p) detailProduct.value = p
  // 加载评价、收藏状态、SKU
  reviewsLoading.value = true
  const [revs, favStatus, skuList] = await Promise.all([
    fetchProductReviews(id),
    get<{favorited: boolean}>(`/member/favorites/${id}/status`).catch(() => ({favorited: false})),
    get<any[]>(`/products/${id}/skus`).catch(() => []),
  ])
  reviews.value = revs
  favorited.value = favStatus.favorited
  skus.value = skuList
  if (skuList.length > 0) {
    const first = skuList[0]
    selectedSpec.value = first.spec
    currentSkuPrice.value = first.price || p?.price || 0
  }
  reviewsLoading.value = false
}

async function toggleFavorite() {
  favLoading.value = true
  try {
    if (favorited.value) {
      await del(`/member/favorites/${String(route.params.id)}`)
    } else {
      await post(`/member/favorites/${String(route.params.id)}`)
    }
    favorited.value = !favorited.value
  } finally { favLoading.value = false }
}

function onSpecChange(spec: string) {
  selectedSpec.value = spec
  const sku = skus.value.find((s: any) => s.spec === spec)
  currentSkuPrice.value = sku?.price || detailProduct.value?.price || 0
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
      opacity: '0.8',
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
      },
    })
  }

  await cart.addItem(product.value.id, selectedSpec.value, quantity.value)
  if (goCheckout) router.push('/cart')
}

async function startChat() {
  if (!product.value || startingChat.value) return
  startingChat.value = true
  try {
    const conversation = await messageStore.openProductConversation(product.value)
    router.push(`/messages/${conversation.id}`)
  } finally {
    startingChat.value = false
  }
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
  <div class="product-detail-view">
    <template v-if="product">
      <main class="detail-page">
        <section class="detail-media">
          <img :src="product.image" :alt="product.title" />
        </section>
        <section class="detail-info">
          <p class="eyebrow">{{ product.sellerName || product.brand }} · {{ product.city }}</p>
          <h1>{{ product.title }}</h1>
          <p>{{ product.subtitle }}</p>
          <div class="price-line">
            <strong>￥{{ skus.length ? currentSkuPrice.toFixed(2) : product.price }}</strong>
            <span v-if="product.originalPrice">￥{{ product.originalPrice }}</span>
          </div>
          <div class="facts">
            <span>{{ product.stock > 0 ? '可交易' : '已转出' }}</span>
            <span>{{ product.condition || '成色待沟通' }}</span>
            <span>卖家评分 {{ product.sellerRating || product.rating }}</span>
          </div>
          <div class="detail-field">
            <div class="detail-field__label">规格</div>
            <a-radio-group v-model:value="selectedSpec" option-type="button" button-style="solid" size="large" @change="(e: any) => onSpecChange(e.target.value)">
              <a-radio-button v-for="spec in product.specs" :key="spec" :value="spec">
                {{ spec }}
              </a-radio-button>
            </a-radio-group>
          </div>
          <div class="detail-field">
            <div class="detail-field__label">数量</div>
            <a-input-number v-model:value="quantity" :min="1" :max="Math.max(product.stock, 1)" size="large" />
          </div>
          <div class="action-row detail-actions">
            <a-button size="large" :disabled="product.stock <= 0" @click="addToCart(false)">
              <template #icon><ShoppingCartOutlined /></template>
              加入购物车
            </a-button>
            <a-button type="primary" size="large" :disabled="product.stock <= 0" @click="addToCart(true)">
              立即购买
            </a-button>
            <a-button size="large" :loading="startingChat" @click="startChat">
              <template #icon><MessageOutlined /></template>
              联系卖家
            </a-button>
            <a-button size="large" @click="shareProduct">
              <template #icon><ShareAltOutlined /></template>
              分享
            </a-button>
            <a-button class="favorite-button" size="large" :loading="favLoading" @click="toggleFavorite" :aria-label="favorited ? '取消收藏' : '收藏'">
              <template #icon>
                <HeartFilled v-if="favorited" />
                <HeartOutlined v-else />
              </template>
            </a-button>
          </div>
        </section>
      </main>

      <!-- 商品评价 -->
      <section class="page-section">
        <div class="section-heading">
          <h2>交易评价 ({{ reviews.length }})</h2>
        </div>

        <a-spin v-if="reviewsLoading" class="center-spin" />

        <a-collapse v-else-if="reviews.length" class="review-list" :bordered="false">
          <a-collapse-panel v-for="r in reviews" :key="r.id">
            <template #header>
              <div class="review-head">
                <a-avatar :src="r.userAvatar" :size="36">
                  {{ r.userName.charAt(0) }}
                </a-avatar>
                <div>
                  <strong>{{ r.userName }}</strong>
                  <span>{{ r.createdAt?.slice(0, 10) }}</span>
                </div>
                <span class="review-rating">{{ r.rating || 5 }}.0 分</span>
              </div>
            </template>
            <p class="review-content">{{ r.content }}</p>
            <div v-if="r.images.length" class="review-images">
              <img v-for="(img, i) in r.images" :key="i" :src="img" :alt="`评价图片 ${i + 1}`" />
            </div>
          </a-collapse-panel>
        </a-collapse>

        <div v-else class="muted-empty">
          暂无评价，完成交易后可以评价卖家
        </div>
      </section>

      <section class="page-section">
        <div class="section-heading">
          <h2>看过这件闲置的人还看了</h2>
        </div>
        <div class="product-grid">
          <ProductCard v-for="item in related" :key="item.id" :product="item" />
        </div>
      </section>
    </template>

    <EmptyState v-else title="商品不存在或已下架" action-text="返回首页" action-to="/" />
  </div>
</template>
