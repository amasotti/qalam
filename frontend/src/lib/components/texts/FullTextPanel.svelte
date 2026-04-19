<script lang="ts">
import type { TextResponse } from '$lib/api/types.gen';

interface Props {
	text: TextResponse;
}

let { text }: Props = $props();
</script>

{#if text.body || text.transliteration}
	<div class="full-text-panel">
		<h2 class="full-text-heading">Full text</h2>

		{#if text.body}
			<div class="full-text-arabic arabic-text">{text.body}</div>
		{/if}

		{#if text.transliteration}
			<div class="full-text-transliteration transliteration">{text.transliteration}</div>
		{/if}

		{#if text.translation}
			<div class="full-text-translation">{text.translation}</div>
		{/if}
	</div>
{/if}

<style>
.full-text-panel {
	margin-top: 2.5rem;
	padding-top: 2rem;
	border-top: 1px solid hsl(var(--border));
}

.full-text-heading {
	font-size: 0.8125rem;
	font-weight: 600;
	text-transform: uppercase;
	letter-spacing: 0.06em;
	color: hsl(var(--muted-foreground));
	margin-bottom: 1rem;
}

.full-text-arabic {
	font-size: 1.25rem;
	line-height: 2.2;
	margin-bottom: 1rem;
}

.full-text-transliteration {
	font-size: 0.9375rem;
	line-height: 1.8;
	margin-bottom: 0.75rem;
}

.full-text-translation {
	font-size: 0.9375rem;
	color: hsl(var(--muted-foreground));
	line-height: 1.7;
}
</style>
