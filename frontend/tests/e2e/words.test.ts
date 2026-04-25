import { test, expect } from '@playwright/test';

const BACKEND = 'http://localhost:8085';
const UUID_RE = /\/words\/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;

test.describe('Words', () => {
	let createdId: string | undefined;

	test.afterEach(async ({ request }) => {
		if (createdId) {
			await request.delete(`${BACKEND}/api/v1/words/${createdId}`);
			createdId = undefined;
		}
	});

	test('create word → detail page shows arabic and translation', async ({ page }) => {
		await page.goto('/words/new');

		await page.locator('#word-arabic').fill('كِتَابَة');
		await page.locator('#word-transliteration').fill('kitāba');
		await page.locator('#word-translation').fill('writing, the act of writing');
		await page.getByRole('button', { name: 'Create word' }).click();

		await page.waitForURL(UUID_RE);
		createdId = page.url().split('/').at(-1);

		await expect(page.locator('.word-hero-arabic')).toContainText('كِتَابَة');
		await expect(page.locator('.word-hero-transliteration')).toContainText('kitāba');
	});

	test('word linked to root appears in root word family', async ({ page, request }) => {
		let rootId: string | undefined;
		let wordId: string | undefined;

		try {
			const rootRes = await request.post(`${BACKEND}/api/v1/roots`, {
				data: { root: 'ض ظ غ', meaning: 'playwright word family test' },
			});
			expect(rootRes.status()).toBe(201);
			rootId = (await rootRes.json()).id;

			const wordRes = await request.post(`${BACKEND}/api/v1/words`, {
				data: {
					arabicText: 'ضظغ',
					transliteration: 'playwright',
					translation: 'playwright test word',
					partOfSpeech: 'NOUN',
					dialect: 'MSA',
					difficulty: 'BEGINNER',
					masteryLevel: 'NEW',
					rootId,
				},
			});
			expect(wordRes.status()).toBe(201);
			wordId = (await wordRes.json()).id;

			await page.goto(`/roots/${rootId}`);
			await expect(page.locator('.word-chip-arabic')).toContainText('ضظغ');
		} finally {
			if (wordId) await request.delete(`${BACKEND}/api/v1/words/${wordId}`);
			if (rootId) await request.delete(`${BACKEND}/api/v1/roots/${rootId}`);
		}
	});

	test('dialect filter narrows word list to selected dialect', async ({ page }) => {
		await page.goto('/words');

		// Baseline: some word cards visible (real DB has data)
		await expect(page.locator('.word-card').first()).toBeVisible();

		// Select MSA dialect
		await page.locator('.words-filter-select').first().selectOption('MSA');

		// Filter applied: words with MSA badge shown, select reflects choice
		await expect(page.locator('.words-filter-select').first()).toHaveValue('MSA');
		await expect(page.locator('.word-card').first()).toBeVisible();
		await expect(page.locator('.dialect-msa').first()).toBeVisible();
	});
});
