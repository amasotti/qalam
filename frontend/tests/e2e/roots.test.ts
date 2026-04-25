import { test, expect } from '@playwright/test';

const BACKEND = 'http://localhost:8085';

test.describe('Roots', () => {
	let createdId: string | undefined;

	test.afterEach(async ({ request }) => {
		if (createdId) {
			await request.delete(`${BACKEND}/api/v1/roots/${createdId}`);
			createdId = undefined;
		}
	});

	test('create root → detail page shows arabic and meaning', async ({ page }) => {
		await page.goto('/roots/new');

		await page.locator('#root-letters').fill('ص ب ر');
		await page.locator('#root-meaning').fill('patience, endurance');
		await page.getByRole('button', { name: 'Create root' }).click();

		await page.waitForURL(/\/roots\/[\w-]+$/);
		createdId = page.url().split('/').at(-1);

		await expect(page.locator('.root-hero-arabic')).toBeVisible();
		await expect(page.locator('.root-hero-arabic')).toContainText('ص-ب-ر');

		// Delete created root
		await page.request.delete(`${BACKEND}/api/v1/roots/${createdId}`);
	});

	test('created root is searchable in list', async ({ page, request }) => {
		await page.goto('/roots');
		await page.getByPlaceholder(/search/i).fill('خ-ي-م');
		await expect(page.locator('.root-card').filter({ hasText: 'خ-ي-م' })).toBeVisible();
	});
});
