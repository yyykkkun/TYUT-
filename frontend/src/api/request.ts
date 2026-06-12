interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

const BASE_URL = '/api'
const MOCK_TOKEN = 'mock-jwt-token-demo'

function getToken(): string {
  return localStorage.getItem('mall-token') || ''
}

/** 当前是否为 mock 登录会话（token 是假的不发给后端） */
export function isMockSession(): boolean {
  return getToken() === MOCK_TOKEN
}

/** 后端可达性标记：只要收到过任何 HTTP 响应就认为后端可达 */
let _backendReachable = false
export function isBackendReachable(): boolean {
  return _backendReachable
}

async function doFetch<T>(method: string, path: string, body?: any, params?: Record<string, any>): Promise<T> {
  if (isMockSession()) throw new Error('Mock session - skip network')
  let url = `${BASE_URL}${path}`
  if (params) {
    const qs = Object.entries(params)
      .filter(([, v]) => v !== undefined && v !== null && v !== '')
      .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
      .join('&')
    if (qs) url += `?${qs}`
  }
  let res: Response
  try {
    res = await fetch(url, {
      method,
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${getToken()}`,
      },
      body: body ? JSON.stringify(body) : undefined,
    })
    _backendReachable = true  // 收到 HTTP 响应即确认后端可达
  } catch {
    // 网络不通，后端不可达
    throw new Error('Backend unreachable')
  }
  return handleResponse<T>(res)
}

export async function get<T>(path: string, params?: Record<string, any>): Promise<T> {
  return doFetch<T>('GET', path, undefined, params)
}

export async function post<T>(path: string, body?: any): Promise<T> {
  return doFetch<T>('POST', path, body)
}

export async function put<T>(path: string, body?: any): Promise<T> {
  return doFetch<T>('PUT', path, body)
}

export async function patch<T>(path: string, body?: any): Promise<T> {
  return doFetch<T>('PATCH', path, body)
}

export async function del<T>(path: string): Promise<T> {
  return doFetch<T>('DELETE', path)
}

async function handleResponse<T>(res: Response): Promise<T> {
  // 认证失败：跳转登录，不降级 mock
  if (res.status === 401) {
    localStorage.removeItem('mall-token')
    const currentPath = window.location.pathname
    if (currentPath !== '/login') {
      window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`
    }
    throw new Error('未登录或登录已过期，请重新登录')
  }

  const json: ApiResponse<T> = await res.json()
  if (json.code !== 0) {
    throw new Error(json.message || '请求失败')
  }
  return json.data
}
