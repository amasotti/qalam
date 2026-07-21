import { test, expect } from '@playwright/test';

const BACKEND = 'http://localhost:8085';
// Non-word combinations — won't exist in real Arabic vocabulary data
const TEST_ROOT = 'ظ ق ء';
const TEST_ROOT_DELETE = 'غ ذ خ';
const UUID_RE = /\/roots\/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
const ROOT_LETTERS = ['ث', 'ج', 'ح', 'خ', 'ذ', 'ز', 'ش', 'ص', 'ض', 'ط', 'ظ', 'غ', 'ف', 'ق'];

function uniqueTestRoot() {
	return Array.from({ length: 6 }, () => ROOT_LETTERS[Math.floor(Math.random() * ROOT_LETTERS.length)]).join(' ');
}

test.describe('Roots', () => {
	let createdId: string | undefined;
	let createdWordId: string | undefined;

	test.afterEach(async ({ request }) => {
		if (createdWordId) {
			await request.delete(`${BACKEND}/api/v1/words/${createdWordId}`);
			createdWordId = undefined;
		}
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

		await expect(page.locator('.root-letter-ar')).toBeVisible();
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

	test('created root is searchable in list', async ({ page, request }) => {
		const root = uniqueTestRoot();
		const displayForm = root.replaceAll(' ', '-');
		const res = await request.post(`${BACKEND}/api/v1/roots`, {
			data: { root, meaning: 'playwright search test' },
		});
		expect(res.status()).toBe(201);
		const { id } = await res.json();

		try {
			await page.goto('/roots');
			await page.getByPlaceholder(/search/i).fill(displayForm);
			await expect(page.locator('.root-card').filter({ hasText: displayForm })).toBeVisible();
		} finally {
			await request.delete(`${BACKEND}/api/v1/roots/${id}`);
		}
	});

	test('AI family preview creates a reviewed word and refreshes family', async ({ page, request }) => {
		const root = uniqueTestRoot();
		const res = await request.post(`${BACKEND}/api/v1/roots`, {
			data: { root, meaning: 'playwright family suggestion' },
		});
		expect(res.status()).toBe(201);
		const { id } = await res.json();
		createdId = id;

		await page.route(`**/api/v1/roots/${id}/suggest-words`, async (route) => {
			await route.fulfill({
				contentType: 'application/json',
				body: JSON.stringify({
					suggestions: [{
						arabicText: 'غَرِيبَةٌ',
						transliteration: 'ghariba',
						translation: 'strange test word',
						partOfSpeech: 'ADJECTIVE',
						dialect: 'MSA',
						difficulty: 'INTERMEDIATE',
					}],
				}),
			});
		});

		await page.goto(`/roots/${id}`);
		await page.getByRole('button', { name: '✦ AI Suggest family words' }).click();
		await page.getByRole('button', { name: 'Create word' }).click();
		await expect(page.getByText('✓ Added to family')).toBeVisible();
		await expect(page.locator('.root-word-chip').filter({ hasText: 'غَرِيبَةٌ' })).toBeVisible();

		const createdWord = await request.get(`${BACKEND}/api/v1/words/by-arabic?q=${encodeURIComponent('غَرِيبَةٌ')}`);
		createdWordId = (await createdWord.json()).id;
	});
});
