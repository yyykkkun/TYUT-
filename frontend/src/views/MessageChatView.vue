<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeftOutlined,
  SendOutlined,
  ShoppingOutlined,
} from '@ant-design/icons-vue'
import EmptyState from '@/components/EmptyState.vue'
import { useMessageStore } from '@/stores/messages'

const route = useRoute()
const router = useRouter()
const messageStore = useMessageStore()
const loading = ref(false)
const draft = ref('')
const chatBodyRef = ref<HTMLElement | null>(null)

const conversationId = computed(() => String(route.params.id || ''))

onMounted(loadConversation)

onUnmounted(() => {
  messageStore.disconnectChat()
})

watch(conversationId, loadConversation)

watch(
  () => messageStore.chatMessages.length,
  () => scrollToBottom(),
)

async function loadConversation() {
  if (!conversationId.value) return
  loading.value = true
  await messageStore.loadConversations()
  if (!messageStore.conversations.some((item) => item.id === conversationId.value)) {
    messageStore.activeConversationId = ''
    messageStore.chatMessages = []
    loading.value = false
    return
  }
  await messageStore.loadChat(conversationId.value)
  messageStore.connectChat()
  loading.value = false
  scrollToBottom()
}

async function send() {
  if (!draft.value.trim()) return
  await messageStore.sendChat(draft.value)
  draft.value = ''
  scrollToBottom()
}

async function scrollToBottom() {
  await nextTick()
  if (chatBodyRef.value) {
    chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
  }
}
</script>

<template>
  <main class="chat-page">
    <a-button class="back-button" @click="router.push('/messages')">
      <template #icon><ArrowLeftOutlined /></template>
      返回会话列表
    </a-button>

    <div v-if="loading" class="loading-panel">
      <a-spin size="large" />
    </div>

    <EmptyState
      v-else-if="!messageStore.activeConversation"
      title="没有找到这个会话"
      action-text="返回会话列表"
      action-to="/messages"
    />

    <section v-else class="chat-shell">
      <header class="chat-header">
        <img
          :src="messageStore.activeConversation.productImage"
          :alt="messageStore.activeConversation.productTitle"
        />
        <div>
          <p class="eyebrow">与 {{ messageStore.activeConversation.sellerName }} 沟通</p>
          <h1>{{ messageStore.activeConversation.productTitle }}</h1>
          <p>
            {{ messageStore.socketConnected ? '实时连接中' : '本地演示模式' }}
          </p>
        </div>
        <RouterLink :to="`/products/${messageStore.activeConversation.productId}`">
          <a-button>
            <template #icon><ShoppingOutlined /></template>
            查看闲置
          </a-button>
        </RouterLink>
      </header>

      <div ref="chatBodyRef" class="chat-body">
        <div
          v-for="message in messageStore.chatMessages"
          :key="message.id"
          class="chat-message"
          :class="{ self: message.self }"
        >
          <a-avatar :size="34">{{ message.senderName.charAt(0) }}</a-avatar>
          <div class="bubble">
            <div class="bubble-meta">
              <span>{{ message.senderName }}</span>
              <time>{{ message.createdAt }}</time>
            </div>
            <p>{{ message.content }}</p>
          </div>
        </div>
      </div>

      <footer class="chat-compose">
        <a-textarea
          v-model:value="draft"
          :rows="2"
          placeholder="输入想询问的成色、价格、交易地点..."
          @keydown.enter.exact.prevent="send"
        />
        <a-button type="primary" :disabled="!draft.trim()" @click="send">
          <template #icon><SendOutlined /></template>
          发送
        </a-button>
      </footer>
      <p v-if="messageStore.socketError" class="connection-note">
        {{ messageStore.socketError }}
      </p>
    </section>
  </main>
</template>

<style scoped>
.chat-page {
  width: min(980px, calc(100% - 32px));
  margin: 28px auto;
}

.back-button {
  margin-bottom: 14px;
}

.loading-panel {
  padding: 48px;
  text-align: center;
}

.chat-shell {
  overflow: hidden;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
}

.chat-header {
  display: grid;
  grid-template-columns: 78px minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  padding: 18px;
  border-bottom: 1px solid var(--line);
}

.chat-header img {
  width: 78px;
  height: 78px;
  object-fit: cover;
  border-radius: var(--radius-md);
}

.chat-header h1 {
  margin: 0 0 5px;
  font-size: 24px;
}

.chat-header p:last-child {
  margin: 0;
  color: var(--ink-muted);
}

.chat-body {
  min-height: 440px;
  max-height: min(58vh, 620px);
  padding: 18px;
  overflow: auto;
  background: var(--surface-soft);
}

.chat-message {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.chat-message.self {
  flex-direction: row-reverse;
}

.bubble {
  max-width: min(620px, 78%);
  padding: 10px 12px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
}

.chat-message.self .bubble {
  color: #fff;
  background: var(--primary);
  border-color: var(--primary);
}

.bubble-meta {
  display: flex;
  gap: 10px;
  align-items: baseline;
  margin-bottom: 4px;
  font-size: 12px;
  opacity: 0.78;
}

.bubble p {
  margin: 0;
  line-height: 1.65;
}

.chat-compose {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  padding: 14px;
  border-top: 1px solid var(--line);
}

.connection-note {
  margin: 0;
  padding: 0 14px 14px;
  color: var(--ink-muted);
  font-size: 13px;
}

@media (max-width: 640px) {
  .chat-page {
    width: min(100% - 24px, 980px);
    margin: 18px auto;
  }

  .chat-header {
    grid-template-columns: 56px minmax(0, 1fr);
  }

  .chat-header img {
    width: 56px;
    height: 56px;
  }

  .chat-header a {
    grid-column: 1 / -1;
  }

  .chat-header .ant-btn,
  .chat-compose .ant-btn {
    width: 100%;
  }

  .chat-body {
    min-height: 380px;
  }

  .chat-compose {
    grid-template-columns: 1fr;
  }

  .bubble {
    max-width: 84%;
  }
}
</style>
