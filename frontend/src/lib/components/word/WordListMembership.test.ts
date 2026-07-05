import { fireEvent, render, screen } from '@testing-library/svelte';
import { describe, expect, it, vi } from 'vitest';
import WordListMembership from './WordListMembership.svelte';

const memberships = {
	data: [] as Array<{ id: string; title: string }>,
	isPending: false,
};
const allLists = {
	data: [] as Array<{ id: string; title: string; itemCount: number }>,
};

const addMutateAsync = vi.fn();
const removeMutate = vi.fn();

vi.mock('$lib/stores/wordLists', () => ({
	useListsForWord: () => memberships,
	useAllWordLists: () => allLists,
	useAddWordToList: () => ({ isPending: false, mutateAsync: addMutateAsync }),
	useRemoveWordFromList: () => ({ isPending: false, mutate: removeMutate }),
}));

describe('WordListMembership', () => {
	it('shows empty state when the word is in no list', () => {
		memberships.data = [];
		allLists.data = [];

		render(WordListMembership, { props: { wordId: 'word-1' } });

		expect(screen.getByText('Not in any list yet.')).toBeInTheDocument();
	});

	it('renders a chip per membership and removes on click', async () => {
		memberships.data = [{ id: 'list-a', title: 'Colors' }];
		allLists.data = [{ id: 'list-a', title: 'Colors', itemCount: 3 }];

		render(WordListMembership, { props: { wordId: 'word-1' } });

		expect(screen.getByRole('link', { name: 'Colors' })).toBeInTheDocument();

		await fireEvent.click(screen.getByRole('button', { name: 'Remove from Colors' }));
		expect(removeMutate).toHaveBeenCalledWith({ listId: 'list-a', wordId: 'word-1' });
	});

	it('offers only lists the word is not already in, and adds via select', async () => {
		memberships.data = [{ id: 'list-a', title: 'Colors' }];
		allLists.data = [
			{ id: 'list-a', title: 'Colors', itemCount: 3 },
			{ id: 'list-b', title: 'Family', itemCount: 5 },
		];

		render(WordListMembership, { props: { wordId: 'word-1' } });

		// "Colors" is already a member → not an <option>; "Family" is available.
		expect(screen.getByRole('option', { name: 'Family' })).toBeInTheDocument();
		expect(screen.queryByRole('option', { name: 'Colors' })).not.toBeInTheDocument();

		await fireEvent.change(screen.getByRole('combobox'), { target: { value: 'list-b' } });
		expect(addMutateAsync).toHaveBeenCalledWith({ listId: 'list-b', wordId: 'word-1' });
	});

	it('hides the add-to-list select when every list already contains the word', () => {
		memberships.data = [{ id: 'list-a', title: 'Colors' }];
		allLists.data = [{ id: 'list-a', title: 'Colors', itemCount: 3 }];

		render(WordListMembership, { props: { wordId: 'word-1' } });

		expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
	});
});
