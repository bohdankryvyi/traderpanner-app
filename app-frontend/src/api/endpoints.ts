import { apiClient } from './apiClient'
import type {
  AiTipsResponse,
  PatternResponse,
  PortfolioPositionRequest,
  PortfolioPositionResponse,
  PricesResponse,
  SecurityDto,
  TradingEntryRequest,
  TradingEntryResponse,
} from './types'

export const api = {
  securities: {
    list: async (q?: string): Promise<SecurityDto[]> => {
      const res = await apiClient.get<SecurityDto[]>('/api/securities', {
        params: q ? { q } : undefined,
      })
      return res.data
    },
  },
  trading: {
    list: async (): Promise<TradingEntryResponse[]> => {
      const res = await apiClient.get<TradingEntryResponse[]>('/api/trading')
      return res.data
    },
    create: async (payload: TradingEntryRequest): Promise<TradingEntryResponse> => {
      const res = await apiClient.post<TradingEntryResponse>('/api/trading', payload)
      return res.data
    },
    update: async (id: number, payload: TradingEntryRequest): Promise<TradingEntryResponse> => {
      const res = await apiClient.put<TradingEntryResponse>(`/api/trading/${id}`, payload)
      return res.data
    },
    delete: async (id: number): Promise<void> => {
      await apiClient.delete(`/api/trading/${id}`)
    },
  },
  portfolio: {
    list: async (): Promise<PortfolioPositionResponse[]> => {
      const res = await apiClient.get<PortfolioPositionResponse[]>('/api/portfolio')
      return res.data
    },
    create: async (payload: PortfolioPositionRequest): Promise<PortfolioPositionResponse> => {
      const res = await apiClient.post<PortfolioPositionResponse>('/api/portfolio', payload)
      return res.data
    },
    update: async (
      id: number,
      payload: PortfolioPositionRequest
    ): Promise<PortfolioPositionResponse> => {
      const res = await apiClient.put<PortfolioPositionResponse>(`/api/portfolio/${id}`, payload)
      return res.data
    },
    delete: async (id: number): Promise<void> => {
      await apiClient.delete(`/api/portfolio/${id}`)
    },
  },
  prices: {
    get: async (tickers: string[]): Promise<PricesResponse> => {
      const res = await apiClient.get<PricesResponse>('/api/prices', {
        params: { tickers: tickers.join(',') },
      })
      return res.data
    },
  },
  ai: {
    tips: async (tf: '1h' | '1d'): Promise<AiTipsResponse> => {
      const res = await apiClient.post<AiTipsResponse>('/api/ai/tips', null, { params: { tf } })
      return res.data
    },
    pattern: async (tf: '1h' | '1d'): Promise<PatternResponse> => {
      const res = await apiClient.post<PatternResponse>('/api/ai/pattern', null, { params: { tf } })
      return res.data
    },
  },
}
