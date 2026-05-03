<script lang="ts">
import type { AlignmentTokenResponse, AnnotationResponse } from '$lib/api/types.gen';
import AnnotationBadge from '$lib/components/annotations/AnnotationBadge.svelte';

interface Props {
	tokens: AlignmentTokenResponse[];
	annotations?: AnnotationResponse[];
	onTokenClick?: (token: AlignmentTokenResponse) => void;
}

let { tokens, annotations = [], onTokenClick }: Props = $props();

function badgesFor(arabicText: string): AnnotationResponse[] {
	return annotations.filter((a) => a.anchor === arabicText);
}
</script>

{#snippet cellBody(token: AlignmentTokenResponse, badges: AnnotationResponse[])}
	<span class="token-ar">{token.arabic}</span>
	{#if token.transliteration}
		<span class="token-tr">{token.transliteration}</span>
	{/if}
	{#if token.translation}
		<span class="token-gloss">{token.translation}</span>
	{/if}
	{#if badges.length > 0}
		<div class="token-badges">
			{#each badges as ann (ann.id)}
				<AnnotationBadge type={ann.type} />
			{/each}
		</div>
	{/if}
{/snippet}

{#if tokens.length > 0}
	<div class="token-grid-wrap">
		{#each tokens as token (token.id)}
			{@const tokenBadges = badgesFor(token.arabic)}
			{#if onTokenClick}
				<div
					class="token-cell token-cell-clickable"
					role="button"
					tabindex="0"
					onclick={() => onTokenClick(token)}
					onkeydown={(e) => e.key === 'Enter' && onTokenClick(token)}
				>
					{@render cellBody(token, tokenBadges)}
				</div>
			{:else}
				<div class="token-cell">
					{@render cellBody(token, tokenBadges)}
				</div>
			{/if}
		{/each}
	</div>
{/if}
