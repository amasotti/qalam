import { test, expect } from '@playwright/test';

const BACKEND = 'http://localhost:8085';
// Non-word combinations — won't exist in real Arabic vocabulary data
const TEST_ROOT = 'ظ ق ء';
const TEST_ROOT_DELETE = 'غ ذ خ';
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

	test('create root button disabled when letters field empty', async ({ page }) => {
		await page.goto('/roots/new');

		const submitBtn = page.getByRole('button', { name: 'Create root' });
		await expect(submitBtn).toBeDisabled();

		await page.locator('#root-letters').fill('ك ت ب');
		await expect(submitBtn).toBeEnabled();

		await page.locator('#root-letters').clear();
		await expect(submitBtn).toBeDisabled();
	});

	test('delete root → redirects to list, root no longer accessible', async ({ page, request }) => {
		const res = await request.post(`${BACKEND}/api/v1/roots`, {
			data: { root: TEST_ROOT_DELETE, meaning: 'playwright delete test' },
		});
		expect(res.status()).toBe(201);
		const { id } = await res.json();

		await page.goto(`/roots/${id}`);

		// Two-click confirmation pattern
		await page.getByRole('button', { name: 'Delete' }).click();
		await page.getByRole('button', { name: 'Confirm delete' }).click();

		await page.waitForURL('/roots');
		const check = await request.get(`${BACKEND}/api/v1/roots/${id}`);
		expect(check.status()).toBe(404);
	});

	test('created root is searchable in list', async ({ page }) => {
		await page.goto('/roots');
		await page.getByPlaceholder(/search/i).fill('خ-ي-م');
		await expect(page.locator('.root-card').filter({ hasText: 'خ-ي-م' })).toBeVisible();
	});
});
