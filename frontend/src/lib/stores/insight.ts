import { createMutation } from '@tanstack/svelte-query';
import { generateInsight } from '$lib/api/sdk.gen';
import type { InsightRequest, InsightResponse } from '$lib/api/types.gen';

export function useInsight() {
	return createMutation(() => ({
		mutationFn: async (req: InsightRequest): Promise<InsightResponse> => {
			const { data, error } = await generateInsight({ body: req });
			if (error) throw error;
			if (!data) throw new Error('Empty response');
			return data as InsightResponse;
		},
	}));
}
