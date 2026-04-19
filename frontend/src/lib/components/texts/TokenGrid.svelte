<script lang="ts">
import type { AlignmentTokenResponse } from '$lib/api/types.gen';

interface Props {
	tokens: AlignmentTokenResponse[];
}

let { tokens }: Props = $props();
</script>

{#if tokens.length > 0}
	<div class="token-grid">
		<div class="token-row">
			{#each tokens as token (token.id)}
				<dl class="token-cell">
					<dt class="token-arabic arabic-text">{token.arabic}</dt>
					{#if token.transliteration}
						<dd class="token-translit transliteration">{token.transliteration}</dd>
					{/if}
					{#if token.translation}
						<dd class="token-translation">{token.translation}</dd>
					{/if}
				</dl>
			{/each}
		</div>
	</div>
{/if}

<style>
.token-grid {
	padding: 0.25rem 0;
}

.token-row {
	display: flex;
	flex-wrap: wrap;
	direction: rtl;
	gap: 0;
}

.token-cell {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 0.25rem 0.625rem;
	border-inline-end: 1px solid hsl(var(--border) / 0.5);
	min-width: 4rem;
	margin: 0;
}

.token-arabic {
	font-size: 1.1rem;
	line-height: 1.8;
}

.token-translit {
	font-size: 0.8125rem;
	margin: 0;
}

.token-translation {
	font-size: 0.75rem;
	color: hsl(var(--foreground) / 0.7);
	text-align: center;
	margin: 0;
}
</style>
