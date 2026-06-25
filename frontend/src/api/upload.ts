import { isMockSession } from '@/api/request'

const BASE_URL = '/api'
let useMock = false

function fileToDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('读取本地文件失败'))
    reader.readAsDataURL(file)
  })
}

export async function uploadFile(file: File): Promise<string> {
  if (useMock || isMockSession()) {
    // Mock 模式使用可持久化 data URL；blob URL 刷新后会失效，不能保存到业务数据里
    useMock = true
    return fileToDataUrl(file)
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
    throw e
  }
}

export async function uploadFiles(files: File[]): Promise<string[]> {
  if (useMock || isMockSession()) {
    useMock = true
    return Promise.all(files.map((f) => fileToDataUrl(f)))
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
    throw e
  }
}
