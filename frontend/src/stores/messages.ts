import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  fetchChatMessages,
  fetchConversations,
  fetchMessages,
  markAllMessagesRead,
  markMessageRead,
  openConversationForProduct,
  sendChatMessage,
} from '@/api/messages'
import type { ChatMessage, Conversation, Message, Product } from '@/types/domain'

export const useMessageStore = defineStore('messages', () => {
  const messages = ref<Message[]>([])
  const conversations = ref<Conversation[]>([])
  const chatMessages = ref<ChatMessage[]>([])
  const activeConversationId = ref('')
  const socketConnected = ref(false)
  const socketError = ref('')
  let socket: WebSocket | null = null

  const unreadCount = computed(() => messages.value.filter((item) => !item.read).length)
  const chatUnreadCount = computed(() =>
    conversations.value.reduce((total, item) => total + item.unread, 0),
  )
  const activeConversation = computed(() =>
    conversations.value.find((item) => item.id === activeConversationId.value),
  )

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

  async function markAllRead() {
    await markAllMessagesRead()
    messages.value.forEach((item) => {
      item.read = true
    })
  }

  async function loadConversations() {
    try {
      conversations.value = await fetchConversations()
    } catch {
      conversations.value = []
    }
  }

  async function loadChat(conversationId: string) {
    activeConversationId.value = conversationId
    chatMessages.value = await fetchChatMessages(conversationId).catch(() => [])
    const conversation = conversations.value.find((item) => item.id === conversationId)
    if (conversation) conversation.unread = 0
  }

  async function openProductConversation(product: Product) {
    const conversation = await openConversationForProduct(product)
    const idx = conversations.value.findIndex((item) => item.id === conversation.id)
    if (idx >= 0) {
      conversations.value[idx] = conversation
    } else {
      conversations.value.unshift(conversation)
    }
    activeConversationId.value = conversation.id
    return conversation
  }

  function connectChat() {
    if (socket || !activeConversationId.value) return
    const token = localStorage.getItem('mall-token') || ''
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    socketError.value = ''
    try {
      socket = new WebSocket(`${protocol}//${window.location.host}/ws/chat?token=${encodeURIComponent(token)}`)
      socket.onopen = () => {
        socketConnected.value = true
        socketError.value = ''
      }
      socket.onmessage = (event) => {
        const payload = JSON.parse(event.data) as ChatMessage & { type?: string; message?: string }
        if (payload.type === 'error') {
          socketError.value = payload.message || '实时消息发送失败'
          return
        }
        if (payload.conversationId === activeConversationId.value) {
          const exists = chatMessages.value.some((item) => item.id === payload.id)
          if (!exists) chatMessages.value.push(payload)
        }
        loadConversations()
      }
      socket.onerror = () => {
        socketError.value = '实时连接不可用，将使用普通发送'
      }
      socket.onclose = () => {
        socketConnected.value = false
        socket = null
      }
    } catch {
      socketError.value = '实时连接不可用，已使用本地消息演示'
      socket = null
    }
  }

  function disconnectChat() {
    socket?.close()
    socket = null
    socketConnected.value = false
  }

  async function sendChat(content: string) {
    const text = content.trim()
    if (!text || !activeConversationId.value) return
    if (socket?.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({
        conversationId: activeConversationId.value,
        content: text,
      }))
      return
    }

    const message = await sendChatMessage(activeConversationId.value, text)
    chatMessages.value.push(message)
    await loadConversations()
    activeConversationId.value = message.conversationId
  }

  return {
    messages,
    conversations,
    chatMessages,
    activeConversationId,
    activeConversation,
    unreadCount,
    chatUnreadCount,
    socketConnected,
    socketError,
    loadMessages,
    markRead,
    markAllRead,
    loadConversations,
    loadChat,
    openProductConversation,
    connectChat,
    disconnectChat,
    sendChat,
  }
})
