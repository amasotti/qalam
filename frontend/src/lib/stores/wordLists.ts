import { createMutation, createQuery, useQueryClient } from '@tanstack/svelte-query';
import {
	addWordToList,
	createWordList,
	deleteWordList,
	getWordListById,
	listsForWord,
	listWordLists,
	removeWordFromList,
	suggestWordsForList,
	updateWordList,
} from '$lib/api/sdk.gen';
import type {
	AiListWordSuggestion,
	CreateWordListRequest,
	UpdateWordListRequest,
	WordListRefResponse,
	WordListResponse,
} from '$lib/api/types.gen';

function requireData<T>(data: T | undefined, label: string): T {
	if (data === undefined) throw new Error(`${label}: empty response`);
	return data;
}

/** Fetch all lists in one shot — personal tool, a handful of lists. */
export function useAllWordLists() {
	return createQuery(() => ({
		queryKey: ['wordLists', 'all'],
		queryFn: async () => {
			const { data, error } = await listWordLists({ query: { size: 500 } });
			if (error) throw error;
			return (requireData(data, 'listWordLists').items ?? []) as WordListResponse[];
		},
	}));
}

export function useWordList(id: () => string | undefined) {
	return createQuery(() => ({
		queryKey: ['wordLists', id()],
		queryFn: async () => {
			const resolved = id();
			if (!resolved) throw new Error('Missing list id');
			const { data, error } = await getWordListById({ path: { id: resolved } });
			if (error) throw error;
			return requireData(data, 'getWordListById');
		},
		enabled: !!id(),
	}));
}

/** Lists a given word belongs to — powers word-detail membership. */
export function useListsForWord(wordId: () => string | undefined) {
	return createQuery(() => ({
		queryKey: ['wordLists', 'forWord', wordId()],
		queryFn: async () => {
			const resolved = wordId();
			if (!resolved) throw new Error('Missing word id');
			const { data, error } = await listsForWord({ path: { wordId: resolved } });
			if (error) throw error;
			return requireData(data, 'listsForWord') as WordListRefResponse[];
		},
		enabled: !!wordId(),
	}));
}

export function useCreateWordList() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async (req: CreateWordListRequest) => {
			const { data, error } = await createWordList({ body: req });
			if (error) throw error;
			return requireData(data, 'createWordList');
		},
		onSuccess: () => {
			qc.invalidateQueries({ queryKey: ['wordLists'] });
		},
	}));
}

export function useUpdateWordList() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({ id, body }: { id: string; body: UpdateWordListRequest }) => {
			const { data, error } = await updateWordList({ path: { id }, body });
			if (error) throw error;
			return requireData(data, 'updateWordList');
		},
		onSuccess: () => {
			qc.invalidateQueries({ queryKey: ['wordLists'] });
		},
	}));
}

export function useDeleteWordList() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async (id: string) => {
			const { error } = await deleteWordList({ path: { id } });
			if (error) throw error;
		},
		onSuccess: () => {
			qc.invalidateQueries({ queryKey: ['wordLists'] });
		},
	}));
}

export function useAddWordToList() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({ listId, wordId }: { listId: string; wordId: string }) => {
			const { error } = await addWordToList({ path: { id: listId }, body: { wordId } });
			if (error) throw error;
		},
		onSuccess: () => {
			qc.invalidateQueries({ queryKey: ['wordLists'] });
		},
	}));
}

/** AI-suggest new words for a list (ephemeral — nothing is saved until the user adds a word). */
export function useSuggestWordsForList() {
	return createMutation(() => ({
		mutationFn: async (listId: string) => {
			const { data, error } = await suggestWordsForList({ path: { id: listId } });
			if (error) throw error;
			return (requireData(data, 'suggestWordsForList').suggestions ?? []) as AiListWordSuggestion[];
		},
	}));
}

export function useRemoveWordFromList() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({ listId, wordId }: { listId: string; wordId: string }) => {
			const { error } = await removeWordFromList({ path: { id: listId, wordId } });
			if (error) throw error;
		},
		onSuccess: () => {
			qc.invalidateQueries({ queryKey: ['wordLists'] });
		},
	}));
}
