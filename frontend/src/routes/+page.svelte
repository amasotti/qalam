<script lang="ts">
import { Badge } from '$lib/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
import { createQuery } from '@tanstack/svelte-query';

const health = createQuery(() => ({
	queryKey: ['health'],
	queryFn: async () => {
		const res = await fetch('/health');
		if (!res.ok) throw new Error(`${res.status}`);
		return (await res.json()) as Promise<{ status: string }>;
	},
	retry: false,
}));
</script>

<div class="page-home">
	<header class="page-home-header">
		<h1 class="arabic-display">قلم</h1>
		<p class="page-home-subtitle">Arabic learning — texts, vocabulary, roots, training</p>
	</header>

	<Card>
		<CardHeader>
			<CardTitle>Backend</CardTitle>
		</CardHeader>
		<CardContent>
			{#if health.isPending}
				<Badge variant="secondary">Checking…</Badge>
			{:else if health.isError}
				<Badge variant="destructive">Unreachable</Badge>
				<p class="page-home-status-detail">Could not reach the backend. Is it running?</p>
			{:else}
				<Badge>Connected</Badge>
				<p class="page-home-status-detail">Status: {health.data?.status}</p>
			{/if}
		</CardContent>
	</Card>
</div>
