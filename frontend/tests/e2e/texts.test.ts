import { test, expect } from '@playwright/test';

const BACKEND = 'http://localhost:8085';
const SENTENCE = 'الصَّبْرُ مِفْتَاحُ الْفَرَجِ';
const UUID_RE = /\/texts\/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;

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

		await page.locator('#tf-title').fill('Playwright test text');
		await page.getByRole('button', { name: 'Create text' }).click();

		await page.waitForURL(UUID_RE);
		createdId = page.url().split('/').at(-1);

		await expect(page.getByRole('heading', { name: 'Playwright test text' })).toBeVisible();
	});

	test('edit text title → updated title shown on detail page', async ({ page, request }) => {
		const res = await request.post(`${BACKEND}/api/v1/texts`, {
			data: { title: 'Original title', dialect: 'MSA', difficulty: 'BEGINNER', tags: [] },
		});
		expect(res.status()).toBe(201);
		createdId = (await res.json()).id;

		await page.goto(`/texts/${createdId}`);

		await page.getByTitle('Edit text info').click();

		await page.locator('#tf-title').clear();
		await page.locator('#tf-title').fill('Updated title');
		await page.getByRole('button', { name: 'Save changes' }).click();

		await expect(page.getByRole('heading', { name: 'Updated title' })).toBeVisible();
	});

	test('add sentence → sentence appears in editor', async ({ page, request }) => {
		const res = await request.post(`${BACKEND}/api/v1/texts`, {
			data: { title: 'Playwright sentence test', dialect: 'MSA', difficulty: 'BEGINNER', tags: [] },
		});
		expect(res.status()).toBe(201);
		createdId = (await res.json()).id;

		await page.goto(`/texts/${createdId}`);

		// Empty state → click to enter sentence editing mode
		await page.getByRole('button', { name: 'Add sentences' }).click();

		// SentenceEditor rendered with addingNew=false → click toggle to open the form
		await page.getByRole('button', { name: 'Add sentence' }).click();

		// Form now visible — fill Arabic text
		await page.locator('.new-sentence-form .sentence-edit-textarea').fill(SENTENCE);

		// Submit — same button text, but now it's the form submit (toggle is hidden)
		await page.getByRole('button', { name: 'Add sentence' }).click();

		// Sentence block appears in the editor
		await expect(page.locator('.sentence-edit-block')).toBeVisible();
	});
});
