<script setup lang="ts">
defineProps<{
  title: string
  actionText?: string
  actionTo?: string
}>()
</script>

<template>
  <section class="empty-state">
    <div class="empty-state__mark">0</div>
    <h2>{{ title }}</h2>
    <RouterLink v-if="actionText && actionTo" class="btn primary" :to="actionTo">{{ actionText }}</RouterLink>
  </section>
</template>
