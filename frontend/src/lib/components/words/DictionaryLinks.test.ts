import { fireEvent, render, screen } from '@testing-library/svelte';
import { describe, expect, it, vi } from 'vitest';
import DictionaryLinks from './DictionaryLinks.svelte';

const queryState = {
	data: [] as Array<{ id: string; source: string; url: string }>,
	isPending: false,
	isError: false,
};

const mutateAsyncMock = vi.fn();
const mutateMock = vi.fn();
const deleteMutateMock = vi.fn();

vi.mock('$lib/stores/words', () => ({
	useDictionaryLinks: () => queryState,
	useAddDictionaryLink: () => ({
		isPending: false,
		mutateAsync: mutateAsyncMock,
		mutate: mutateMock,
	}),
	useDeleteDictionaryLink: () => ({
		isPending: false,
		mutate: deleteMutateMock,
	}),
}));

describe('DictionaryLinks', () => {
	it('shows empty state when no links exist', () => {
		queryState.data = [];

		render(DictionaryLinks, {
			props: {
				wordId: 'word-1',
				arabicText: 'كتاب',
			},
		});

		expect(screen.getByText('No dictionary links yet.')).toBeInTheDocument();
	});

	it('autofills templated URL and submits selected source', async () => {
		queryState.data = [];
		mutateMock.mockImplementation((_payload, options) => options?.onSuccess?.());

		render(DictionaryLinks, {
			props: {
				wordId: 'word-1',
				arabicText: 'كتاب',
			},
		});

		await fireEvent.click(screen.getByRole('button', { name: '+ Custom link' }));

		const urlInput = screen.getByPlaceholderText('URL') as HTMLInputElement;
		expect(urlInput.value).toBe('https://www.almaany.com/en/dict/ar-en/%D9%83%D8%AA%D8%A7%D8%A8');

		await fireEvent.change(screen.getByRole('combobox'), {
			target: { value: 'WIKTIONARY' },
		});
		expect(urlInput.value).toBe('https://en.wiktionary.org/wiki/%D9%83%D8%AA%D8%A7%D8%A8');

		await fireEvent.click(screen.getByRole('button', { name: 'Add' }));

		expect(mutateMock).toHaveBeenCalledWith(
			{
				id: 'word-1',
				body: {
					source: 'WIKTIONARY',
					url: 'https://en.wiktionary.org/wiki/%D9%83%D8%AA%D8%A7%D8%A8',
				},
			},
			expect.objectContaining({
				onSuccess: expect.any(Function),
				onError: expect.any(Function),
			})
		);
	});
});
