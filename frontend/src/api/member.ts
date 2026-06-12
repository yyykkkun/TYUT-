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
const PROFILE_KEY = 'mall-mock-profile'
const BALANCE_RECORDS_KEY = 'mall-mock-balance-records'

/** 后端可达时禁止一切 mock */
function forceReal(): boolean {
  return !isMockSession()
}

// ===== Mock 余额流水 =====

export interface BalanceRecord {
  id: string; userId: string; amount: number; type: string
  orderNo?: string; balanceAfter: number; remark: string; createdAt: string
}

export function addBalanceRecord(rec: Omit<BalanceRecord, 'id' | 'createdAt'>) {
  try {
    const raw = localStorage.getItem(BALANCE_RECORDS_KEY)
    const list: BalanceRecord[] = raw ? JSON.parse(raw) : []
    list.unshift({
      ...rec,
      id: 'br' + Date.now(),
      createdAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
    })
    localStorage.setItem(BALANCE_RECORDS_KEY, JSON.stringify(list))
  } catch { /* ignore */ }
}

export function getBalanceRecords(): BalanceRecord[] {
  try {
    const raw = localStorage.getItem(BALANCE_RECORDS_KEY)
    return raw ? JSON.parse(raw) : []
  } catch { return [] }
}

// ===== Mock 积分流水 =====
const POINTS_RECORDS_KEY = 'mall-mock-points-records'

export interface PointsRecord {
  id: string; userId: string; amount: number; type: string
  orderNo?: string; pointsAfter: number; remark: string; createdAt: string
}

export function addPointsRecord(rec: Omit<PointsRecord, 'id' | 'createdAt'>) {
  try {
    const raw = localStorage.getItem(POINTS_RECORDS_KEY)
    const list: PointsRecord[] = raw ? JSON.parse(raw) : []
    list.unshift({ ...rec, id: 'pr' + Date.now(), createdAt: new Date().toISOString().replace('T', ' ').slice(0, 19) })
    localStorage.setItem(POINTS_RECORDS_KEY, JSON.stringify(list))
  } catch { /* ignore */ }
}

export function getPointsRecords(): PointsRecord[] {
  try { const raw = localStorage.getItem(POINTS_RECORDS_KEY); return raw ? JSON.parse(raw) : [] }
  catch { return [] }
}

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

// ===== Profile（持久化可变字段：积分、余额、礼品卡） =====

const defaultProfile: MemberProfile = {
  balance: 688,
  points: 2680,
  giftCard: 120,
  growth: 7420,
  level: '黄金会员',
  couponCount: 3,
  unreadCount: 2,
}

/** 读取持久化的 profile，首次用默认值初始化 */
export function loadProfile(): MemberProfile {
  try {
    const raw = localStorage.getItem(PROFILE_KEY)
    if (raw) return JSON.parse(raw)
    // 首次初始化
    const init = { ...defaultProfile }
    localStorage.setItem(PROFILE_KEY, JSON.stringify(init))
    return init
  } catch {
    return { ...defaultProfile }
  }
}

/** 保存 profile 到 localStorage */
export function saveProfile(p: MemberProfile) {
  localStorage.setItem(PROFILE_KEY, JSON.stringify(p))
}

/** 增量扣减积分（供下单/兑换等场景使用），返回扣减后的 profile */
export function deductPoints(amount: number): MemberProfile {
  const p = loadProfile()
  p.points = Math.max(0, p.points - amount)
  saveProfile(p)
  return p
}

/** 增量扣减礼品卡余额 */
export function deductGiftCard(amount: number): MemberProfile {
  const p = loadProfile()
  p.giftCard = Math.max(0, p.giftCard - amount)
  saveProfile(p)
  return p
}

import { isBackendReachable } from '@/api/request'

function shouldSkipMock(e: unknown): boolean {
  if (!isMockSession()) return true  // 真实后端：永不降级 mock
  if (e instanceof Error && e.message === 'Backend unreachable') return false
  if (e instanceof Error && e.message.includes('未登录')) return true
  return false
}

export async function fetchProfile(): Promise<MemberProfile> {
  if (useMock) return loadProfile()
  try {
    return await get<MemberProfile>('/member/profile')
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    console.warn('后端不可用，降级为本地 mock 数据')
    return loadProfile()
  }
}

// ===== Addresses =====

export async function fetchAddresses(): Promise<Address[]> {
  if (useMock) return loadAddresses()
  try {
    return await get<Address[]>('/member/addresses')
  } catch (e) {
    if (shouldSkipMock(e)) throw e
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
      addrs.forEach((a) => (a.isDefault = false))
    }
    addrs.push(addr)
    saveAddresses(addrs)
    return
  }
  try {
    return await post<void>('/member/addresses', data)
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    return addAddress(data)
  }
}

export async function setDefaultAddress(id: string): Promise<void> {
  if (useMock) {
    const addrs = loadAddresses()
    addrs.forEach((a) => (a.isDefault = a.id === id))
    saveAddresses(addrs)
    return
  }
  try {
    return await put<void>(`/member/addresses/${id}/default`)
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    return setDefaultAddress(id)
  }
}

export async function removeAddress(id: string): Promise<void> {
  if (useMock) {
    const addrs = loadAddresses().filter((a) => a.id !== id)
    // 如果删了默认地址，第一个自动变默认
    const firstAddress = addrs[0]
    if (firstAddress && !addrs.find((a) => a.isDefault)) {
      firstAddress.isDefault = true
    }
    saveAddresses(addrs)
    return
  }
  try {
    return await del<void>(`/member/addresses/${id}`)
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    return removeAddress(id)
  }
}

// ===== Coupons =====

export async function fetchCoupons(): Promise<Coupon[]> {
  if (useMock) return mockCoupons
  try {
    return await get<Coupon[]>('/member/coupons')
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    return mockCoupons
  }
}

export async function exchangeCoupon(points: number): Promise<void> {
  if (useMock) {
    const p = loadProfile()
    if (p.points < points) {
      throw new Error('积分不足')
    }
    deductPoints(points)
    addPointsRecord({ userId: '1', amount: -points, type: 'exchange', pointsAfter: p.points - points, remark: '积分兑换优惠券' })
    return
  }
  try {
    return await post<void>('/member/coupons/exchange', { points })
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    const p = loadProfile()
    if (p.points >= points) {
      deductPoints(points)
      addPointsRecord({ userId: '1', amount: -points, type: 'exchange', pointsAfter: p.points - points, remark: '积分兑换优惠券' })
    }
  }
}

export async function fetchBrowseHistory(): Promise<string[]> {
  if (useMock) return ['p1001', 'p1002']
  try {
    return await get<string[]>('/member/browse-history')
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    return ['p1001', 'p1002']
  }
}

export async function addBrowseHistory(productId: string): Promise<void> {
  if (useMock) return
  try {
    return await post<void>('/member/browse-history', { productId: Number(productId) })
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
  }
}

export async function recharge(amount: number): Promise<MemberProfile> {
  if (useMock) {
    const p = loadProfile()
    p.balance += amount
    saveProfile(p)
    addBalanceRecord({ userId: '1', amount, type: 'recharge', balanceAfter: p.balance, remark: '余额充值' })
    return p
  }
  try {
    return await post<MemberProfile>('/member/recharge', { amount })
  } catch (e) {
    if (shouldSkipMock(e)) throw e
    useMock = true
    const p = loadProfile()
    p.balance += amount
    saveProfile(p)
    addBalanceRecord({ userId: '1', amount, type: 'recharge', balanceAfter: p.balance, remark: '余额充值' })
    return p
  }
}
