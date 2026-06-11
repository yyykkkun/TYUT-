<script setup lang="ts">
import { onMounted } from 'vue'
import { useMessageStore } from '@/stores/messages'

const messageStore = useMessageStore()

onMounted(() => {
  messageStore.loadMessages()
})
</script>

<template>
  <main class="stack-page">
    <div class="page-title">
      <h1>站内信</h1>
      <p>降价通知、活动通知和订单消息集中展示。</p>
    </div>
    <div class="line-list">
      <article
        v-for="message in messageStore.messages"
        :key="message.id"
        class="message-card"
        :class="{ unread: !message.read }"
      >
        <div>
          <h2>{{ message.title }}</h2>
          <p>{{ message.body }}</p>
          <small>{{ message.createdAt }}</small>
        </div>
        <button class="btn" type="button" @click="messageStore.markRead(message.id)">
          标为已读
        </button>
      </article>
    </div>
  </main>
</template>
