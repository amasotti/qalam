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
