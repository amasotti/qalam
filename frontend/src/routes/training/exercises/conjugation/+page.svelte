<script lang="ts">
import ConjugationExerciseComposer from '$lib/components/training/ConjugationExerciseComposer.svelte';
import ConjugationExerciseHistory from '$lib/components/training/ConjugationExerciseHistory.svelte';
import { useAllWordLists } from '$lib/stores/wordLists';

const wordLists = useAllWordLists();
</script>
<div class="training-home">
	<header class="training-home-header"><p class="training-home-kicker">Practice · Verb morphology</p><h1>Match conjugated forms</h1><p>Identify person, number, and gender from four fully vocalised forms.</p></header>
	<ConjugationExerciseComposer wordLists={wordLists.data} isLoadingLists={wordLists.isPending} isListError={wordLists.isError} />
	<ConjugationExerciseHistory />
</div>
