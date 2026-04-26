<script lang="ts">
import type { TrainingSessionWordResponse } from '$lib/api/types.gen';

interface Props {
	word: TrainingSessionWordResponse;
	isPending?: boolean;
	currentIndex: number;
	totalWords: number;
	mode: string;
	onresult: (result: 'CORRECT' | 'INCORRECT' | 'SKIPPED') => void;
}

let { word, isPending = false, currentIndex, totalWords, mode, onresult }: Props = $props();

let revealed = $state(false);

const isArabicFront = $derived(word.frontSide === 'ARABIC');
const progress = $derived(totalWords > 0 ? (currentIndex / totalWords) * 100 : 0);

$effect(() => {
	revealed = false;
});

$effect(() => {
	function onKeyDown(e: KeyboardEvent) {
		if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return;
		if (!revealed && (e.code === 'Space' || e.code === 'Enter')) {
			e.preventDefault();
			revealed = true;
		} else if (revealed) {
			if (e.code === 'Digit1' || e.code === 'Numpad1') handleResult('CORRECT');
			else if (e.code === 'Digit2' || e.code === 'Numpad2') handleResult('INCORRECT');
			else if (e.code === 'Digit3' || e.code === 'Numpad3') handleResult('SKIPPED');
		}
	}
	window.addEventListener('keydown', onKeyDown);
	return () => window.removeEventListener('keydown', onKeyDown);
});

function handleResult(result: 'CORRECT' | 'INCORRECT' | 'SKIPPED') {
	if (isPending) return;
	revealed = false;
	onresult(result);
}
</script>

<div class="fc-wrap">
	<!-- Progress -->
	<div class="fc-progress-area">
		<div class="fc-bar-track">
			<div class="fc-bar-fill" style="width: {progress}%"></div>
		</div>
		<div class="fc-meta">
			<span>{currentIndex + 1} / {totalWords}</span>
			<span>{mode}</span>
		</div>
	</div>

	<!-- Card -->
	<div class="fc-card">
		<!-- Question block -->
		{#if revealed}
			<!-- Compact pinned question -->
			<div class="fc-question fc-question-compact">
				<div class="fc-direction">{isArabicFront ? 'Arabic → Translation' : 'Translation → Arabic'}</div>
				<div class="fc-question-row">
					{#if isArabicFront}
						<span class="arabic fc-arabic-compact">{word.arabicText}</span>
						{#if word.transliteration}
							<span class="transliteration">{word.transliteration}</span>
						{/if}
					{:else}
						<span class="fc-translation-compact">{word.translation ?? word.arabicText}</span>
					{/if}
					{#if word.root}
						<span class="fc-root-chip">{word.root}</span>
					{/if}
				</div>
			</div>
		{:else}
			<!-- Full question face -->
			<div class="fc-question">
				<div class="fc-direction">{isArabicFront ? 'Arabic → Translation' : 'Translation → Arabic'}</div>
				{#if isArabicFront}
					<div class="arabic arabic-display fc-arabic-main">{word.arabicText}</div>
					{#if word.transliteration}
						<div class="transliteration fc-center">{word.transliteration}</div>
					{/if}
					{#if word.root}
						<div class="fc-center fc-root-area">
							<span class="fc-root-chip arabic">{word.root}</span>
						</div>
					{/if}
				{:else}
					<div class="fc-translation-main">{word.translation ?? word.arabicText}</div>
				{/if}
			</div>
		{/if}

		<!-- Reveal button (before reveal only) -->
		{#if !revealed}
			<div class="fc-reveal-row">
				<button class="fc-reveal-btn" onclick={() => (revealed = true)} disabled={isPending}>
					▾ Reveal
					<kbd class="fc-kbd">Space</kbd>
				</button>
			</div>
		{/if}

		<!-- Answer + enriched content (after reveal) -->
		{#if revealed}
			<!-- Answer -->
			<div class="fc-section">
				<div class="fc-section-label">Answer</div>
				{#if isArabicFront}
					<div class="fc-answer-text">{word.translation ?? '—'}</div>
				{:else}
					<div class="arabic arabic-display fc-arabic-main">{word.arabicText}</div>
					{#if word.transliteration}
						<div class="transliteration fc-center">{word.transliteration}</div>
					{/if}
				{/if}
			</div>

			<!-- Examples -->
			{#if word.examples && word.examples.length > 0}
				<div class="fc-section fc-section-divider">
					<div class="fc-section-label">Examples</div>
					{#each word.examples as ex}
						<div class="fc-example">
							<div class="arabic-text fc-example-ar">{ex.arabic}</div>
							{#if ex.transliteration}
								<div class="transliteration">{ex.transliteration}</div>
							{/if}
							{#if ex.translation}
								<div class="fc-example-tr">{ex.translation}</div>
							{/if}
						</div>
					{/each}
				</div>
			{/if}

			<!-- Notes -->
			{#if word.notes}
				<div class="fc-note-block">
					<div class="fc-note-label">Note</div>
					<div class="fc-note-text">{word.notes}</div>
				</div>
			{/if}

			<!-- Relations -->
			{#if word.relations && word.relations.length > 0}
				<div class="fc-section fc-section-divider">
					<div class="fc-section-label">Related</div>
					<div class="fc-relations">
						{#each word.relations as rel}
							<span class="fc-rel-chip">
								<span class="arabic">{rel.relatedWordArabic}</span>
								{#if rel.relatedWordTranslation}
									<span class="fc-rel-tr">{rel.relatedWordTranslation}</span>
								{/if}
								<span class="fc-rel-type">{rel.relationType}</span>
							</span>
						{/each}
					</div>
				</div>
			{/if}

			<!-- Actions -->
			<div class="fc-actions">
				<button class="fc-btn fc-btn-correct" onclick={() => handleResult('CORRECT')} disabled={isPending}>
					✓ Correct <kbd class="fc-kbd fc-kbd-dark">1</kbd>
				</button>
				<button class="fc-btn fc-btn-outline" onclick={() => handleResult('INCORRECT')} disabled={isPending}>
					✗ Wrong <kbd class="fc-kbd">2</kbd>
				</button>
				<button class="fc-btn fc-btn-outline" onclick={() => handleResult('SKIPPED')} disabled={isPending}>
					→ Skip <kbd class="fc-kbd">3</kbd>
				</button>
			</div>
		{/if}
	</div>
</div>

<style>
	.fc-wrap {
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 1.5rem 1rem 2rem;
		width: 100%;
	}

	.fc-progress-area {
		width: 100%;
		max-width: 560px;
		margin-bottom: 0.5rem;
	}

	.fc-bar-track {
		height: 3px;
		background: hsl(var(--muted));
		border-radius: 9999px;
		overflow: hidden;
		margin-bottom: 0.375rem;
	}

	.fc-bar-fill {
		height: 3px;
		background: hsl(var(--foreground));
		border-radius: 9999px;
		transition: width 0.3s ease;
	}

	.fc-meta {
		display: flex;
		justify-content: space-between;
		font-size: 0.7rem;
		color: hsl(var(--muted-foreground));
		letter-spacing: 0.04em;
	}

	.fc-card {
		width: 100%;
		max-width: 560px;
		border: 1px solid hsl(var(--border));
		border-radius: 10px;
		background: hsl(var(--background));
		overflow: hidden;
	}

	.fc-question {
		padding: 1.25rem 1.25rem 1rem;
		border-bottom: 1px solid hsl(var(--border));
	}

	.fc-question-compact {
		padding: 0.75rem 1.25rem;
		background: hsl(var(--muted) / 0.4);
	}

	.fc-direction {
		font-size: 0.65rem;
		color: hsl(var(--muted-foreground));
		text-transform: uppercase;
		letter-spacing: 0.06em;
		margin-bottom: 0.5rem;
	}

	.fc-question-row {
		display: flex;
		align-items: center;
		gap: 0.625rem;
		flex-wrap: wrap;
	}

	.fc-arabic-compact {
		font-size: 1.25rem;
	}

	.fc-translation-compact {
		font-size: 0.9rem;
		color: hsl(var(--foreground));
		font-weight: 500;
	}

	.fc-arabic-main {
		text-align: center;
		margin: 0.5rem 0 0.25rem;
	}

	.fc-translation-main {
		font-size: 1.5rem;
		font-weight: 500;
		text-align: center;
		color: hsl(var(--foreground));
		margin: 0.75rem 0;
	}

	.fc-center {
		text-align: center;
	}

	.fc-root-area {
		margin-top: 0.625rem;
	}

	.fc-root-chip {
		background: hsl(var(--muted));
		border-radius: 4px;
		padding: 0.125rem 0.5rem;
		font-size: 0.8rem;
		color: hsl(var(--muted-foreground));
	}

	.fc-reveal-row {
		padding: 0.875rem 1.25rem;
		text-align: center;
	}

	.fc-reveal-btn {
		display: inline-flex;
		align-items: center;
		gap: 0.5rem;
		background: hsl(var(--muted));
		border: none;
		border-radius: 6px;
		padding: 0.5rem 1.25rem;
		font-size: 0.8rem;
		color: hsl(var(--foreground));
		cursor: pointer;
	}

	.fc-reveal-btn:hover:not(:disabled) {
		background: hsl(var(--muted) / 0.8);
	}

	.fc-reveal-btn:disabled {
		opacity: 0.5;
		cursor: not-allowed;
	}

	.fc-kbd {
		font-size: 0.65rem;
		background: hsl(var(--muted));
		color: hsl(var(--muted-foreground));
		padding: 0.1rem 0.35rem;
		border-radius: 3px;
		border: 1px solid hsl(var(--border));
		font-family: inherit;
	}

	.fc-kbd-dark {
		background: hsl(var(--foreground) / 0.15);
		color: hsl(var(--background));
		border-color: transparent;
	}

	.fc-section {
		padding: 0.875rem 1.25rem;
	}

	.fc-section-divider {
		border-top: 1px solid hsl(var(--border));
	}

	.fc-section-label {
		font-size: 0.65rem;
		color: hsl(var(--muted-foreground));
		text-transform: uppercase;
		letter-spacing: 0.06em;
		margin-bottom: 0.375rem;
	}

	.fc-answer-text {
		font-size: 1.25rem;
		font-weight: 600;
		color: hsl(var(--foreground));
	}

	.fc-example {
		margin-bottom: 0.75rem;
	}

	.fc-example:last-child {
		margin-bottom: 0;
	}

	.fc-example-ar {
		font-size: 1rem;
	}

	.fc-example-tr {
		font-size: 0.75rem;
		color: hsl(var(--muted-foreground));
		margin-top: 0.125rem;
	}

	.fc-note-block {
		border-top: 1px solid hsl(var(--border));
		padding: 0.625rem 1.25rem;
		background: hsl(38 92% 95%);
	}

	.fc-note-label {
		font-size: 0.65rem;
		color: hsl(32 95% 35%);
		text-transform: uppercase;
		letter-spacing: 0.06em;
		margin-bottom: 0.2rem;
	}

	.fc-note-text {
		font-size: 0.8rem;
		color: hsl(22 88% 25%);
		line-height: 1.5;
	}

	.fc-relations {
		display: flex;
		flex-wrap: wrap;
		gap: 0.375rem;
	}

	.fc-rel-chip {
		background: hsl(var(--muted));
		border-radius: 4px;
		padding: 0.2rem 0.5rem;
		font-size: 0.75rem;
		color: hsl(var(--foreground));
		display: inline-flex;
		align-items: center;
		gap: 0.3rem;
	}

	.fc-rel-tr {
		font-size: 0.7rem;
		color: hsl(var(--muted-foreground));
	}

	.fc-rel-type {
		font-size: 0.6rem;
		color: hsl(var(--muted-foreground));
		text-transform: uppercase;
		letter-spacing: 0.04em;
	}

	.fc-actions {
		border-top: 1px solid hsl(var(--border));
		padding: 0.75rem 1.25rem;
		display: flex;
		gap: 0.5rem;
	}

	.fc-btn {
		flex: 1;
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 0.375rem;
		border-radius: 6px;
		padding: 0.5rem 0;
		font-size: 0.8rem;
		cursor: pointer;
		border: 1px solid transparent;
	}

	.fc-btn:disabled {
		opacity: 0.5;
		cursor: not-allowed;
	}

	.fc-btn-correct {
		background: hsl(var(--foreground));
		color: hsl(var(--background));
	}

	.fc-btn-correct:hover:not(:disabled) {
		opacity: 0.85;
	}

	.fc-btn-outline {
		background: transparent;
		color: hsl(var(--foreground));
		border-color: hsl(var(--border));
	}

	.fc-btn-outline:hover:not(:disabled) {
		background: hsl(var(--muted));
	}
</style>
