import { defineConfig, devices } from '@playwright/test';

/**
 * Requires the backend + DB to be running before tests start.
 * Locally: `just up` (or `just dev`), then `just e2e`.
 * CI: start the full docker-compose stack first.
 *
 * The frontend dev server is started automatically; it proxies /api → localhost:8085.
 */
export default defineConfig({
	testDir: './tests/e2e',
	fullyParallel: false, // serial — tests share DB state
	forbidOnly: !!process.env.CI,
	retries: process.env.CI ? 1 : 0,
	workers: 1,
	reporter: [['html', { open: 'never' }], ['list']],
	use: {
		baseURL: 'http://localhost:5173',
		trace: 'on-first-retry',
	},
	projects: [
		{ name: 'chromium', use: { ...devices['Desktop Chrome'] } },
	],
	webServer: {
		command: 'pnpm dev',
		url: 'http://localhost:5173',
		reuseExistingServer: !process.env.CI,
		timeout: 30_000,
	},
});
