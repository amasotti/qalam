import { expect, test } from '@playwright/test';

const BACKEND = 'http://localhost:8085';
const UUID_RE = /\/word-lists\/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;

test.describe('Word lists', () => {
	let createdId: string | undefined;

	test.afterEach(async ({ request }) => {
		if (createdId) {
			await request.delete(`${BACKEND}/api/v1/word-lists/${createdId}`);
			createdId = undefined;
		}
	});

	test('create list → detail page shows the title', async ({ page }) => {
		await page.goto('/word-lists/new');

		await page.locator('#wl-title').fill('Colors — playwright');
		await page.locator('#wl-desc').fill('e2e test list');
		await page.getByRole('button', { name: 'Create list' }).click();

		await page.waitForURL(UUID_RE);
		createdId = page.url().split('/').at(-1);

		await expect(page.locator('.list-page-title')).toHaveText('Colors — playwright');
		await expect(page.getByText('No words yet — search above to add some.')).toBeVisible();
	});

	test('create list button disabled when title empty', async ({ page }) => {
		await page.goto('/word-lists/new');

		const submitBtn = page.getByRole('button', { name: 'Create list' });
		await expect(submitBtn).toBeDisabled();

		await page.locator('#wl-title').fill('Family');
		await expect(submitBtn).toBeEnabled();

		await page.locator('#wl-title').clear();
		await expect(submitBtn).toBeDisabled();
	});

	test('delete list → redirects to index, list no longer accessible', async ({ page, request }) => {
		const res = await request.post(`${BACKEND}/api/v1/word-lists`, {
			data: { title: 'Temp — playwright delete' },
		});
		expect(res.status()).toBe(201);
		const { id } = await res.json();

		await page.goto(`/word-lists/${id}`);

		// Two-click confirmation pattern
		await page.getByRole('button', { name: 'Delete' }).click();
		await page.getByRole('button', { name: 'Confirm delete' }).click();

		await page.waitForURL('/word-lists');
		const check = await request.get(`${BACKEND}/api/v1/word-lists/${id}`);
		expect(check.status()).toBe(404);
	});

	test('created list appears on the index', async ({ page, request }) => {
		const title = 'Kitchen — playwright search';
		const res = await request.post(`${BACKEND}/api/v1/word-lists`, { data: { title } });
		expect(res.status()).toBe(201);
		const { id } = await res.json();

		try {
			await page.goto('/word-lists');
			await expect(page.locator('.wl-card').filter({ hasText: title })).toBeVisible();
		} finally {
			await request.delete(`${BACKEND}/api/v1/word-lists/${id}`);
		}
	});
});
