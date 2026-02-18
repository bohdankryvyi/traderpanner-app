import type { APIRequestContext } from '@playwright/test'
import { getApiBaseURL } from '../config/env'

export function createApiClient(request: APIRequestContext) {
  const apiBase = getApiBaseURL()
  return {
    async getJson<T>(path: string): Promise<{ status: number; body: T }> {
      const res = await request.get(`${apiBase}${path}`)
      const body = (await res.json().catch(() => ({}))) as T
      return { status: res.status(), body }
    },

    async postJson<T>(path: string, body: unknown): Promise<{ status: number; body: T }> {
      const res = await request.post(`${apiBase}${path}`, {
        data: body,
        headers: { 'Content-Type': 'application/json' },
      })
      const resBody = (await res.json().catch(() => ({}))) as T
      return { status: res.status(), body: resBody }
    },

    async putJson<T>(path: string, body: unknown): Promise<{ status: number; body: T }> {
      const res = await request.put(`${apiBase}${path}`, {
        data: body,
        headers: { 'Content-Type': 'application/json' },
      })
      const resBody = (await res.json().catch(() => ({}))) as T
      return { status: res.status(), body: resBody }
    },

    async delete(path: string): Promise<{ status: number }> {
      const res = await request.delete(`${apiBase}${path}`)
      return { status: res.status() }
    },
  }
}

export type ApiClient = ReturnType<typeof createApiClient>
