import { createMutation, createQuery } from '@tanstack/svelte-query';
import { getProductionPracticePrompt, reviewProductionPracticeSentence } from '$lib/api/sdk.gen';
import type {
	ProductionPracticePromptResponse,
	ProductionPracticeReviewResponse,
	ReviewProductionPracticeRequest,
} from '$lib/api/types.gen';

function requireData<T>(data: T | undefined, label: string): T {
	if (data === undefined) throw new Error(`${label}: empty response`);
	return data;
}

export function useProductionPracticePrompt() {
	return createQuery(() => ({
		queryKey: ['production-practice', 'prompt'],
		refetchOnWindowFocus: false,
		queryFn: async () => {
			const { data, error } = await getProductionPracticePrompt();
			if (error) throw error;
			return requireData(data, 'getProductionPracticePrompt') as ProductionPracticePromptResponse;
		},
	}));
}

export function useReviewProductionPractice() {
	return createMutation(() => ({
		mutationFn: async (request: ReviewProductionPracticeRequest) => {
			const { data, error } = await reviewProductionPracticeSentence({ body: request });
			if (error) throw error;
			return requireData(
				data,
				'reviewProductionPracticeSentence'
			) as ProductionPracticeReviewResponse;
		},
	}));
}
