import { test, expect } from '@playwright/test';

const BACKEND = 'http://localhost:8085';

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

		await page.waitForURL(/\/words\/[\w-]+$/);
		createdId = page.url().split('/').at(-1);

		await expect(page.locator('.word-hero-arabic')).toContainText('كِتَابَة');
		await expect(page.locator('.word-hero-transliteration')).toContainText('kitāba');
	});
});
