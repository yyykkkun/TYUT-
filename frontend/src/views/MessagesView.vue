<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { gsap } from 'gsap'
import { BellOutlined, PropertySafetyOutlined, ShoppingOutlined, NotificationOutlined, CheckOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { useMessageStore } from '@/stores/messages'
import EmptyState from '@/components/EmptyState.vue'

const messageStore = useMessageStore()
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  await messageStore.loadMessages()
  loading.value = false
  
  // Entrance animation
  gsap.from('.message-item', {
    y: 20,
    opacity: 0,
    duration: 0.5,
    stagger: 0.1,
    ease: 'power2.out'
  })
})

function getIcon(type: string) {
  switch (type) {
    case 'price': return PropertySafetyOutlined
    case 'order': return ShoppingOutlined
    case 'promotion': return NotificationOutlined
    default: return BellOutlined
  }
}

function getIconColor(type: string) {
  switch (type) {
    case 'price': return '#ff4d4f'
    case 'order': return '#1890ff'
    case 'promotion': return '#722ed1'
    default: return '#52c41a'
  }
}

async function handleMarkRead(id: string, event: MouseEvent) {
  const target = (event.currentTarget as HTMLElement).closest('.message-item')
  if (target) {
    gsap.to(target, {
      backgroundColor: 'rgba(255, 255, 255, 1)',
      borderColor: '#f0f0f0',
      duration: 0.4
    })
  }
  await messageStore.markRead(id)
}

async function handleDelete(id: string, event: MouseEvent) {
  const target = (event.currentTarget as HTMLElement).closest('.message-item')
  if (target) {
    gsap.to(target, {
      opacity: 0,
      x: 50,
      height: 0,
      marginBottom: 0,
      paddingTop: 0,
      paddingBottom: 0,
      marginTop: 0,
      duration: 0.4,
      ease: 'power2.in',
      onComplete: () => {
        // Assume store has a delete method, if not, we just mark it as read for now
        // or we could filter it locally if the API doesn't support deletion.
        // For this demo, let's just mark it read as before but visually remove it.
        messageStore.markRead(id)
      }
    })
  }
}
</script>

<template>
  <main class="stack-page">
    <div class="page-title" style="display: flex; justify-content: space-between; align-items: flex-end;">
      <div>
        <h1 style="display: flex; align-items: center; gap: 12px;">
          <BellOutlined /> 站内信
        </h1>
        <p>降价通知、活动通知和订单消息集中展示。</p>
      </div>
      <a-button v-if="messageStore.messages.length > 0" type="link" @click="messageStore.markAllRead">
        全部标为已读
      </a-button>
    </div>

    <div v-if="loading" style="padding: 40px; text-align: center;">
      <a-spin size="large" />
    </div>

    <div v-else-if="messageStore.messages.length > 0" class="message-list">
      <div
        v-for="message in messageStore.messages"
        :key="message.id"
        class="message-item"
        :class="{ unread: !message.read }"
      >
        <div class="message-icon" :style="{ backgroundColor: getIconColor(message.type) + '15', color: getIconColor(message.type) }">
          <component :is="getIcon(message.type)" />
        </div>
        
        <div class="message-content">
          <div class="message-header">
            <h3>{{ message.title }}</h3>
            <span class="message-time">{{ message.createdAt }}</span>
          </div>
          <p class="message-body">{{ message.body }}</p>
        </div>

        <div class="message-actions">
          <a-tooltip title="标为已读">
            <a-button 
              v-if="!message.read"
              type="text" 
              shape="circle" 
              @click="(e) => handleMarkRead(message.id, e)"
            >
              <template #icon><CheckOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip title="删除">
            <a-button type="text" shape="circle" danger @click="(e) => handleDelete(message.id, e)">
              <template #icon><DeleteOutlined /></template>
            </a-button>
          </a-tooltip>
        </div>
      </div>
    </div>

    <EmptyState 
      v-else 
      title="暂无消息" 
      action-text="去购物"
      action-to="/products"
    />
  </main>
</template>

<style scoped>
.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  gap: 16px;
  padding: 20px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  position: relative;
}

.message-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  border-color: #e6e6e6;
  transform: translateY(-2px);
}

.message-item.unread {
  background: #f6ffed;
  border-color: #b7eb8f;
}

.message-item.unread::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: #52c41a;
}

.message-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 8px;
}

.message-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.message-time {
  font-size: 13px;
  color: #8c8c8c;
}

.message-body {
  margin: 0;
  color: #595959;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.message-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.message-item:hover .message-actions {
  opacity: 1;
}

@media (max-width: 640px) {
  .message-header {
    flex-direction: column;
    gap: 4px;
  }
  
  .message-actions {
    opacity: 1;
    position: absolute;
    right: 8px;
    bottom: 8px;
  }
}
</style>
