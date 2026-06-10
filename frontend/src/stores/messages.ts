import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchMessages, markMessageRead } from '@/api/messages'
import type { Message } from '@/types/domain'

export const useMessageStore = defineStore('messages', () => {
  const messages = ref<Message[]>([])
  const unreadCount = computed(() => messages.value.filter((item) => !item.read).length)

  async function loadMessages() {
    try {
      messages.value = await fetchMessages()
    } catch {
      messages.value = []
    }
  }

  async function markRead(id: string) {
    await markMessageRead(id)
    const msg = messages.value.find((item) => item.id === id)
    if (msg) msg.read = true
  }

  return { messages, unreadCount, loadMessages, markRead }
})
