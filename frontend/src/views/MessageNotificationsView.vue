<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  ArrowLeftOutlined,
  CheckOutlined,
  NotificationOutlined,
} from '@ant-design/icons-vue'
import EmptyState from '@/components/EmptyState.vue'
import { useMessageStore } from '@/stores/messages'

const messageStore = useMessageStore()
const loading = ref(false)
const activeType = ref<'all' | 'chat' | 'order' | 'price' | 'system'>('all')

const visibleMessages = computed(() => {
  if (activeType.value === 'all') return messageStore.messages
  return messageStore.messages.filter((item) => item.type === activeType.value)
})

onMounted(async () => {
  loading.value = true
  await messageStore.loadMessages()
  loading.value = false
})
</script>

<template>
  <main class="notifications-page">
    <section class="notifications-hero">
      <div>
        <p class="eyebrow">通知</p>
        <h1><NotificationOutlined /> 消息通知</h1>
      </div>
      <div class="notification-actions">
        <RouterLink to="/messages">
          <a-button>
            <template #icon><ArrowLeftOutlined /></template>
            返回会话
          </a-button>
        </RouterLink>
        <a-button type="primary" :disabled="!messageStore.unreadCount" @click="messageStore.markAllRead()">
          全部已读
        </a-button>
      </div>
    </section>

    <a-tabs v-model:activeKey="activeType" class="notification-tabs">
      <a-tab-pane key="all" tab="全部" />
      <a-tab-pane key="chat" tab="沟通" />
      <a-tab-pane key="order" tab="订单" />
      <a-tab-pane key="price" tab="降价" />
      <a-tab-pane key="system" tab="系统" />
    </a-tabs>

    <div v-if="loading" class="loading-panel">
      <a-spin size="large" />
    </div>

    <EmptyState
      v-else-if="!visibleMessages.length"
      title="暂无通知"
      action-text="返回会话"
      action-to="/messages"
    />

    <section v-else class="notification-list">
      <article
        v-for="message in visibleMessages"
        :key="message.id"
        class="notification-item"
        :class="{ unread: !message.read }"
      >
        <div>
          <span class="notification-type">{{ message.type }}</span>
          <h2>{{ message.title }}</h2>
          <p>{{ message.body }}</p>
          <time>{{ message.createdAt }}</time>
        </div>
        <a-tooltip v-if="!message.read" title="标为已读">
          <a-button shape="circle" @click="messageStore.markRead(message.id)">
            <template #icon><CheckOutlined /></template>
          </a-button>
        </a-tooltip>
      </article>
    </section>
  </main>
</template>

<style scoped>
.notifications-page {
  width: min(920px, calc(100% - 32px));
  margin: 32px auto;
}

.notifications-hero,
.notification-list {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
}

.notifications-hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 22px;
}

.notifications-hero h1 {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 6px;
}

.notifications-hero p:last-child {
  margin: 0;
  color: var(--ink-muted);
}

.notification-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.notification-tabs {
  margin: 16px 0;
}

.loading-panel {
  padding: 48px;
  text-align: center;
}

.notification-list {
  overflow: hidden;
}

.notification-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  padding: 18px;
  border-bottom: 1px solid var(--line);
}

.notification-item:last-child {
  border-bottom: 0;
}

.notification-item.unread {
  background: var(--success-soft);
}

.notification-type {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  color: var(--primary-hover);
  background: var(--primary-soft);
  border: 1px solid oklch(0.8 0.075 170);
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
}

.notification-item h2 {
  margin: 10px 0 6px;
  font-size: 18px;
}

.notification-item p {
  margin: 0 0 10px;
  color: var(--ink-muted);
  line-height: 1.65;
}

.notification-item time {
  color: var(--ink-subtle);
  font-size: 12px;
}

@media (max-width: 640px) {
  .notifications-page {
    width: min(100% - 24px, 920px);
    margin: 18px auto;
  }

  .notifications-hero {
    flex-direction: column;
  }

  .notification-actions,
  .notification-actions a,
  .notification-actions .ant-btn {
    width: 100%;
  }

  .notification-item {
    grid-template-columns: 1fr;
  }
}
</style>
