import { createMutation, createQuery, useQueryClient } from '@tanstack/svelte-query';
import {
	addDictionaryLink,
	autocompleteWords,
	createWord,
	deleteDictionaryLink,
	deleteWord,
	generateWordExamples,
	getAnnotationsForWord,
	getWordById,
	listDictionaryLinks,
	listWords,
	updateWord,
} from '$lib/api/sdk.gen';
import type {
	AnnotationResponse,
	CreateDictionaryLinkRequest,
	CreateWordRequest,
	Dialect,
	DictionaryLinkResponse,
	Difficulty,
	ExamplesResponse,
	MasteryLevel,
	PartOfSpeech,
	UpdateWordRequest,
	WordAutocompleteResponse,
	WordResponse,
} from '$lib/api/types.gen';

function requireData<T>(data: T | undefined, label: string): T {
	if (data === undefined) throw new Error(`${label}: empty response`);
	return data;
}

export type WordFilters = {
	q?: string;
	dialect?: Dialect;
	difficulty?: Difficulty;
	partOfSpeech?: PartOfSpeech;
	masteryLevel?: MasteryLevel;
	page?: number;
	size?: number;
};

export function useWords(filters: () => WordFilters) {
	return createQuery(() => ({
		queryKey: ['words', 'all', filters()],
		queryFn: async () => {
			const f = filters();
			const { data, error } = await listWords({ query: { ...f } });
			if (error) throw error;
			const result = requireData(data, 'listWords');
			return {
				items: (result.items ?? []) as WordResponse[],
				total: result.total,
				page: result.page,
				size: result.size,
			};
		},
	}));
}

export function useWord(id: () => string | undefined) {
	return createQuery(() => ({
		queryKey: ['words', id()],
		queryFn: async () => {
			const resolvedId = id();
			if (!resolvedId) throw new Error('Missing word id');
			const { data, error } = await getWordById({ path: { id: resolvedId } });
			if (error) throw error;
			return requireData(data, 'getWordById') as WordResponse;
		},
		enabled: !!id(),
	}));
}

export function useWordAnnotations(id: () => string | undefined) {
	return createQuery(() => ({
		queryKey: ['words', id(), 'annotations'],
		queryFn: async () => {
			const resolvedId = id();
			if (!resolvedId) throw new Error('Missing word id');
			const { data, error } = await getAnnotationsForWord({ path: { wordId: resolvedId } });
			if (error) throw error;
			return requireData(data, 'getAnnotationsForWord') as AnnotationResponse[];
		},
		enabled: !!id(),
	}));
}

export function useDictionaryLinks(id: () => string | undefined) {
	return createQuery(() => ({
		queryKey: ['words', id(), 'dictionary-links'],
		queryFn: async () => {
			const resolvedId = id();
			if (!resolvedId) throw new Error('Missing word id');
			const { data, error } = await listDictionaryLinks({ path: { id: resolvedId } });
			if (error) throw error;
			return requireData(data, 'listDictionaryLinks') as DictionaryLinkResponse[];
		},
		enabled: !!id(),
	}));
}

export function useWordAutocomplete(q: () => string) {
	return createQuery(() => ({
		queryKey: ['words', 'autocomplete', q()],
		queryFn: async () => {
			const { data, error } = await autocompleteWords({ query: { q: q() } });
			if (error) throw error;
			return requireData(data, 'autocompleteWords') as WordAutocompleteResponse[];
		},
		enabled: q().length >= 2,
	}));
}

export function useCreateWord() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async (req: CreateWordRequest) => {
			const { data, error } = await createWord({ body: req });
			if (error) throw error;
			return requireData(data, 'createWord') as WordResponse;
		},
		onSuccess: () => {
			qc.invalidateQueries({ queryKey: ['words'] });
		},
	}));
}

export function useUpdateWord() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({ id, body }: { id: string; body: UpdateWordRequest }) => {
			const { data, error } = await updateWord({ path: { id }, body });
			if (error) throw error;
			return requireData(data, 'updateWord') as WordResponse;
		},
		onSuccess: (_data, variables) => {
			qc.invalidateQueries({ queryKey: ['words', variables.id] });
			qc.invalidateQueries({ queryKey: ['words', 'all'] });
		},
	}));
}

export function useDeleteWord() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async (id: string) => {
			const { error } = await deleteWord({ path: { id } });
			if (error) throw error;
		},
		onSuccess: () => {
			qc.invalidateQueries({ queryKey: ['words'] });
		},
	}));
}

export function useAddDictionaryLink() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({ id, body }: { id: string; body: CreateDictionaryLinkRequest }) => {
			const { data, error } = await addDictionaryLink({ path: { id }, body });
			if (error) throw error;
			return requireData(data, 'addDictionaryLink') as DictionaryLinkResponse;
		},
		onSuccess: (_data, variables) => {
			qc.invalidateQueries({ queryKey: ['words', variables.id, 'dictionary-links'] });
		},
	}));
}

export function useDeleteDictionaryLink() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({ id, linkId }: { id: string; linkId: string }) => {
			const { error } = await deleteDictionaryLink({ path: { id, linkId } });
			if (error) throw error;
		},
		onSuccess: (_data, variables) => {
			qc.invalidateQueries({ queryKey: ['words', variables.id, 'dictionary-links'] });
		},
	}));
}

export function useGenerateExamples() {
	return createMutation(() => ({
		mutationFn: async (id: string) => {
			const { data, error } = await generateWordExamples({ path: { id } });
			if (error) throw error;
			return requireData(data, 'generateWordExamples') as ExamplesResponse;
		},
	}));
}
