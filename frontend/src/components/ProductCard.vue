<script setup lang="ts">
import { ref } from 'vue'
import type { Product } from '@/types/domain'
import { useCartStore } from '@/stores/cart'

const props = defineProps<{
  product: Product
}>()

const cart = useCartStore()
const adding = ref(false)

async function quickAdd(e: Event) {
  e.preventDefault()
  e.stopPropagation()
  if (!props.product.specs.length || props.product.stock <= 0) return
  adding.value = true
  await cart.addItem(props.product.id, props.product.specs[0], 1)
  adding.value = false
}
</script>

<template>
  <RouterLink class="product-card" :to="`/products/${product.id}`">
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
  </RouterLink>
</template>
