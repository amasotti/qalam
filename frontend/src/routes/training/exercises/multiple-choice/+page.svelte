<script lang="ts">
import ExerciseSessionComposer from '$lib/components/training/ExerciseSessionComposer.svelte';
import ExerciseSessionHistory from '$lib/components/training/ExerciseSessionHistory.svelte';
import { useTrainingStats } from '$lib/stores/training';
import { useAllWordLists } from '$lib/stores/wordLists';

const stats = useTrainingStats();
const wordLists = useAllWordLists();
const totalVocabulary = $derived(
	Object.values(stats.data?.masteryDistribution ?? {}).reduce((total, count) => total + count, 0)
);
</script>

<div class="training-home">
	<header class="training-home-header"><p class="training-home-kicker">Practice · Multiple choice</p><h1>Choose the meaning</h1><p>Recognition practice with four plausible answers for every word.</p></header>
	<ExerciseSessionComposer wordLists={wordLists.data} isLoadingLists={wordLists.isPending} isListError={wordLists.isError} {totalVocabulary} />
	<ExerciseSessionHistory />
</div>
