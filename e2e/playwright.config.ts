import { defineConfig, devices } from '@playwright/test'
import { getBaseURL } from './src/config/env'

export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  workers: process.env.CI ? 2 : undefined,
  retries: 1,
  reporter: [['html', { open: 'never' }]],
  use: {
    baseURL: getBaseURL(),
    trace: 'on-first-retry',
    video: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  globalSetup: './globalSetup.ts',
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
})
