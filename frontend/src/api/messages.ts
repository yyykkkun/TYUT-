import { get, put } from '@/api/request'
import type { Message } from '@/types/domain'
import { messages as mockMessages } from '@/data/mock'

let useMock = false
const MSG_READ_KEY = 'mall-mock-messages-read'
const MSG_STORE_KEY = 'mall-mock-messages'

/** 保存 mock 消息列表到 localStorage */
function loadMockMessages(): Message[] {
  try {
    const raw = localStorage.getItem(MSG_STORE_KEY)
    return raw ? JSON.parse(raw) : mockMessages
  } catch {
    return [...mockMessages]
  }
}

function saveMockMessages(msgs: Message[]) {
  localStorage.setItem(MSG_STORE_KEY, JSON.stringify(msgs))
}

/** 给 mock 模式创建一条消息通知 */
export function addMockMessage(title: string, body: string, type: string = 'order') {
  if (useMock) {
    const msgs = loadMockMessages()
    msgs.unshift({
      id: `m${Date.now()}`,
      title,
      body,
      type,
      read: false,
      createdAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
    })
    saveMockMessages(msgs)
  }
}

import { isBackendReachable } from '@/api/request'

function shouldSkipMock(e: unknown): boolean {
  if (!isMockSession()) return true
  if (e instanceof Error && e.message === 'Backend unreachable') return false
  if (e instanceof Error && e.message.includes('未登录')) return true
  return false
}

// ===== localStorage 已读状态持久化 =====

function loadReadIds(): Set<string> {
  try {
    const raw = localStorage.getItem(MSG_READ_KEY)
    return raw ? new Set(JSON.parse(raw)) : new Set()
  } catch {
    return new Set()
  }
}

function saveReadId(id: string) {
  const ids = loadReadIds()
  ids.add(id)
  localStorage.setItem(MSG_READ_KEY, JSON.stringify([...ids]))
}

function saveAllRead() {
  const ids = new Set(mockMessages.map((m) => m.id))
  localStorage.setItem(MSG_READ_KEY, JSON.stringify([...ids]))
}

/** 返回合并了已读状态的消息列表 */
function getMessagesWithReadState(): Message[] {
  const readIds = loadReadIds()
  const msgs = useMock ? loadMockMessages() : mockMessages
  return msgs.map((m) => ({
    ...m,
    read: readIds.has(m.id) ? true : m.read,
  }))
}

// ===== API 函数 =====

export async function fetchMessages(): Promise<Message[]> {
  if (useMock) return getMessagesWithReadState()
  try {
    return await get<Message[]>('/messages')
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    console.warn('后端不可用，降级为本地 mock 数据')
    return getMessagesWithReadState()
  }
}

export async function markMessageRead(id: string): Promise<void> {
  if (useMock) {
    saveReadId(id)
    return
  }
  try {
    return await put<void>(`/messages/${id}/read`)
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    saveReadId(id)
  }
}

export async function markAllMessagesRead(): Promise<void> {
  if (useMock) {
    saveAllRead()
    return
  }
  try {
    // 后端暂无批量已读接口，逐条标记
    for (const m of mockMessages) {
      if (!m.read) await put<void>(`/messages/${m.id}/read`).catch(() => {})
    }
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    saveAllRead()
  }
}
