<script lang="ts">
import TrainingSessionComposer from '$lib/components/training/TrainingSessionComposer.svelte';
import TrainingSessionHistory from '$lib/components/training/TrainingSessionHistory.svelte';
import { useTrainingStats } from '$lib/stores/training';
import { useAllWordLists } from '$lib/stores/wordLists';
import { formatTrainingMode } from '$lib/training/presentation';

const stats = useTrainingStats();
const wordLists = useAllWordLists();
const totalVocabulary = $derived(
	Object.values(stats.data?.masteryDistribution ?? {}).reduce((total, count) => total + count, 0)
);
</script>

<div class="training-home">
	<header class="training-home-header">
		<p class="training-home-kicker">Practice · Flashcards</p>
		<h1>Review your vocabulary</h1>
		<p>Choose a focused set, then recall before you reveal.</p>
	</header>

	<div class="training-home-layout">
		<TrainingSessionComposer wordLists={wordLists.data} isLoadingLists={wordLists.isPending} isListError={wordLists.isError} {totalVocabulary} />

		<aside class="training-context" aria-label="Vocabulary overview">
			<h2>Your vocabulary</h2>
			{#if stats.isPending}
				<p class="training-muted">Loading progress…</p>
			{:else if stats.data}
				<p class="training-context-total">{totalVocabulary}<span>words</span></p>
				<div class="training-mastery-list">
					{#each Object.entries(stats.data.masteryDistribution) as [level, count]}
						<div class="training-mastery-row"><span>{formatTrainingMode(level)}</span><div><i style:width={`${totalVocabulary === 0 ? 0 : (count / totalVocabulary) * 100}%`}></i></div><strong>{count}</strong></div>
					{/each}
				</div>
				<p class="training-context-note">{stats.data.totalSessions} completed session{stats.data.totalSessions === 1 ? '' : 's'}.</p>
			{:else}
				<p class="training-muted">Progress is unavailable.</p>
			{/if}
		</aside>
	</div>

	<TrainingSessionHistory />
</div>
