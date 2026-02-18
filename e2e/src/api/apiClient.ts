import type { APIRequestContext } from '@playwright/test'

function url(base: string, path: string): string {
  return path.startsWith('/') ? `${base}${path}` : `${base}/${path}`
}

export function createApiClient(request: APIRequestContext, apiBase: string) {
  const base = apiBase.replace(/\/$/, '')
  return {
    async getJson<T>(path: string): Promise<{ status: number; body: T }> {
      const res = await request.get(url(base, path))
      const body = (await res.json().catch(() => ({}))) as T
      return { status: res.status(), body }
    },

    async postJson<T>(path: string, body: unknown): Promise<{ status: number; body: T }> {
      const res = await request.post(url(base, path), {
        data: body,
        headers: { 'Content-Type': 'application/json' },
      })
      const resBody = (await res.json().catch(() => ({}))) as T
      return { status: res.status(), body: resBody }
    },

    async putJson<T>(path: string, body: unknown): Promise<{ status: number; body: T }> {
      const res = await request.put(url(base, path), {
        data: body,
        headers: { 'Content-Type': 'application/json' },
      })
      const resBody = (await res.json().catch(() => ({}))) as T
      return { status: res.status(), body: resBody }
    },

    async delete(path: string): Promise<{ status: number }> {
      const res = await request.delete(url(base, path))
      return { status: res.status() }
    },
  }
}

export type ApiClient = ReturnType<typeof createApiClient>
