import { createMutation, createQuery, useQueryClient } from '@tanstack/svelte-query';
import {
	addDictionaryLink,
	addWordPlural,
	addWordRelation,
	analyzeWord,
	autocompleteWords,
	createWord,
	deleteDictionaryLink,
	deleteWord,
	deleteWordExample,
	deleteWordPlural,
	deleteWordRelation,
	enrichWord,
	generateWordExamples,
	getAnnotationsForWord,
	getWordByArabic,
	getWordById,
	getWordMorphology,
	getWordPlurals,
	getWordRelations,
	listDictionaryLinks,
	listWordExamples,
	listWords,
	saveWordExample,
	updateWord,
	upsertWordMorphology,
} from '$lib/api/sdk.gen';
import type {
	AiExamplesResponse,
	AnnotationResponse,
	CreateDictionaryLinkRequest,
	CreateWordExampleRequest,
	CreateWordPluralRequest,
	CreateWordRelationRequest,
	CreateWordRequest,
	Dialect,
	DictionaryLinkResponse,
	Difficulty,
	MasteryLevel,
	PartOfSpeech,
	UpdateWordRequest,
	UpsertWordMorphologyRequest,
	WordAnalysisResponse,
	WordAutocompleteResponse,
	WordEnrichmentSuggestion,
	WordExampleResponse,
	WordMorphologyResponse,
	WordPluralResponse,
	WordRelationResponse,
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

export function useWordExamples(id: () => string | undefined) {
	return createQuery(() => ({
		queryKey: ['words', id(), 'examples'],
		queryFn: async () => {
			const resolvedId = id();
			if (!resolvedId) throw new Error('Missing word id');
			const { data, error } = await listWordExamples({ path: { id: resolvedId } });
			if (error) throw error;
			return requireData(data, 'listWordExamples') as WordExampleResponse[];
		},
		enabled: !!id(),
	}));
}

export function useSaveWordExample() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({ id, body }: { id: string; body: CreateWordExampleRequest }) => {
			const { data, error } = await saveWordExample({ path: { id }, body });
			if (error) throw error;
			return requireData(data, 'saveWordExample') as WordExampleResponse;
		},
		onSuccess: (_data, variables) => {
			qc.invalidateQueries({ queryKey: ['words', variables.id, 'examples'] });
		},
	}));
}

export function useDeleteWordExample() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({ id, exampleId }: { id: string; exampleId: string }) => {
			const { error } = await deleteWordExample({ path: { id, exampleId } });
			if (error) throw error;
		},
		onSuccess: (_data, variables) => {
			qc.invalidateQueries({ queryKey: ['words', variables.id, 'examples'] });
		},
	}));
}

export function useGenerateExamples() {
	return createMutation(() => ({
		mutationFn: async (id: string) => {
			const { data, error } = await generateWordExamples({ path: { id } });
			if (error) throw error;
			return requireData(data, 'generateWordExamples') as AiExamplesResponse;
		},
	}));
}

export function useLookupWordByArabic() {
	return createMutation(() => ({
		mutationFn: async (arabicText: string): Promise<WordResponse | null> => {
			const { data, error, response } = await getWordByArabic({ query: { q: arabicText } });
			if (response.status === 404) return null;
			if (error) throw error;
			return requireData(data, 'getWordByArabic') as WordResponse;
		},
	}));
}

export function useAnalyzeWord() {
	return createMutation(() => ({
		mutationFn: async (arabicText: string): Promise<WordAnalysisResponse> => {
			const { data, error } = await analyzeWord({ body: { arabicText } });
			if (error) throw error;
			return requireData(data, 'analyzeWord') as WordAnalysisResponse;
		},
	}));
}

// --- Word enrichment: morphology ---

export function useMorphology(id: () => string | undefined) {
	return createQuery(() => ({
		queryKey: ['words', id(), 'morphology'],
		queryFn: async () => {
			const resolvedId = id();
			if (!resolvedId) throw new Error('Missing word id');
			const { data, error } = await getWordMorphology({ path: { id: resolvedId } });
			if (error) throw error;
			return requireData(data, 'getWordMorphology') as WordMorphologyResponse;
		},
		enabled: !!id(),
	}));
}

export function useUpsertMorphology() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({ id, body }: { id: string; body: UpsertWordMorphologyRequest }) => {
			const { data, error } = await upsertWordMorphology({ path: { id }, body });
			if (error) throw error;
			return requireData(data, 'upsertWordMorphology') as WordMorphologyResponse;
		},
		onSuccess: (_data: WordMorphologyResponse, variables: { id: string; body: UpsertWordMorphologyRequest }) => {
			qc.invalidateQueries({ queryKey: ['words', variables.id, 'morphology'] });
		},
	}));
}

// --- Word enrichment: plurals ---

export function useWordPlurals(id: () => string | undefined) {
	return createQuery(() => ({
		queryKey: ['words', id(), 'plurals'],
		queryFn: async () => {
			const resolvedId = id();
			if (!resolvedId) throw new Error('Missing word id');
			const { data, error } = await getWordPlurals({ path: { id: resolvedId } });
			if (error) throw error;
			return requireData(data, 'getWordPlurals') as WordPluralResponse[];
		},
		enabled: !!id(),
	}));
}

export function useAddWordPlural() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({ id, body }: { id: string; body: CreateWordPluralRequest }) => {
			const { data, error } = await addWordPlural({ path: { id }, body });
			if (error) throw error;
			return requireData(data, 'addWordPlural') as WordPluralResponse;
		},
		onSuccess: (_data: WordPluralResponse, variables: { id: string; body: CreateWordPluralRequest }) => {
			qc.invalidateQueries({ queryKey: ['words', variables.id, 'plurals'] });
		},
	}));
}

export function useDeleteWordPlural() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({ id, pluralId }: { id: string; pluralId: string }) => {
			const { error } = await deleteWordPlural({ path: { id, pluralId } });
			if (error) throw error;
		},
		onSuccess: (_data: void, variables: { id: string; pluralId: string }) => {
			qc.invalidateQueries({ queryKey: ['words', variables.id, 'plurals'] });
		},
	}));
}

// --- Word enrichment: relations ---

export function useWordRelations(id: () => string | undefined) {
	return createQuery(() => ({
		queryKey: ['words', id(), 'relations'],
		queryFn: async () => {
			const resolvedId = id();
			if (!resolvedId) throw new Error('Missing word id');
			const { data, error } = await getWordRelations({ path: { id: resolvedId } });
			if (error) throw error;
			return requireData(data, 'getWordRelations') as WordRelationResponse[];
		},
		enabled: !!id(),
	}));
}

export function useAddWordRelation() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({ id, body }: { id: string; body: CreateWordRelationRequest }) => {
			const { data, error } = await addWordRelation({ path: { id }, body });
			if (error) throw error;
			return requireData(data, 'addWordRelation') as WordRelationResponse;
		},
		onSuccess: (_data: WordRelationResponse, variables: { id: string; body: CreateWordRelationRequest }) => {
			qc.invalidateQueries({ queryKey: ['words', variables.id, 'relations'] });
		},
	}));
}

export function useDeleteWordRelation() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({ id, relatedWordId, type }: { id: string; relatedWordId: string; type: 'SYNONYM' | 'ANTONYM' | 'RELATED' }) => {
			const { error } = await deleteWordRelation({ path: { id, relatedWordId, type } });
			if (error) throw error;
		},
		onSuccess: (_data: void, variables: { id: string; relatedWordId: string; type: 'SYNONYM' | 'ANTONYM' | 'RELATED' }) => {
			qc.invalidateQueries({ queryKey: ['words', variables.id, 'relations'] });
		},
	}));
}

// --- Word enrichment: AI suggestions ---

export function useEnrichWord() {
	return createMutation(() => ({
		mutationFn: async (id: string): Promise<WordEnrichmentSuggestion> => {
			const { data, error } = await enrichWord({ path: { id } });
			if (error) throw error;
			return requireData(data, 'enrichWord') as WordEnrichmentSuggestion;
		},
		// No cache invalidation — ephemeral preview only
	}));
}
