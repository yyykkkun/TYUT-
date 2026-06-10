import { get, put } from '@/api/request'
import type { Message } from '@/types/domain'
import { messages as mockMessages } from '@/data/mock'

let useMock = false

export async function fetchMessages(): Promise<Message[]> {
  if (useMock) return mockMessages
  try {
    return await get<Message[]>('/messages')
  } catch {
    useMock = true
    console.warn('后端不可用，降级为本地 mock 数据')
    return mockMessages
  }
}

export async function markMessageRead(id: string): Promise<void> {
  if (useMock) {
    const msg = mockMessages.find(m => m.id === id)
    if (msg) msg.read = true
    return
  }
  try {
    return await put<void>(`/messages/${id}/read`)
  } catch {
    useMock = true
  }
}
