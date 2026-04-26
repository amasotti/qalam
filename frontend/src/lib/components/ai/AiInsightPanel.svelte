<script lang="ts">
import { marked } from 'marked';
import type { InsightRequest } from '$lib/api/types.gen';
import { useInsight } from '$lib/stores/insight';

interface Props {
	entityType: 'WORD' | 'SENTENCE';
	entityId: string;
	mode?: 'HOMEWORK' | 'READING';
}

let { entityType, entityId, mode }: Props = $props();

const insight = useInsight();

type State = 'idle' | 'loading' | 'result';
let uiState: State = $state('idle');
let renderedHtml = $state('');
let aiUnavailable = $state(false);

async function fetchInsight() {
	uiState = 'loading';
	try {
		const req: InsightRequest = { entityType, entityId };
		if (mode !== undefined) req.mode = mode;
		const result = await insight.mutateAsync(req);
		renderedHtml = await marked(result.insight);
		uiState = 'result';
	} catch (err: unknown) {
		const status = (err as { status?: number })?.status;
		if (status === 503) {
			aiUnavailable = true;
			uiState = 'idle';
		} else {
			uiState = 'idle';
		}
	}
}

function reset() {
	uiState = 'idle';
	renderedHtml = '';
}
</script>

{#if !aiUnavailable}
	{#if uiState === 'idle'}
		<button class="btn ai-insight-btn" onclick={fetchInsight}>Get insight</button>
	{:else if uiState === 'loading'}
		<span class="ai-insight-loading">Loading…</span>
	{:else if uiState === 'result'}
		<div class="ai-insight-panel">
			<div class="ai-insight-content">{@html renderedHtml}</div>
			<div class="ai-insight-actions">
				<button class="btn" onclick={fetchInsight} disabled={insight.isPending}>Refresh</button>
				<button class="btn" onclick={reset}>Close</button>
			</div>
		</div>
	{/if}
{/if}
