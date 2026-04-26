import { createQuery } from '@tanstack/svelte-query';
import { getAnalyticsOverview } from '$lib/api/sdk.gen';
import type { AnalyticsOverviewResponse } from '$lib/api/types.gen';

function requireData<T>(data: T | undefined, label: string): T {
	if (data === undefined) throw new Error(`${label}: empty response`);
	return data;
}

export function useAnalytics() {
	return createQuery(() => ({
		queryKey: ['analytics', 'overview'],
		queryFn: async () => {
			const { data, error } = await getAnalyticsOverview({});
			if (error) throw error;
			return requireData(data, 'getAnalyticsOverview') as AnalyticsOverviewResponse;
		},
		staleTime: 60_000,
	}));
}
