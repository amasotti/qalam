<script lang="ts">
	import { ChevronDown } from 'lucide-svelte';
	import type { TrainingSessionWordResponse } from '$lib/api/types.gen';
	import Button from '$lib/components/ui/button/button.svelte';

	interface Props {
		word: TrainingSessionWordResponse;
		isPending?: boolean;
	}

	let { word, isPending = false }: Props = $props();

	let revealed = $state(false);

	const front = $derived(
		word.frontSide === 'ARABIC'
			? word.arabicText
			: word.translation || word.arabicText
	);

	const back = $derived(
		word.frontSide === 'ARABIC'
			? word.translation || ''
			: word.arabicText
	);

	const backIsArabic = $derived(word.frontSide === 'TRANSLATION');

	function handleReveal() {
		revealed = true;
	}

	function handleResult(result: 'CORRECT' | 'INCORRECT' | 'SKIPPED') {
		revealed = false;
		// Dispatch event up to parent component
		const event = new CustomEvent('result', {
			detail: { result, wordId: word.wordId }
		});
		window.dispatchEvent(event);
	}
</script>

<div class="flashcard-container">
	<div class="flashcard">
		{#if !revealed}
			<!-- Front side -->
			<div class="flashcard-front">
				<div class="flashcard-content">
					{#if word.frontSide === 'ARABIC'}
						<div class="arabic arabic-display">
							{front}
						</div>
						{#if word.transliteration}
							<div class="transliteration">
								{word.transliteration}
							</div>
						{/if}
					{:else}
						<div class="translation-front">
							{front}
						</div>
					{/if}
				</div>

				<Button
					onclick={handleReveal}
					disabled={isPending}
					size="lg"
				>
					<ChevronDown size={20} />
					Reveal
				</Button>
			</div>
		{:else}
			<!-- Back side -->
			<div class="flashcard-back">
				<div class="flashcard-content">
					{#if backIsArabic}
						<div class="arabic arabic-display">
							{back}
						</div>
						{#if word.transliteration}
							<div class="transliteration">
								{word.transliteration}
							</div>
						{/if}
					{:else}
						<div class="translation-back">
							{back}
						</div>
					{/if}
				</div>

				<div class="flashcard-actions">
					<Button
						onclick={() => handleResult('CORRECT')}
						disabled={isPending}
						size="lg"
						variant="default"
					>
						Correct
					</Button>
					<Button
						onclick={() => handleResult('INCORRECT')}
						disabled={isPending}
						size="lg"
						variant="outline"
					>
						Incorrect
					</Button>
					<Button
						onclick={() => handleResult('SKIPPED')}
						disabled={isPending}
						size="lg"
						variant="outline"
					>
						Skip
					</Button>
				</div>
			</div>
		{/if}
	</div>
</div>

<style>
	.flashcard-container {
		display: flex;
		justify-content: center;
		align-items: center;
		min-height: 100vh;
		padding: 1rem;
	}

	.flashcard {
		width: 100%;
		max-width: 600px;
		aspect-ratio: 3 / 4;
		background: hsl(var(--background));
		border: 2px solid hsl(var(--border));
		border-radius: 12px;
		padding: 3rem 2rem;
		display: flex;
		flex-direction: column;
		justify-content: center;
		align-items: center;
		gap: 3rem;
		box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
	}

	.flashcard-front,
	.flashcard-back {
		display: flex;
		flex-direction: column;
		gap: 2rem;
		width: 100%;
		align-items: center;
	}

	.flashcard-content {
		width: 100%;
		text-align: center;
		flex: 1;
		display: flex;
		flex-direction: column;
		justify-content: center;
		gap: 1rem;
	}

	.transliteration {
		font-style: italic;
		color: hsl(var(--muted-foreground));
		font-size: 0.95em;
		letter-spacing: 0.02em;
	}

	.translation-front,
	.translation-back {
		font-size: 1.5rem;
		font-weight: 500;
		color: hsl(var(--foreground));
		line-height: 1.5;
	}

	.flashcard-actions {
		display: flex;
		gap: 1rem;
		width: 100%;
		justify-content: center;
		flex-wrap: wrap;
	}

	.flashcard-actions :global(button) {
		flex: 1;
		min-width: 120px;
	}
</style>
