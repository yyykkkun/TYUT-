<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { gsap } from 'gsap'
import type { Product } from '@/types/domain'
import { useCartStore } from '@/stores/cart'

const props = defineProps<{
  product: Product
}>()

const cart = useCartStore()
const adding = ref(false)
const cardRef = ref<HTMLElement | null>(null)
const safeTags = computed(() => Array.isArray(props.product.tags) ? props.product.tags : [])
const safeSpecs = computed(() => Array.isArray(props.product.specs) ? props.product.specs : [])

onUnmounted(() => {
  if (!cardRef.value) return
  gsap.killTweensOf([cardRef.value, cardRef.value.querySelector('img')])
})

async function quickAdd(e: Event) {
  e.preventDefault()
  e.stopPropagation()
  const button = e.currentTarget as HTMLElement
  const firstSpec = safeSpecs.value[0]
  if (!firstSpec || props.product.stock <= 0) return
  
  // Animation setup
  const card = cardRef.value
  const img = card?.querySelector('img')
  const cartIcon = document.getElementById('cart-icon')
  
  if (img && cartIcon && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
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

  adding.value = true
  await cart.addItem(props.product.id, firstSpec, 1)
  adding.value = false

  if (!window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    gsap.fromTo(button, { scale: 0.96 }, { scale: 1, duration: 0.24, ease: 'power3.out' })
  }
}

function enterCard(e: MouseEvent) {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  const card = e.currentTarget as HTMLElement
  const image = card.querySelector('img')
  gsap.to(card, { y: -4, duration: 0.22, ease: 'power3.out', overwrite: 'auto' })
  if (image) gsap.to(image, { scale: 1.035, duration: 0.42, ease: 'power3.out', overwrite: 'auto' })
}

function leaveCard(e: MouseEvent) {
  const card = e.currentTarget as HTMLElement
  const image = card.querySelector('img')
  gsap.to(card, { y: 0, duration: 0.2, ease: 'power3.out', overwrite: 'auto' })
  if (image) gsap.to(image, { scale: 1, duration: 0.32, ease: 'power3.out', overwrite: 'auto' })
}
</script>

<template>
  <RouterLink v-slot="{ href, navigate }" custom :to="`/products/${product.id}`">
    <a
      ref="cardRef"
      class="product-card"
      :href="href"
      @click="navigate"
      @mouseenter="enterCard"
      @mouseleave="leaveCard"
    >
      <img :src="product.image" :alt="product.title" />
      <div class="product-card__body">
        <div class="product-card__meta">
          <span>{{ product.sellerName || product.brand }}</span>
          <span>{{ product.city }}</span>
        </div>
        <h3>{{ product.title }}</h3>
        <p>{{ product.subtitle }}</p>
        <div class="tag-row product-card__tags">
          <span v-for="tag in safeTags.slice(0, 3)" :key="tag" class="tag">{{ tag }}</span>
        </div>
        <div class="product-card__footer">
          <div class="product-card__price">
            <strong>￥{{ product.price }}</strong>
            <span>
              {{ product.condition || '闲置' }} · 卖家 {{ product.sellerRating || product.rating }} 分
            </span>
          </div>
          <a-button
            class="product-card__action"
            type="primary"
            :disabled="product.stock <= 0"
            :loading="adding"
            @click="quickAdd"
          >
            {{ product.stock <= 0 ? '已转出' : '加购' }}
          </a-button>
        </div>
      </div>
    </a>
  </RouterLink>
</template>
