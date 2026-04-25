import { test, expect } from '@playwright/test';

const BACKEND = 'http://localhost:8085';
// Non-word combination — won't exist in real Arabic vocabulary data
const TEST_ROOT = 'ظ ق ء';
const UUID_RE = /\/roots\/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;

test.describe('Roots', () => {
	let createdId: string | undefined;

	test.afterEach(async ({ request }) => {
		if (createdId) {
			await request.delete(`${BACKEND}/api/v1/roots/${createdId}`);
			createdId = undefined;
		}
	});

	test('create root → detail page shows arabic letters', async ({ page }) => {
		await page.goto('/roots/new');

		await page.locator('#root-letters').fill(TEST_ROOT);
		await page.locator('#root-meaning').fill('test — playwright');
		await page.getByRole('button', { name: 'Create root' }).click();

		await page.waitForURL(UUID_RE);
		createdId = page.url().split('/').at(-1);

		await expect(page.locator('.root-hero-arabic')).toBeVisible();
	});

	test('created root is searchable in list', async ({ page, request }) => {
		await page.goto('/roots');
		await page.getByPlaceholder(/search/i).fill('خ-ي-م');
		await expect(page.locator('.root-card').filter({ hasText: 'خ-ي-م' })).toBeVisible();
	});
});
