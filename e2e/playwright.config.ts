import { defineConfig, devices } from '@playwright/test'
import { getBaseUrl } from './src/config/env'

export default defineConfig({
  testDir: './tests',
  globalSetup: './globalSetup.ts',
  fullyParallel: true,
  workers: process.env.CI ? 2 : undefined,
  retries: 1,
  reporter: [['html', { open: 'never' }]],
  use: {
    baseURL: getBaseUrl(),
    trace: 'on-first-retry',
    video: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
})
