import { get, post, put, del } from '@/api/request'
import type { Address, Coupon } from '@/types/domain'
import { addresses as mockAddresses, coupons as mockCoupons } from '@/data/mock'

export interface MemberProfile {
  balance: number
  points: number
  giftCard: number
  growth: number
  level: string
  couponCount: number
  unreadCount: number
}

let useMock = false
const ADDR_KEY = 'mall-mock-addresses'

// ===== localStorage 地址持久化 =====

function loadAddresses(): Address[] {
  try {
    const raw = localStorage.getItem(ADDR_KEY)
    const addrs: Address[] = raw ? JSON.parse(raw) : []
    // 首次使用用 mock 默认地址初始化
    if (!addrs.length) {
      const init = JSON.parse(JSON.stringify(mockAddresses))
      localStorage.setItem(ADDR_KEY, JSON.stringify(init))
      return init
    }
    return addrs
  } catch {
    return JSON.parse(JSON.stringify(mockAddresses))
  }
}

function saveAddresses(addrs: Address[]) {
  localStorage.setItem(ADDR_KEY, JSON.stringify(addrs))
}

// ===== Profile =====

const mockProfile: MemberProfile = {
  balance: 688,
  points: 2680,
  giftCard: 120,
  growth: 7420,
  level: '黄金会员',
  couponCount: 3,
  unreadCount: 2,
}

export async function fetchProfile(): Promise<MemberProfile> {
  if (useMock) return mockProfile
  try {
    return await get<MemberProfile>('/member/profile')
  } catch {
    useMock = true
    console.warn('后端不可用，降级为本地 mock 数据')
    return mockProfile
  }
}

// ===== Addresses =====

export async function fetchAddresses(): Promise<Address[]> {
  if (useMock) return loadAddresses()
  try {
    return await get<Address[]>('/member/addresses')
  } catch {
    useMock = true
    return loadAddresses()
  }
}

export async function addAddress(data: Omit<Address, 'id'>): Promise<void> {
  if (useMock) {
    const addrs = loadAddresses()
    const addr: Address = {
      id: `addr${Date.now()}`,
      ...data,
    }
    // 设为默认时取消其他默认
    if (addr.isDefault) {
      addrs.forEach(a => (a.isDefault = false))
    }
    addrs.push(addr)
    saveAddresses(addrs)
    return
  }
  try {
    return await post<void>('/member/addresses', data)
  } catch {
    useMock = true
    return addAddress(data)
  }
}

export async function setDefaultAddress(id: string): Promise<void> {
  if (useMock) {
    const addrs = loadAddresses()
    addrs.forEach(a => (a.isDefault = a.id === id))
    saveAddresses(addrs)
    return
  }
  try {
    return await put<void>(`/member/addresses/${id}/default`)
  } catch {
    useMock = true
    return setDefaultAddress(id)
  }
}

export async function removeAddress(id: string): Promise<void> {
  if (useMock) {
    const addrs = loadAddresses().filter(a => a.id !== id)
    // 如果删了默认地址，第一个自动变默认
    if (addrs.length && !addrs.find(a => a.isDefault)) {
      addrs[0].isDefault = true
    }
    saveAddresses(addrs)
    return
  }
  try {
    return await del<void>(`/member/addresses/${id}`)
  } catch {
    useMock = true
    return removeAddress(id)
  }
}

// ===== Coupons =====

export async function fetchCoupons(): Promise<Coupon[]> {
  if (useMock) return mockCoupons
  try {
    return await get<Coupon[]>('/member/coupons')
  } catch {
    useMock = true
    return mockCoupons
  }
}

export async function exchangeCoupon(points: number): Promise<void> {
  if (useMock) return
  try {
    return await post<void>('/member/coupons/exchange', { points })
  } catch {
    useMock = true
  }
}

export async function fetchBrowseHistory(): Promise<string[]> {
  if (useMock) return ['p1001', 'p1002']
  try {
    return await get<string[]>('/member/browse-history')
  } catch {
    useMock = true
    return ['p1001', 'p1002']
  }
}

export async function addBrowseHistory(productId: string): Promise<void> {
  if (useMock) return
  try {
    return await post<void>('/member/browse-history', { productId: Number(productId) })
  } catch {
    useMock = true
  }
}
