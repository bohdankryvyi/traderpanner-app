import type { ApiClient } from './apiClient'
import type { PortfolioPositionResponse } from './types'

export function createPortfolioApi(api: ApiClient) {
  return {
    async listPortfolio(): Promise<PortfolioPositionResponse[]> {
      const { status, body } = await api.getJson<PortfolioPositionResponse[]>('/api/portfolio')
      if (status !== 200) return []
      return Array.isArray(body) ? body : []
    },

    async getListResponse(): Promise<{ status: number; body: PortfolioPositionResponse[] }> {
      const out = await api.getJson<PortfolioPositionResponse[]>('/api/portfolio')
      return { status: out.status, body: Array.isArray(out.body) ? out.body : [] }
    },

    /** 0 matches -> null; 1 -> id; >1 -> throw. */
    async findIdByNotes(notes: string): Promise<number | null> {
      const list = await this.listPortfolio()
      const trimmed = notes.trim()
      const matches = list.filter((p) => (p.notes ?? '').trim() === trimmed)
      if (matches.length === 0) return null
      if (matches.length > 1) {
        throw new Error(
          `Data integrity: multiple portfolio positions with notes "${notes}" (${matches.length}). Expected at most one.`
        )
      }
      return matches[0].id
    },

    async assertNoPositionWithNotes(notes: string): Promise<void> {
      const list = await this.listPortfolio()
      const trimmed = notes.trim()
      const found = list.some((p) => (p.notes ?? '').trim() === trimmed)
      if (found)
        throw new Error(`Expected no portfolio position with notes "${notes}" but found one`)
    },

    async assertNoPositionsWithPrefix(prefix: string): Promise<void> {
      const list = await this.listPortfolio()
      const withPrefix = list.filter((p) => (p.notes ?? '').startsWith(prefix))
      if (withPrefix.length > 0) {
        throw new Error(
          `Expected no portfolio positions with notes prefix "${prefix}" but found ${withPrefix.length}`
        )
      }
    },

    /** Delete only positions whose notes start with prefix; verify none remain. */
    async deleteE2EPortfolioAndVerify(prefix: string): Promise<void> {
      const list = await this.listPortfolio()
      const toDelete = list.filter((p) => (p.notes ?? '').startsWith(prefix))
      for (const pos of toDelete) {
        await api.delete(`/api/portfolio/${pos.id}`)
      }
      const after = await this.listPortfolio()
      const remaining = after.filter((p) => (p.notes ?? '').startsWith(prefix))
      if (remaining.length > 0) {
        throw new Error(
          `Cleanup verification failed: ${remaining.length} portfolio position(s) with prefix "${prefix}" still remain after delete`
        )
      }
    },
  }
}

export type PortfolioApi = ReturnType<typeof createPortfolioApi>
