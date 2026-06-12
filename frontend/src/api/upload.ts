import { isMockSession } from '@/api/request'

const BASE_URL = '/api'
let useMock = false

export async function uploadFile(file: File): Promise<string> {
  if (useMock || isMockSession()) {
    // Mock 模式：返回一个本地预览 URL
    useMock = true
    return URL.createObjectURL(file)
  }
  try {
    const formData = new FormData()
    formData.append('file', file)
    const token = localStorage.getItem('mall-token') || ''
    const res = await fetch(`${BASE_URL}/upload`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
      body: formData,
    })
    const json = await res.json()
    if (json.code !== 0) throw new Error(json.message || '上传失败')
    return json.data.url as string
  } catch (e: any) {
    if (e?.message?.includes('未登录')) throw e
    useMock = true
    return URL.createObjectURL(file)
  }
}

export async function uploadFiles(files: File[]): Promise<string[]> {
  if (useMock || isMockSession()) {
    useMock = true
    return files.map((f) => URL.createObjectURL(f))
  }
  try {
    const formData = new FormData()
    files.forEach((f) => formData.append('files', f))
    const token = localStorage.getItem('mall-token') || ''
    const res = await fetch(`${BASE_URL}/upload/batch`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
      body: formData,
    })
    const json = await res.json()
    if (json.code !== 0) throw new Error(json.message || '上传失败')
    return json.data.urls as string[]
  } catch (e: any) {
    if (e?.message?.includes('未登录')) throw e
    useMock = true
    return files.map((f) => URL.createObjectURL(f))
  }
}
