import { defineConfig, devices } from '@playwright/test';

const reviewBrowser = { channel: process.env.PLAYWRIGHT_CHANNEL || undefined, video: 'off', trace: 'retain-on-failure' };

/**
 * PITAGORAS - Playwright End-to-End Test Configuration
 * Resolves TS-06 (Formalized E2E test runner configuration)
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 30 * 1000,
  expect: {
    timeout: 5000,
  },
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['list']
  ],
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    { name: 'mobile', use: { ...devices['Desktop Chrome'], ...reviewBrowser, viewport: { width: 360, height: 800 }, isMobile: true, hasTouch: true } },
    { name: 'tablet', use: { ...devices['Desktop Chrome'], ...reviewBrowser, viewport: { width: 768, height: 1024 }, hasTouch: true } },
    { name: 'tablet-landscape', use: { ...devices['Desktop Chrome'], ...reviewBrowser, viewport: { width: 1024, height: 768 }, hasTouch: true } },
    { name: 'desktop-review', use: { ...devices['Desktop Chrome'], ...reviewBrowser, viewport: { width: 1366, height: 768 } } },
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
  ],
  /* Run local dev server before starting the tests */
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: !process.env.CI,
    timeout: 120 * 1000,
  },
});
