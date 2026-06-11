<script setup lang="ts">
import { InboxOutlined } from '@ant-design/icons-vue'

defineProps<{
  title: string
  actionText?: string
  actionTo?: string
}>()
</script>

<template>
  <a-empty :description="title" style="padding: 48px 0;">
    <template #image>
      <InboxOutlined style="font-size: 64px; color: #d9d9d9;" />
    </template>
    <template v-if="actionText && actionTo">
      <a-button type="primary" size="large" @click="$router.push(actionTo)">
        {{ actionText }}
      </a-button>
    </template>
  </a-empty>
</template>
