<script lang="ts">
import type { AlignmentTokenResponse, AnnotationResponse } from '$lib/api/types.gen';
import AnnotationBadge from '$lib/components/annotations/AnnotationBadge.svelte';

interface Props {
	tokens: AlignmentTokenResponse[];
	annotations?: AnnotationResponse[];
	onTokenClick?: (anchor: string) => void;
}

let { tokens, annotations = [], onTokenClick }: Props = $props();

function badgesFor(arabicText: string): AnnotationResponse[] {
	return annotations.filter((a) => a.anchor === arabicText);
}
</script>

{#if tokens.length > 0}
	<div class="token-grid-wrap">
		{#each tokens as token (token.id)}
			{@const tokenBadges = badgesFor(token.arabic)}
			<div
				class="token-cell"
				class:token-cell-clickable={!!onTokenClick}
				role={onTokenClick ? 'button' : undefined}
				tabindex={onTokenClick ? 0 : undefined}
				onclick={() => onTokenClick?.(token.arabic)}
				onkeydown={(e) => e.key === 'Enter' && onTokenClick?.(token.arabic)}
			>
				<span class="token-ar">{token.arabic}</span>
				{#if token.transliteration}
					<span class="token-tr">{token.transliteration}</span>
				{/if}
				{#if token.translation}
					<span class="token-gloss">{token.translation}</span>
				{/if}
				{#if tokenBadges.length > 0}
					<div style="display:flex;gap:0.125rem;margin-top:0.125rem;justify-content:center;">
						{#each tokenBadges as ann (ann.id)}
							<AnnotationBadge type={ann.type} />
						{/each}
					</div>
				{/if}
			</div>
		{/each}
	</div>
{/if}

<style>
/* Token grid styles are in layout.css via .token-grid-wrap and .token-cell */
</style>
