<script setup lang="ts">
import { onUnmounted, ref } from 'vue'
import { gsap } from 'gsap'
import type { Product } from '@/types/domain'
import { useCartStore } from '@/stores/cart'

const props = defineProps<{
  product: Product
}>()

const cart = useCartStore()
const adding = ref(false)
const cardRef = ref<HTMLElement | null>(null)

onUnmounted(() => {
  if (!cardRef.value) return
  gsap.killTweensOf([cardRef.value, cardRef.value.querySelector('img')])
})

async function quickAdd(e: Event) {
  e.preventDefault()
  e.stopPropagation()
  const button = e.currentTarget as HTMLElement
  const firstSpec = props.product.specs[0]
  if (!firstSpec || props.product.stock <= 0) return
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
          <span>{{ product.brand }}</span>
          <span>{{ product.city }}</span>
        </div>
        <h3>{{ product.title }}</h3>
        <p>{{ product.subtitle }}</p>
        <div class="tag-row">
          <span v-for="tag in product.tags.slice(0, 3)" :key="tag" class="tag">{{ tag }}</span>
        </div>
        <div class="product-card__footer">
          <strong>￥{{ product.price }}</strong>
          <span>已售 {{ product.sales }}</span>
          <button
            class="btn small"
            type="button"
            :disabled="product.stock <= 0 || adding"
            @click="quickAdd"
          >
            {{ product.stock <= 0 ? '缺货' : adding ? '...' : '加入购物车' }}
          </button>
        </div>
      </div>
    </a>
  </RouterLink>
</template>
