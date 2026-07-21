import { fireEvent, render, screen, waitFor } from '@testing-library/svelte';
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import type { AiRootWordSuggestion, WordResponse } from '$lib/api/types.gen';
import RootFamilySuggestDialog from './RootFamilySuggestDialog.svelte';

const suggestMutateAsync = vi.fn();
const lookupMutateAsync = vi.fn();
const createMutateAsync = vi.fn();
const updateMutateAsync = vi.fn();

vi.mock('$lib/stores/roots', () => ({
	useSuggestWordsForRoot: () => ({ mutateAsync: suggestMutateAsync }),
}));

vi.mock('$lib/stores/words', () => ({
	useLookupWordByArabic: () => ({ mutateAsync: lookupMutateAsync }),
	useCreateWord: () => ({ mutateAsync: createMutateAsync }),
	useUpdateWord: () => ({ mutateAsync: updateMutateAsync }),
}));

const suggestion: AiRootWordSuggestion = {
	arabicText: 'كِتَاب',
	transliteration: 'kitāb',
	translation: 'book',
	partOfSpeech: 'NOUN',
	dialect: 'MSA',
};

function word(rootId: string | null): WordResponse {
	return {
		id: 'word-1',
		arabicText: suggestion.arabicText,
		transliteration: suggestion.transliteration,
		translation: suggestion.translation,
		partOfSpeech: suggestion.partOfSpeech,
		dialect: suggestion.dialect,
		difficulty: 'INTERMEDIATE',
		masteryLevel: 'NEW',
		rootId,
		createdAt: '2026-01-01T00:00:00Z',
		updatedAt: '2026-01-01T00:00:00Z',
	};
}

beforeAll(() => {
	HTMLDialogElement.prototype.showModal = function () {
		this.setAttribute('open', '');
	};
	HTMLDialogElement.prototype.close = function () {
		this.removeAttribute('open');
	};
});

beforeEach(() => {
	vi.clearAllMocks();
	suggestMutateAsync.mockResolvedValue([suggestion]);
});

function renderDialog(onAiUnavailable = vi.fn()) {
	return {
		onAiUnavailable,
		...render(RootFamilySuggestDialog, {
			props: { rootId: 'root-1', open: true, onClose: vi.fn(), onAiUnavailable },
		}),
	};
}

describe('RootFamilySuggestDialog', () => {
	it('creates a missing word with root id and intermediate fallback', async () => {
		lookupMutateAsync.mockResolvedValue(null);
		createMutateAsync.mockResolvedValue(word('root-1'));

		renderDialog();

		await screen.findByRole('button', { name: 'Create word' });
		await fireEvent.click(screen.getByRole('button', { name: 'Create word' }));

		await waitFor(() => {
			expect(createMutateAsync).toHaveBeenCalledWith({
				...suggestion,
				difficulty: 'INTERMEDIATE',
				rootId: 'root-1',
			});
		});
		expect(await screen.findByText('✓ Added to family')).toBeInTheDocument();
	});

	it('links an unlinked word but only offers navigation for a word in another family', async () => {
		lookupMutateAsync.mockResolvedValueOnce(word(null)).mockResolvedValueOnce(word('other-root'));
		updateMutateAsync.mockResolvedValue(word('root-1'));
		suggestMutateAsync.mockResolvedValue([
			suggestion,
			{ ...suggestion, arabicText: 'مَكْتَب', transliteration: 'maktab', translation: 'desk' },
		]);

		renderDialog();

		await screen.findByRole('button', { name: 'Link to family' });
		expect(screen.getByRole('link', { name: 'View word' })).toHaveAttribute(
			'href',
			'/words/word-1'
		);
		await fireEvent.click(screen.getByRole('button', { name: 'Link to family' }));
		await waitFor(() =>
			expect(updateMutateAsync).toHaveBeenCalledWith({ id: 'word-1', body: { rootId: 'root-1' } })
		);
	});

	it('reports unavailable AI to the owning page', async () => {
		const unavailable = vi.fn();
		suggestMutateAsync.mockRejectedValue({ status: 503 });

		renderDialog(unavailable);

		await screen.findByText('AI is not configured for this app.');
		expect(unavailable).toHaveBeenCalledOnce();
	});
});
