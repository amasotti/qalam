import { test, expect } from '@playwright/test';

const BACKEND = 'http://localhost:8085';
const SENTENCE = 'الصَّبْرُ مِفْتَاحُ الْفَرَجِ';

test.describe('Texts', () => {
	let createdId: string | undefined;

	test.afterEach(async ({ request }) => {
		if (createdId) {
			// Cascades to sentences and tokens
			await request.delete(`${BACKEND}/api/v1/texts/${createdId}`);
			createdId = undefined;
		}
	});

	test('create text → detail page shows title', async ({ page }) => {
		await page.goto('/texts/new');

		await page.locator('#tf-title').fill('Test text — patience');
		await page.getByRole('button', { name: 'Create text' }).click();

		await page.waitForURL(/\/texts\/[\w-]+$/);
		createdId = page.url().split('/').at(-1);

		await expect(page.locator('h1, .text-detail-title')).toContainText('Test text');
	});

	test('add sentence → sentence appears in editor', async ({ page, request }) => {
		// Create text via API — we're testing the sentence UI
		const res = await request.post(`${BACKEND}/api/v1/texts`, {
			data: { title: 'Sentence test text', dialect: 'MSA', difficulty: 'BEGINNER', tags: [] },
		});
		expect(res.status()).toBe(201);
		const body = await res.json();
		createdId = body.id;

		await page.goto(`/texts/${createdId}`);

		// No sentences yet → empty state with "Add sentences" button
		await page.getByRole('button', { name: 'Add sentences' }).click();

		// New sentence form appears
		const newSentenceArea = page.locator('.new-sentence-form .sentence-edit-textarea').first();
		await expect(newSentenceArea).toBeVisible();
		await newSentenceArea.fill(SENTENCE);

		await page.getByRole('button', { name: 'Add sentence' }).click();

		// Sentence appears in the editor list
		await expect(page.locator('.sentence-edit-block')).toBeVisible();
		await expect(page.locator('.arabic-text').first()).toContainText('الصبر');
	});
});
