<script lang="ts">
import type { ExerciseSessionSummaryResponse } from '$lib/api/types.gen';
import { Button } from '$lib/components/ui/button';

interface Props {
	summary: ExerciseSessionSummaryResponse;
}

let { summary }: Props = $props();
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
					<li class="promotion-item">{promotion.from} → {promotion.to}</li>
				{/each}
			</ul>
		</section>
	{/if}

	<div class="session-summary-actions">
		<Button href="/training/exercises/multiple-choice">New multiple-choice exercise</Button>
	</div>
</div>
