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
	it('shows quick-add buttons when no links exist', () => {
		queryState.data = [];

		render(DictionaryLinks, {
			props: {
				wordId: 'word-1',
				arabicText: 'كتاب',
			},
		});

		expect(screen.getByText('Almaany')).toBeInTheDocument();
		expect(screen.getByText('Living Arabic')).toBeInTheDocument();
	});

	it('renders existing links as rows', () => {
		queryState.data = [
			{ id: 'l1', source: 'ALMANY', url: 'https://almaany.com/test' },
		];

		render(DictionaryLinks, {
			props: {
				wordId: 'word-1',
				arabicText: 'كتاب',
			},
		});

		const link = screen.getByText('Almaany');
		expect(link.closest('a')).toHaveAttribute('href', 'https://almaany.com/test');
	});

	it('opens custom add form and autofills URL', async () => {
		queryState.data = [];
		mutateMock.mockImplementation((_payload, options) => options?.onSuccess?.());

		render(DictionaryLinks, {
			props: {
				wordId: 'word-1',
				arabicText: 'كتاب',
			},
		});

		await fireEvent.click(screen.getByText('Custom'));

		const urlInput = screen.getByPlaceholderText('URL') as HTMLInputElement;
		expect(urlInput.value).toBe('https://www.almaany.com/en/dict/ar-en/%D9%83%D8%AA%D8%A7%D8%A8');
	});
});