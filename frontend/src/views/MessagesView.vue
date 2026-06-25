<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  BellOutlined,
  SearchOutlined,
  ShoppingOutlined,
} from '@ant-design/icons-vue'
import EmptyState from '@/components/EmptyState.vue'
import { useMessageStore } from '@/stores/messages'

const route = useRoute()
const router = useRouter()
const messageStore = useMessageStore()
const loading = ref(false)
const keyword = ref('')

const filteredConversations = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  if (!key) return messageStore.conversations
  return messageStore.conversations.filter((item) =>
    [item.sellerName, item.productTitle, item.lastMessage]
      .some((text) => text.toLowerCase().includes(key)),
  )
})

onMounted(async () => {
  loading.value = true
  await Promise.all([messageStore.loadMessages(), messageStore.loadConversations()])
  loading.value = false

  const legacyConversation = String(route.query.conversation || '')
  if (legacyConversation) router.replace(`/messages/${legacyConversation}`)
})
</script>

<template>
  <main class="messages-page">
    <section class="messages-hero">
      <RouterLink to="/messages/notifications">
        <a-button>
          <template #icon><BellOutlined /></template>
          通知
          <a-badge :count="messageStore.unreadCount" />
        </a-button>
      </RouterLink>
    </section>

    <section class="messages-toolbar">
      <a-input
        v-model:value="keyword"
        allow-clear
        size="large"
        placeholder="搜索卖家、闲置或最近消息"
      >
        <template #prefix><SearchOutlined /></template>
      </a-input>
      <span>{{ filteredConversations.length }} 个会话</span>
    </section>

    <div v-if="loading" class="loading-panel">
      <a-spin size="large" />
    </div>

    <EmptyState
      v-else-if="!filteredConversations.length"
      title="暂无匹配会话"
      action-text="去看看闲置"
      action-to="/products"
    />

    <section v-else class="conversation-list-page" aria-label="会话列表">
      <RouterLink
        v-for="conversation in filteredConversations"
        :key="conversation.id"
        class="conversation-row"
        :to="`/messages/${conversation.id}`"
      >
        <img :src="conversation.productImage" :alt="conversation.productTitle" />
        <span class="conversation-row__main">
          <span class="conversation-row__top">
            <strong>{{ conversation.sellerName }}</strong>
            <time>{{ conversation.updatedAt }}</time>
          </span>
          <span class="conversation-row__product">
            <ShoppingOutlined />
            {{ conversation.productTitle }}
          </span>
          <span class="conversation-row__message">{{ conversation.lastMessage }}</span>
        </span>
        <a-badge :count="conversation.unread" />
      </RouterLink>
    </section>
  </main>
</template>

<style scoped>
.messages-page {
  width: min(920px, calc(100% - 32px));
  margin: 32px auto;
}

.messages-hero,
.messages-toolbar,
.conversation-list-page {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
}

.messages-hero {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
  padding: 14px;
}

.messages-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  margin: 16px 0;
  padding: 14px;
  color: var(--ink-muted);
  font-weight: 800;
}

.loading-panel {
  padding: 48px;
  text-align: center;
}

.conversation-list-page {
  overflow: hidden;
}

.conversation-row {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid var(--line);
  transition:
    background-color 0.16s ease,
    transform 0.16s var(--ease-out);
}

.conversation-row:last-child {
  border-bottom: 0;
}

.conversation-row:hover {
  background: var(--primary-soft);
  transform: translateY(-1px);
}

.conversation-row img {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: var(--radius-md);
}

.conversation-row__main,
.conversation-row__top,
.conversation-row__product {
  display: flex;
  min-width: 0;
}

.conversation-row__main {
  flex-direction: column;
  gap: 5px;
}

.conversation-row__top {
  justify-content: space-between;
  gap: 12px;
}

.conversation-row__top time {
  color: var(--ink-subtle);
  font-size: 12px;
  white-space: nowrap;
}

.conversation-row__product,
.conversation-row__message {
  overflow: hidden;
  color: var(--ink-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-row__product {
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 800;
}

@media (max-width: 640px) {
  .messages-page {
    width: min(100% - 24px, 920px);
    margin: 18px auto;
  }

  .messages-hero,
  .messages-toolbar {
    align-items: stretch;
    grid-template-columns: 1fr;
  }

  .messages-hero {
    flex-direction: column;
  }

  .conversation-row {
    grid-template-columns: 58px minmax(0, 1fr) auto;
    padding: 14px;
  }

  .conversation-row img {
    width: 58px;
    height: 58px;
  }

  .conversation-row__top {
    flex-direction: column;
    gap: 2px;
  }
}
</style>
