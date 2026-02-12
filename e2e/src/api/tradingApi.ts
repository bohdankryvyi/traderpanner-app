import type { ApiClient } from './apiClient'
import type { TradingEntryResponse } from './types'

export function createTradingApi(api: ApiClient) {
  return {
    async listTrading(): Promise<TradingEntryResponse[]> {
      const { status, body } = await api.getJson<TradingEntryResponse[]>('/api/trading')
      if (status !== 200) return []
      return Array.isArray(body) ? body : []
    },

    async getListResponse(): Promise<{ status: number; body: TradingEntryResponse[] }> {
      const out = await api.getJson<TradingEntryResponse[]>('/api/trading')
      return { status: out.status, body: Array.isArray(out.body) ? out.body : [] }
    },

    async findIdByNote(note: string): Promise<number | null> {
      const list = await this.listTrading()
      const trimmed = note.trim()
      const matches = list.filter((e) => (e.note ?? '').trim() === trimmed)
      if (matches.length === 0) return null
      if (matches.length > 1) {
        throw new Error(
          'Data integrity: multiple trading entries with same note. Expected at most one.'
        )
      }
      return matches[0].id
    },

    async assertNoEntryWithNote(note: string): Promise<void> {
      const list = await this.listTrading()
      const trimmed = note.trim()
      const found = list.some((e) => (e.note ?? '').trim() === trimmed)
      if (found) throw new Error('Expected no trading entry with note but found one')
    },

    async assertNoEntriesWithPrefix(prefix: string): Promise<void> {
      const list = await this.listTrading()
      const withPrefix = list.filter((e) => (e.note ?? '').startsWith(prefix))
      if (withPrefix.length > 0) {
        throw new Error(
          'Expected no trading entries with note prefix but found ' + withPrefix.length
        )
      }
    },

    async deleteE2ETradingAndVerify(prefix: string): Promise<void> {
      const list = await this.listTrading()
      const toDelete = list.filter((e) => (e.note ?? '').startsWith(prefix))
      for (const entry of toDelete) {
        await api.delete('/api/trading/' + entry.id)
      }
      const after = await this.listTrading()
      const remaining = after.filter((e) => (e.note ?? '').startsWith(prefix))
      if (remaining.length > 0) {
        throw new Error(
          'Cleanup verification failed: trading entries with prefix still remain after delete'
        )
      }
    },
  }
}

export type TradingApi = ReturnType<typeof createTradingApi>
