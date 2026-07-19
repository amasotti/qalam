<script lang="ts">
import { goto } from '$app/navigation';
import type { SessionSummaryResponse, TrainingSessionWordResponse } from '$lib/api/types.gen';
import { Button } from '$lib/components/ui/button';

interface Props {
	summary: SessionSummaryResponse;
	words: TrainingSessionWordResponse[];
}

const STATUS_MAP = {
	CORRECT: 'Correct',
	INCORRECT: 'Wrong',
	SKIPPED: 'Skipped',
} as const;
let { summary, words }: Props = $props();
let accuracyPercent = $derived(Math.round(summary.accuracy * 100));
let answered = $derived(summary.correct + summary.incorrect);
</script>

<div class="session-summary">
	<header class="session-summary-header">
		<p class="session-summary-kicker">Flashcards | {summary.mode}</p>
		<h1 class="session-summary-heading">Session Review</h1>
		<p class="session-summary-subtitle">{summary.totalWords} words reviewed</p>
	</header>

	<section class="session-outcome" aria-label="Session outcome">
		<div
			class="session-accuracy-chart"
			style:--accuracy={`${accuracyPercent}%`}
			aria-label={`${accuracyPercent}% accuracy`}
		>
			<div class="session-accuracy-value">{accuracyPercent}<span>%</span></div>
			<div class="session-accuracy-label">accuracy</div>
		</div>
		<div class="session-outcome-copy">
			<h2>How it went</h2>
			<p>{answered === 0 ? 'No answers recorded.' : `${summary.correct} of ${answered} answered correctly.`}</p>
			<dl class="session-outcome-breakdown">
				<div class="correct"><dt>Correct</dt><dd>{summary.correct}</dd></div>
				<div class="incorrect"><dt>Wrong</dt><dd>{summary.incorrect}</dd></div>
				<div class="skipped"><dt>Skipped</dt><dd>{summary.skipped}</dd></div>
			</dl>
		</div>
	</section>

	{#if summary.promotions.length > 0}
		<div class="promotions-section">
			<h2 class="promotions-heading">Mastery promotions</h2>
			<ul class="promotions-list">
				{#each summary.promotions as promotion (promotion.wordId)}
					<li class="promotion-item">{promotion.from} → {promotion.to}</li>
				{/each}
			</ul>
		</div>
	{/if}

	<section class="session-word-results" aria-labelledby="session-word-results-heading">
		<div class="session-word-results-title-row">
			<h2 id="session-word-results-heading" class="session-word-results-heading">Words reviewed</h2>
			<span>{words.length}</span>
		</div>

		<ul class="session-word-results-list">
			{#each words as word (word.wordId)}
				<li class="session-word-result">
					<a class="session-word-link" href={`/words/${word.wordId}`}>
						<span class="session-word-arabic arabic" lang="ar">{word.arabicText}</span>
						<span class="session-word-transliteration">{word.transliteration || '' }</span>
						<span class="session-word-translation">{word.translation || 'No translation'}</span>
					</a>
					<span
						class="session-word-result-status"
						class:correct={word.result === 'CORRECT'}
						class:incorrect={word.result === 'INCORRECT'}
						class:skipped={word.result === 'SKIPPED'}
					>{STATUS_MAP[word.result!]}</span>
				</li>
			{/each}
		</ul>
	</section>

	<div class="session-summary-actions">
		<Button onclick={() => goto('/training/flashcards')}>New flashcard session</Button>
	</div>
</div>
