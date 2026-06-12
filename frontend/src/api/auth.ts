import { get, post } from '@/api/request'
import { saveProfile } from '@/api/member'

export interface LoginParams {
  account: string
  password: string
}

export interface RegisterParams {
  username: string
  password: string
  phone?: string
  nickname?: string
}

export interface LoginResult {
  token: string
  user: { id: string; name: string; account: string; level: string }
}

let useMock = false

import { isBackendReachable } from '@/api/request'

function shouldSkipMock(e: unknown): boolean {
  if (!isMockSession()) return true
  if (e instanceof Error && e.message === 'Backend unreachable') return false
  if (e instanceof Error && e.message.includes('未登录')) return true
  return false
}

const mockUser = {
  id: '1',
  name: '演示会员',
  account: 'demo@mall.test',
  level: '黄金会员',
  role: 'admin',
}

const mockToken = 'mock-jwt-token-demo'

export async function login(data: LoginParams): Promise<LoginResult> {
  if (useMock) return { token: mockToken, user: mockUser }
  try {
    return await post<LoginResult>('/auth/login', data)
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    console.warn('后端不可用，降级为本地 mock 数据')
    // 演示账号直接放行
    if (data.account === 'demo@mall.test' || data.account === 'test') {
      return { token: mockToken, user: mockUser }
    }
    throw new Error('Mock 模式：仅支持 demo@mall.test 或 test 账号')
  }
}

export async function register(data: RegisterParams): Promise<LoginResult> {
  if (useMock) {
    // 新用户初始化干净 profile（余额0，积分0）
    saveProfile({ balance: 0, points: 0, giftCard: 0, growth: 0, level: '普通会员', couponCount: 0, unreadCount: 0 })
    return {
      token: mockToken,
      user: {
        id: '2',
        name: data.nickname || data.username,
        account: data.username,
        level: '普通会员',
        role: 'user',
      },
    }
  }
  try {
    return await post<LoginResult>('/auth/register', data)
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    saveProfile({ balance: 0, points: 0, giftCard: 0, growth: 0, level: '普通会员', couponCount: 0, unreadCount: 0 })
    return {
      token: mockToken,
      user: {
        id: '2',
        name: data.nickname || data.username,
        account: data.username,
        level: '普通会员',
        role: 'user',
      },
    }
  }
}

export async function anonymousLogin(phone: string): Promise<LoginResult> {
  if (useMock) {
    // 匿名新用户初始化干净 profile
    saveProfile({ balance: 0, points: 0, giftCard: 0, growth: 0, level: '普通会员', couponCount: 0, unreadCount: 0 })
    return {
      token: mockToken,
      user: {
        id: '3',
        name: `匿名用户${phone.slice(-4)}`,
        account: `u_${phone}`,
        level: '普通会员',
        role: 'user',
      },
    }
  }
  try {
    return await post<LoginResult>('/auth/login/anonymous', { phone })
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    saveProfile({ balance: 0, points: 0, giftCard: 0, growth: 0, level: '普通会员', couponCount: 0, unreadCount: 0 })
    return {
      token: mockToken,
      user: {
        id: '3',
        name: `匿名用户${phone.slice(-4)}`,
        account: `u_${phone}`,
        level: '普通会员',
        role: 'user',
      },
    }
  }
}

export async function getMe(): Promise<LoginResult['user']> {
  if (useMock) return mockUser
  try {
    return await get<LoginResult['user']>('/auth/me')
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    return mockUser
  }
}
