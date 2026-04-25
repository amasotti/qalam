import { test, expect } from '@playwright/test';

const WORKING_ROUTES = ['/', '/roots', '/words', '/texts'];

test.describe('Navigation', () => {
	for (const route of WORKING_ROUTES) {
		test(`${route} loads without error`, async ({ page }) => {
			await page.goto(route);
			// Error boundary shows status code — assert it's not visible
			await expect(page.locator('.error-page')).not.toBeVisible();
			// Page has at least some content
			await expect(page.locator('body')).not.toBeEmpty();
		});
	}

	test('sidebar is present on all pages', async ({ page }) => {
		for (const route of WORKING_ROUTES) {
			await page.goto(route);
			await expect(page.locator('.sidebar')).toBeVisible();
		}
	});
});
