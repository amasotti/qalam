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
	<div class="token-grid">
		<div class="token-row">
			{#each tokens as token (token.id)}
				{@const tokenBadges = badgesFor(token.arabic)}
				<dl
					class="token-cell"
					class:token-cell-clickable={!!onTokenClick}
					class:token-cell-annotated={tokenBadges.length > 0}
					role={onTokenClick ? 'button' : undefined}
					tabindex={onTokenClick ? 0 : undefined}
					onclick={() => onTokenClick?.(token.arabic)}
					onkeydown={(e) => e.key === 'Enter' && onTokenClick?.(token.arabic)}
				>
					<dt class="token-arabic arabic-text">{token.arabic}</dt>
					{#if token.transliteration}
						<dd class="token-translit transliteration">{token.transliteration}</dd>
					{/if}
					{#if token.translation}
						<dd class="token-translation">{token.translation}</dd>
					{/if}
					{#if tokenBadges.length > 0}
						<dd class="token-badges">
							{#each tokenBadges as ann (ann.id)}
								<AnnotationBadge type={ann.type} />
							{/each}
						</dd>
					{/if}
				</dl>
			{/each}
		</div>
	</div>
{/if}

<style>
.token-grid { padding: 0.25rem 0; }

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

.token-cell-clickable { cursor: pointer; }
.token-cell-clickable:hover { background: hsl(var(--muted) / 0.5); }
.token-cell-clickable:focus-visible { outline: 2px solid hsl(var(--primary) / 0.6); outline-offset: -2px; }

.token-cell-annotated { position: relative; }

.token-arabic { font-size: 1.1rem; line-height: 1.8; }
.token-translit { font-size: 0.8125rem; margin: 0; }
.token-translation { font-size: 0.75rem; color: hsl(var(--foreground) / 0.7); text-align: center; margin: 0; }

.token-badges {
	display: flex;
	gap: 0.125rem;
	margin: 0.125rem 0 0;
	justify-content: center;
}
</style>
