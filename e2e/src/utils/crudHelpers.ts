import { expect } from '@playwright/test'
import type { TradingPage } from '../pages/TradingPage'
import type { PortfolioPage } from '../pages/PortfolioPage'
import type { TradingApi } from '../api/tradingApi'
import type { PortfolioApi } from '../api/portfolioApi'
import type { TradingEntryFormData } from '../data/builders/tradingEntryBuilder'
import type { PortfolioPositionFormData } from '../data/builders/portfolioPositionBuilder'
import type { TradingEntryResponse } from '../api/types'
import type { PortfolioPositionResponse } from '../api/types'

const POLL_TIMEOUT_MS = 10_000

function lastNotesDebug(body: TradingEntryResponse[]): string {
  const notes = body.slice(-5).map((e) => e.note ?? '')
  return JSON.stringify(notes)
}

function lastNotesPortfolioDebug(body: PortfolioPositionResponse[]): string {
  const notes = body.slice(-5).map((p) => p.notes ?? '')
  return JSON.stringify(notes)
}

/**
 * Create trading entry via UI and resolve its id from API list (exact note match).
 * On poll timeout throws with note, last GET status, and last 5 notes from list.
 */
export async function createTradingEntryAndResolveId(
  tradingPage: TradingPage,
  tradingApi: TradingApi,
  data: TradingEntryFormData
): Promise<number> {
  await tradingPage.goto()
  await tradingPage.createEntry(data)
  let id: number | null = null
  let lastResponse: { status: number; body: TradingEntryResponse[] } | null = null
  try {
    await expect
      .poll(
        async () => {
          lastResponse = await tradingApi.getListResponse()
          id = await tradingApi.findIdByNote(data.note)
          return id
        },
        { timeout: POLL_TIMEOUT_MS }
      )
      .not.toBeNull()
  } catch {
    const status = lastResponse?.status ?? 'N/A'
    const debug = lastResponse ? lastNotesDebug(lastResponse.body) : 'N/A'
    throw new Error(
      `Create+resolve timed out. note=${data.note} | last GET status=${status} | last 5 notes from list: ${debug}`
    )
  }
  return id!
}

/**
 * Create portfolio position via UI and resolve its id from API list (exact notes match).
 * On poll timeout throws with notes, last GET status, and last 5 notes from list.
 */
export async function createPortfolioPositionAndResolveId(
  portfolioPage: PortfolioPage,
  portfolioApi: PortfolioApi,
  data: PortfolioPositionFormData
): Promise<number> {
  await portfolioPage.goto()
  await portfolioPage.createPosition(data)
  let id: number | null = null
  let lastResponse: { status: number; body: PortfolioPositionResponse[] } | null = null
  try {
    await expect
      .poll(
        async () => {
          lastResponse = await portfolioApi.getListResponse()
          id = await portfolioApi.findIdByNotes(data.notes)
          return id
        },
        { timeout: POLL_TIMEOUT_MS }
      )
      .not.toBeNull()
  } catch {
    const status = lastResponse?.status ?? 'N/A'
    const debug = lastResponse ? lastNotesPortfolioDebug(lastResponse.body) : 'N/A'
    throw new Error(
      `Create+resolve timed out. notes=${data.notes} | last GET status=${status} | last 5 notes from list: ${debug}`
    )
  }
  return id!
}
