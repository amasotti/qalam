<script lang="ts">
import { goto } from '$app/navigation';
import type { SessionSummaryResponse } from '$lib/api/types.gen';
import { Button } from '$lib/components/ui/button';

interface Props {
	summary: SessionSummaryResponse;
}

let { summary }: Props = $props();

let accuracyPercent = $derived(Math.round(summary.accuracy * 100));
</script>

<div class="session-summary">
	<h1 class="session-summary-heading">Session complete</h1>

	<div class="session-summary-stats">
		<div class="session-stat">
			<div class="session-stat-value accuracy">{accuracyPercent}%</div>
			<div class="session-stat-label">Accuracy</div>
		</div>
		<div class="session-stat">
			<div class="session-stat-value correct">{summary.correct}</div>
			<div class="session-stat-label">Correct</div>
		</div>
		<div class="session-stat">
			<div class="session-stat-value incorrect">{summary.incorrect}</div>
			<div class="session-stat-label">Wrong</div>
		</div>
		<div class="session-stat">
			<div class="session-stat-value skipped">{summary.skipped}</div>
			<div class="session-stat-label">Skipped</div>
		</div>
		<div class="session-stat">
			<div class="session-stat-value mode">{summary.mode}</div>
			<div class="session-stat-label">Mode</div>
		</div>
	</div>

	{#if summary.promotions.length > 0}
		<div class="promotions-section">
			<h2 class="promotions-heading">Mastery promotions</h2>
			<ul class="promotions-list">
				{#each summary.promotions as promotion (promotion.wordId)}
					<li class="promotion-item">
						{promotion.from} → {promotion.to}
					</li>
				{/each}
			</ul>
		</div>
	{/if}

	<div class="session-summary-actions">
		<Button onclick={() => goto('/training')}>New session</Button>
	</div>
</div>
