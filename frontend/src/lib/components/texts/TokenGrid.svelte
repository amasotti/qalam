<script lang="ts">
import type { AlignmentTokenResponse } from '$lib/api/types.gen';

interface Props {
	tokens: AlignmentTokenResponse[];
}

let { tokens }: Props = $props();
</script>

{#if tokens.length > 0}
	<div class="token-grid" role="table" aria-label="Word alignment tokens">
		<div class="token-row" role="row">
			{#each tokens as token (token.id)}
				<div class="token-cell" role="cell">
					<span class="token-arabic arabic">{token.arabic}</span>
					{#if token.transliteration}
						<span class="token-translit transliteration">{token.transliteration}</span>
					{/if}
					{#if token.translation}
						<span class="token-translation">{token.translation}</span>
					{/if}
				</div>
			{/each}
		</div>
	</div>
{/if}

<style>
.token-grid {
	overflow-x: auto;
	padding-bottom: 0.25rem;
}

.token-row {
	display: flex;
	flex-direction: row-reverse;
	gap: 0;
	width: max-content;
}

.token-cell {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 0.25rem 0.625rem;
	border-inline-end: 1px solid hsl(var(--border) / 0.5);
	min-width: 4rem;
}

.token-cell:last-child {
	border-inline-end: none;
}

.token-arabic {
	font-size: 1.1rem;
	line-height: 1.8;
}

.token-translit {
	font-size: 0.8125rem;
}

.token-translation {
	font-size: 0.75rem;
	color: hsl(var(--foreground) / 0.7);
	text-align: center;
}
</style>
