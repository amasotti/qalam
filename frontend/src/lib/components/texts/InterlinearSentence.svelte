<script lang="ts">
import type { AnnotationResponse, SentenceResponse } from '$lib/api/types.gen';
import StaleTokenBanner from './StaleTokenBanner.svelte';
import TokenGrid from './TokenGrid.svelte';

interface Props {
	sentence: SentenceResponse;
	annotations?: AnnotationResponse[];
	onRetokenize?: (sentence: SentenceResponse) => Promise<void>;
	onMarkValid?: (sentence: SentenceResponse) => Promise<void>;
	onTokenClick?: (anchor: string) => void;
	isPending?: boolean;
}

let {
	sentence,
	annotations = [],
	onRetokenize,
	onMarkValid,
	onTokenClick,
	isPending = false,
}: Props = $props();

const showStaleBanner = $derived(!sentence.tokensValid && sentence.tokens.length > 0);
</script>

<div class="interlinear-sentence">
	<div class="sentence-number">{sentence.position}</div>

	<div class="sentence-body">
		<div class="sentence-arabic arabic-text">{sentence.arabicText}</div>

		{#if sentence.transliteration}
			<div class="sentence-transliteration transliteration">{sentence.transliteration}</div>
		{/if}

		{#if showStaleBanner && onRetokenize && onMarkValid}
			<StaleTokenBanner
				onRetokenize={() => onRetokenize!(sentence)}
				onMarkValid={() => onMarkValid!(sentence)}
				{isPending}
			/>
		{/if}

		{#if sentence.tokens.length > 0}
			<div class="sentence-tokens">
				<TokenGrid tokens={sentence.tokens} {annotations} {onTokenClick} />
			</div>
		{/if}

		{#if sentence.freeTranslation}
			<div class="sentence-free-translation">
				<span class="sentence-free-label">Trans.</span>
				{sentence.freeTranslation}
			</div>
		{/if}

		{#if sentence.notes}
			<div class="sentence-notes">
				<span class="sentence-notes-label">Notes.</span>
				{sentence.notes}
			</div>
		{/if}
	</div>
</div>

<style>
.interlinear-sentence {
	display: flex;
	gap: 0.75rem;
	padding: 1rem 0;
	border-bottom: 1px solid hsl(var(--border) / 0.5);
}

.interlinear-sentence:last-child {
	border-bottom: none;
}

.sentence-number {
	flex-shrink: 0;
	width: 1.5rem;
	font-size: 0.75rem;
	color: hsl(var(--muted-foreground));
	padding-top: 0.25rem;
	text-align: right;
}

.sentence-body {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 0.5rem;
}

.sentence-arabic {
	font-size: 1.25rem;
	line-height: 1.8;
}

.sentence-transliteration {
	font-size: 0.9375rem;
}

.sentence-tokens {
	border: 1px solid hsl(var(--border) / 0.6);
	border-radius: 0.375rem;
	overflow: hidden;
	background: hsl(var(--muted) / 0.3);
}

.sentence-free-translation {
	font-size: 0.875rem;
	color: hsl(var(--foreground) / 0.85);
	line-height: 1.6;
}

.sentence-free-label,
.sentence-notes-label {
	font-size: 0.75rem;
	font-weight: 600;
	text-transform: uppercase;
	letter-spacing: 0.05em;
	color: hsl(var(--muted-foreground));
	margin-right: 0.375rem;
}

.sentence-notes {
	font-size: 0.8125rem;
	color: hsl(var(--muted-foreground));
	line-height: 1.6;
	font-style: italic;
}
</style>
