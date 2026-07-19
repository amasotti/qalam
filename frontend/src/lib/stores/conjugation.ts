import { createMutation, createQuery } from '@tanstack/svelte-query';
import { computeConjugation, conjugateWord } from '$lib/api/sdk.gen';
import type { AdHocConjugationRequest, ConjugationResponse } from '$lib/api/types.gen';

function requireData<T>(data: T | undefined, label: string): T {
	if (data === undefined) throw new Error(`${label}: empty response`);
	return data;
}

export function useConjugation(wordId: () => string | null) {
	return createQuery(() => ({
		queryKey: ['conjugation', 'word', wordId()],
		queryFn: async () => {
			const id = wordId();
			if (!id) throw new Error('No wordId');
			const { data, error } = await conjugateWord({ path: { wordId: id } });
			if (error) throw error;
			return requireData(data, 'conjugateWord') as ConjugationResponse;
		},
		enabled: !!wordId(),
	}));
}

export function useAdHocConjugation() {
	return createMutation(() => ({
		mutationFn: async (req: AdHocConjugationRequest) => {
			const { data, error } = await computeConjugation({ body: req });
			if (error) throw error;
			return requireData(data, 'computeConjugation') as ConjugationResponse;
		},
	}));
}
