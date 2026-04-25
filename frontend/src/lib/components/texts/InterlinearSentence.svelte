<script lang="ts">
import type {
	AlignmentTokenResponse,
	AnnotationResponse,
	SentenceResponse,
} from '$lib/api/types.gen';
import StaleTokenBanner from './StaleTokenBanner.svelte';
import TokenGrid from './TokenGrid.svelte';

interface Props {
	sentence: SentenceResponse;
	annotations?: AnnotationResponse[];
	onRetokenize?: (sentence: SentenceResponse) => Promise<void>;
	onMarkValid?: (sentence: SentenceResponse) => Promise<void>;
	onTokenClick?: (token: AlignmentTokenResponse) => void;
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

<div class="sentence-block">
	<span class="sentence-num">{sentence.position}</span>
	<div class="sentence-body">
		<div class="sentence-ar">{sentence.arabicText}</div>

		{#if sentence.transliteration}
			<div class="sentence-tr">{sentence.transliteration}</div>
		{/if}

		{#if showStaleBanner && onRetokenize && onMarkValid}
			<StaleTokenBanner
				onRetokenize={() => onRetokenize!(sentence)}
				onMarkValid={() => onMarkValid!(sentence)}
				{isPending}
			/>
		{/if}

		{#if sentence.tokens.length > 0}
			<TokenGrid tokens={sentence.tokens} {annotations} {onTokenClick} />
		{/if}

		{#if sentence.freeTranslation}
			<div class="sentence-translation">
				<span class="sentence-trans-label">Trans.</span>
				{sentence.freeTranslation}
			</div>
		{/if}

		{#if sentence.notes}
			<div class="sentence-notes">
				<span class="sentence-notes-label">Note.</span>
				{sentence.notes}
			</div>
		{/if}
	</div>
</div>
