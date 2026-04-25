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
		<div class="stat">
			<div class="stat-value accuracy">{accuracyPercent}%</div>
			<div class="stat-label">Accuracy</div>
		</div>
		<div class="stat">
			<div class="stat-value correct">{summary.correct}</div>
			<div class="stat-label">Correct</div>
		</div>
		<div class="stat">
			<div class="stat-value incorrect">{summary.incorrect}</div>
			<div class="stat-label">Wrong</div>
		</div>
		<div class="stat">
			<div class="stat-value skipped">{summary.skipped}</div>
			<div class="stat-label">Skipped</div>
		</div>
		<div class="stat">
			<div class="stat-value mode">{summary.mode}</div>
			<div class="stat-label">Mode</div>
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

<style>
	.session-summary {
		display: flex;
		flex-direction: column;
		gap: 2rem;
		padding: 2rem;
		max-width: 600px;
		margin: 0 auto;
	}

	.session-summary-heading {
		font-size: 1.875rem;
		font-weight: 700;
		text-align: center;
	}

	.session-summary-stats {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
		gap: 1rem;
	}

	.stat {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.5rem;
		padding: 1rem;
		border-radius: 0.5rem;
		background-color: var(--color-bg-secondary);
	}

	.stat-value {
		font-size: 1.5rem;
		font-weight: 700;
	}

	.stat-value.correct {
		color: rgb(34 197 94);
	}

	.stat-value.incorrect {
		color: rgb(239 68 68);
	}

	.stat-value.accuracy {
		color: rgb(59 130 246);
	}

	.stat-label {
		font-size: 0.875rem;
		color: var(--color-text-secondary);
		text-align: center;
	}

	.promotions-section {
		display: flex;
		flex-direction: column;
		gap: 1rem;
		padding: 1rem;
		border-radius: 0.5rem;
		background-color: var(--color-bg-secondary);
	}

	.promotions-heading {
		font-size: 1.125rem;
		font-weight: 600;
	}

	.promotions-list {
		list-style: none;
		padding: 0;
		margin: 0;
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
	}

	.promotion-item {
		padding: 0.75rem;
		background-color: var(--color-bg-tertiary);
		border-radius: 0.375rem;
		font-size: 0.95rem;
	}

	.session-summary-actions {
		display: flex;
		justify-content: center;
	}
</style>
