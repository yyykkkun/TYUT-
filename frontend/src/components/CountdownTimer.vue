<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'

const remaining = ref(2 * 60 * 60 + 26 * 60)
const timer = window.setInterval(() => {
  remaining.value = Math.max(0, remaining.value - 1)
}, 1000)

onBeforeUnmount(() => window.clearInterval(timer))

const text = computed(() => {
  const hours = String(Math.floor(remaining.value / 3600)).padStart(2, '0')
  const minutes = String(Math.floor((remaining.value % 3600) / 60)).padStart(2, '0')
  const seconds = String(remaining.value % 60).padStart(2, '0')
  return `${hours}:${minutes}:${seconds}`
})
</script>

<template>
  <span class="countdown">{{ text }}</span>
</template>
