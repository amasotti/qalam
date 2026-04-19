import { createMutation, createQuery, useQueryClient } from '@tanstack/svelte-query';
import {
	autoTokenize,
	clearTokens,
	createSentence,
	createText,
	deleteSentence,
	deleteText,
	getTextById,
	listSentences,
	listTexts,
	replaceTokens,
	transliterateSentence,
	updateSentence,
	updateText,
} from '$lib/api/sdk.gen';
import type {
	AlignmentTokenResponse,
	CreateSentenceRequest,
	CreateTextRequest,
	Dialect,
	Difficulty,
	ReplaceTokensRequest,
	SentenceResponse,
	TextResponse,
	UpdateSentenceRequest,
	UpdateTextRequest,
} from '$lib/api/types.gen';

function requireData<T>(data: T | undefined, label: string): T {
	if (data === undefined) throw new Error(`${label}: empty response`);
	return data;
}

export type TextFilters = {
	q?: string;
	dialect?: Dialect;
	difficulty?: Difficulty;
	tag?: string;
	page?: number;
	size?: number;
};

export function useTexts(filters: () => TextFilters) {
	return createQuery(() => ({
		queryKey: ['texts', 'all', filters()],
		queryFn: async () => {
			const f = filters();
			const { data, error } = await listTexts({ query: { ...f } });
			if (error) throw error;
			const result = requireData(data, 'listTexts');
			return {
				items: (result.items ?? []) as TextResponse[],
				total: result.total,
				page: result.page,
				size: result.size,
			};
		},
	}));
}

export function useText(id: () => string | undefined) {
	return createQuery(() => ({
		queryKey: ['texts', id()],
		queryFn: async () => {
			const resolvedId = id();
			if (!resolvedId) throw new Error('Missing text id');
			const { data, error } = await getTextById({ path: { id: resolvedId } });
			if (error) throw error;
			return requireData(data, 'getTextById') as TextResponse;
		},
		enabled: !!id(),
	}));
}

export function useSentences(textId: () => string | undefined) {
	return createQuery(() => ({
		queryKey: ['texts', textId(), 'sentences'],
		queryFn: async () => {
			const resolvedId = textId();
			if (!resolvedId) throw new Error('Missing text id');
			const { data, error } = await listSentences({ path: { textId: resolvedId } });
			if (error) throw error;
			return requireData(data, 'listSentences') as SentenceResponse[];
		},
		enabled: !!textId(),
	}));
}

export function useCreateText() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async (req: CreateTextRequest) => {
			const { data, error } = await createText({ body: req });
			if (error) throw error;
			return requireData(data, 'createText') as TextResponse;
		},
		onSuccess: () => {
			qc.invalidateQueries({ queryKey: ['texts', 'all'] });
		},
	}));
}

export function useUpdateText() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({ id, body }: { id: string; body: UpdateTextRequest }) => {
			const { data, error } = await updateText({ path: { id }, body });
			if (error) throw error;
			return requireData(data, 'updateText') as TextResponse;
		},
		onSuccess: (_data, variables) => {
			qc.invalidateQueries({ queryKey: ['texts', variables.id] });
			qc.invalidateQueries({ queryKey: ['texts', 'all'] });
		},
	}));
}

export function useDeleteText() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async (id: string) => {
			const { error } = await deleteText({ path: { id } });
			if (error) throw error;
		},
		onSuccess: () => {
			qc.invalidateQueries({ queryKey: ['texts'] });
		},
	}));
}

export function useCreateSentence() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({ textId, body }: { textId: string; body: CreateSentenceRequest }) => {
			const { data, error } = await createSentence({ path: { textId }, body });
			if (error) throw error;
			return requireData(data, 'createSentence') as SentenceResponse;
		},
		onSuccess: (_data, variables) => {
			qc.invalidateQueries({ queryKey: ['texts', variables.textId, 'sentences'] });
		},
	}));
}

export function useUpdateSentence() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({
			textId,
			id,
			body,
		}: {
			textId: string;
			id: string;
			body: UpdateSentenceRequest;
		}) => {
			const { data, error } = await updateSentence({ path: { textId, id }, body });
			if (error) throw error;
			return requireData(data, 'updateSentence') as SentenceResponse;
		},
		onSuccess: (_data, variables) => {
			qc.invalidateQueries({ queryKey: ['texts', variables.textId, 'sentences'] });
		},
	}));
}

export function useDeleteSentence() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({ textId, id }: { textId: string; id: string }) => {
			const { error } = await deleteSentence({ path: { textId, id } });
			if (error) throw error;
		},
		onSuccess: (_data, variables) => {
			qc.invalidateQueries({ queryKey: ['texts', variables.textId, 'sentences'] });
		},
	}));
}

export function useReplaceTokens() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({
			textId,
			id,
			body,
		}: {
			textId: string;
			id: string;
			body: ReplaceTokensRequest;
		}) => {
			const { data, error } = await replaceTokens({ path: { textId, id }, body });
			if (error) throw error;
			return requireData(data, 'replaceTokens') as SentenceResponse;
		},
		onSuccess: (_data, variables) => {
			qc.invalidateQueries({ queryKey: ['texts', variables.textId, 'sentences'] });
		},
	}));
}

export function useClearTokens() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({ textId, id }: { textId: string; id: string }) => {
			const { data, error } = await clearTokens({
				path: { textId, id },
				query: { confirm: 'true' },
			});
			if (error) throw error;
			return requireData(data, 'clearTokens') as SentenceResponse;
		},
		onSuccess: (_data, variables) => {
			qc.invalidateQueries({ queryKey: ['texts', variables.textId, 'sentences'] });
		},
	}));
}

export function useAutoTokenize() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({ textId, id }: { textId: string; id: string }) => {
			const { data, error } = await autoTokenize({ path: { textId, id } });
			if (error) throw error;
			return requireData(data, 'autoTokenize') as SentenceResponse;
		},
		onSuccess: (_data, variables) => {
			qc.invalidateQueries({ queryKey: ['texts', variables.textId, 'sentences'] });
		},
	}));
}

export function useTransliterateSentence() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({ textId, id }: { textId: string; id: string }) => {
			const { data, error } = await transliterateSentence({ path: { textId, id } });
			if (error) throw error;
			return requireData(data, 'transliterateSentence') as SentenceResponse;
		},
		onSuccess: (_data, variables) => {
			qc.invalidateQueries({ queryKey: ['texts', variables.textId, 'sentences'] });
		},
	}));
}

export function useMarkTokensValid() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({
			textId,
			id,
			currentTokens,
		}: {
			textId: string;
			id: string;
			currentTokens: AlignmentTokenResponse[];
		}) => {
			const body: ReplaceTokensRequest = {
				tokens: currentTokens.map((t) => ({
					position: t.position,
					arabic: t.arabic,
					transliteration: t.transliteration ?? null,
					translation: t.translation ?? null,
					wordId: t.wordId ?? null,
				})),
			};
			const { data, error } = await replaceTokens({ path: { textId, id }, body });
			if (error) throw error;
			return requireData(data, 'markTokensValid') as SentenceResponse;
		},
		onSuccess: (_data, variables) => {
			qc.invalidateQueries({ queryKey: ['texts', variables.textId, 'sentences'] });
		},
	}));
}
