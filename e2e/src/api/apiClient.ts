import type { APIRequestContext } from '@playwright/test'

const API_BASE = 'http://localhost:8080'

export function createApiClient(request: APIRequestContext) {
  return {
    async getJson<T>(path: string): Promise<{ status: number; body: T }> {
      const res = await request.get(`${API_BASE}${path}`)
      const body = (await res.json().catch(() => ({}))) as T
      return { status: res.status(), body }
    },

    async postJson<T>(path: string, body: unknown): Promise<{ status: number; body: T }> {
      const res = await request.post(`${API_BASE}${path}`, {
        data: body,
        headers: { 'Content-Type': 'application/json' },
      })
      const resBody = (await res.json().catch(() => ({}))) as T
      return { status: res.status(), body: resBody }
    },

    async putJson<T>(path: string, body: unknown): Promise<{ status: number; body: T }> {
      const res = await request.put(`${API_BASE}${path}`, {
        data: body,
        headers: { 'Content-Type': 'application/json' },
      })
      const resBody = (await res.json().catch(() => ({}))) as T
      return { status: res.status(), body: resBody }
    },

    async delete(path: string): Promise<{ status: number }> {
      const res = await request.delete(`${API_BASE}${path}`)
      return { status: res.status() }
    },
  }
}

export type ApiClient = ReturnType<typeof createApiClient>
