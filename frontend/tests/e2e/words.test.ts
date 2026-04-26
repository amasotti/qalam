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

		await expect(page.locator('.word-hero-ar')).toContainText('كِتَابَة');
		await expect(page.locator('.word-hero-tr')).toContainText('kitāba');
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
			await expect(page.locator('.wc-ar')).toContainText('ضظغ');
		} finally {
			if (wordId) await request.delete(`${BACKEND}/api/v1/words/${wordId}`);
			if (rootId) await request.delete(`${BACKEND}/api/v1/roots/${rootId}`);
		}
	});

	test('dialect filter narrows word list to selected dialect', async ({ page, request }) => {
		const res = await request.post(`${BACKEND}/api/v1/words`, {
			data: {
				arabicText: 'تَصْفِيَة',
				transliteration: 'taṣfiya',
				translation: 'playwright dialect filter test',
				partOfSpeech: 'NOUN',
				dialect: 'MSA',
				difficulty: 'BEGINNER',
				masteryLevel: 'NEW',
			},
		});
		expect(res.status()).toBe(201);
		const { id } = await res.json();

		try {
			await page.goto('/words');
			await page.locator('.words-filters .filter-select').first().selectOption('MSA');

			await expect(page.locator('.words-filters .filter-select').first()).toHaveValue('MSA');
			await expect(page.locator('.word-card').first()).toBeVisible();
			await expect(page.locator('.word-card .chip').filter({ hasText: 'MSA' }).first()).toBeVisible();
		} finally {
			await request.delete(`${BACKEND}/api/v1/words/${id}`);
		}
	});
});
