<script lang="ts">
import type {
	ExerciseSessionItemResponse,
	ExerciseSessionSummaryResponse,
} from '$lib/api/types.gen';
import { Button } from '$lib/components/ui/button';
import { exerciseItemWord } from '$lib/exercises/presentation';

interface Props {
	summary: ExerciseSessionSummaryResponse;
	items: ExerciseSessionItemResponse[];
}

const STATUS_MAP = {
	CORRECT: 'Correct',
	INCORRECT: 'Wrong',
	SKIPPED: 'Skipped',
} as const;

let { summary, items }: Props = $props();
const accuracyPercent = $derived(Math.round(summary.accuracy * 100));
const answered = $derived(summary.correct + summary.incorrect);
</script>

<div class="session-summary">
	<header class="session-summary-header">
		<p class="session-summary-kicker">Multiple choice | {summary.mode}</p>
		<h1 class="session-summary-heading">Exercise complete</h1>
		<p class="session-summary-subtitle">{summary.totalItems} questions reviewed</p>
	</header>

	<section class="session-outcome" aria-label="Exercise outcome">
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
		<section class="promotions-section" aria-labelledby="exercise-promotions-heading">
			<h2 id="exercise-promotions-heading" class="promotions-heading">Mastery promotions</h2>
			<ul class="promotions-list">
				{#each summary.promotions as promotion (promotion.wordId)}
					{@const item = items.find((candidate) => candidate.wordId === promotion.wordId)}
					{@const word = item ? exerciseItemWord(item) : null}
					<li class="promotion-item">
						<a href={`/words/${promotion.wordId}`}>
							{#if word}
								<span class="promotion-word-arabic arabic" lang="ar">{word.arabicText}</span>
								<span>{word.translation ?? 'No translation'}</span>
							{/if}
							<span class="promotion-levels">{promotion.from} → {promotion.to}</span>
						</a>
					</li>
				{/each}
			</ul>
		</section>
	{/if}

	{#if items.length > 0}
		<section class="session-word-results" aria-labelledby="exercise-word-results-heading">
			<div class="session-word-results-title-row">
				<h2 id="exercise-word-results-heading" class="session-word-results-heading">Words reviewed</h2>
				<span>{items.length}</span>
			</div>
			<ul class="session-word-results-list">
				{#each items as item (item.itemId)}
					{@const word = exerciseItemWord(item)}
					<li class="session-word-result">
						<a class="session-word-link" href={`/words/${item.wordId}`}>
							<span class="session-word-arabic arabic" lang="ar">{word.arabicText}</span>
							<span class="session-word-transliteration">{word.transliteration ?? ''}</span>
							<span class="session-word-translation">{word.translation ?? 'No translation'}</span>
						</a>
						{#if item.result}
							<span class="session-word-result-status" class:correct={item.result === 'CORRECT'} class:incorrect={item.result === 'INCORRECT'} class:skipped={item.result === 'SKIPPED'}>{STATUS_MAP[item.result]}</span>
						{/if}
					</li>
				{/each}
			</ul>
		</section>
	{/if}

	<div class="session-summary-actions">
		<Button href="/training/exercises/multiple-choice">New multiple-choice exercise</Button>
	</div>
</div>
