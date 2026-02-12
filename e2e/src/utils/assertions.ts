import { expect } from '@playwright/test'

/**
 * Asserts response is 200 and body is exactly [].
 */
export function assertEmptyList200(response: { status: number; body: unknown }): void {
  expect(response.status).toBe(200)
  expect(Array.isArray(response.body)).toBe(true)
  expect((response.body as unknown[]).length).toBe(0)
}
