import axios from 'axios'

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export const apiClient = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
})

export function getApiErrorMessage(err: unknown): string {
  if (!axios.isAxiosError(err)) return 'Unexpected error'
  const data = err.response?.data as { message?: string } | undefined
  if (data && typeof data.message === 'string') return data.message
  if (typeof err.message === 'string' && err.message) return err.message
  return 'Request failed'
}
