<script lang="ts">
import { goto } from '$app/navigation';
import type { WordListResponse } from '$lib/api/types.gen';
import { Button } from '$lib/components/ui/button';
import {
	useConjugationExerciseEligibility,
	useCreateConjugationExerciseSession,
} from '$lib/stores/conjugationExercises';

interface Props {
	wordLists?: WordListResponse[];
	isLoadingLists: boolean;
	isListError: boolean;
}
const modes = ['MIXED', 'NEW', 'LEARNING', 'KNOWN'] as const;
const sizes = [3, 5, 7, 10];
const tenses = ['PRESENT', 'PAST'] as const;
let { wordLists = [], isLoadingLists, isListError }: Props = $props();
const createSession = useCreateConjugationExerciseSession();
let mode = $state<(typeof modes)[number]>('MIXED');
let size = $state(5);
let tense = $state<'PAST' | 'PRESENT'>('PRESENT');
let selectedListIds = $state<string[]>([]);
let scope = $state<'ALL' | 'LISTS'>('ALL');
const selectedCount = $derived(selectedListIds.length);
const eligibility = useConjugationExerciseEligibility(
	() => mode,
	() => (scope === 'ALL' ? [] : selectedListIds)
);
const availableVerbs = $derived(eligibility.data?.availableVerbs ?? 0);
const notEnoughVerbs = $derived(
	!eligibility.isPending && !eligibility.isError && availableVerbs < 3
);

function toggleList(id: string) {
	selectedListIds = selectedListIds.includes(id)
		? selectedListIds.filter((value) => value !== id)
		: [...selectedListIds, id];
}
function start() {
	if (scope === 'LISTS' && selectedCount === 0) return;
	createSession.mutate(
		{ mode, size, wordListIds: scope === 'ALL' ? [] : selectedListIds, tense, voice: 'ACTIVE' },
		{ onSuccess: (session) => goto(`/training/conjugation-exercises/${session.id}`) }
	);
}
</script>

<section class="training-composer" aria-labelledby="conjugation-composer-heading">
	<div class="training-section-heading"><div><h2 id="conjugation-composer-heading">New conjugation exercise</h2><p>Match four vocalised forms to their person labels.</p></div><span class="training-card-count">{size} boards</span></div>
	<div class="training-form-section"><div class="training-field-heading"><span class="training-field-label">Verbs to practise</span></div>
		{#if isLoadingLists}<p class="training-muted">Loading word lists…</p>
		{:else if isListError}<p class="form-error-msg">Could not load word lists.</p>
		{:else}<div class="training-scope-options"><button type="button" class="training-scope-option" class:selected={scope === 'ALL'} onclick={() => (scope = 'ALL')}><span>All eligible verbs</span><small>Saved verbs with root and metadata</small></button><button type="button" class="training-scope-option" class:selected={scope === 'LISTS'} onclick={() => (scope = 'LISTS')}><span>Specific lists</span><small>{selectedCount === 0 ? 'Choose lists' : `${selectedCount} selected`}</small></button></div>
			{#if scope === 'LISTS'}<div class="conj-exercise-list-options">{#each wordLists as list (list.id)}<label><input type="checkbox" checked={selectedListIds.includes(list.id)} disabled={list.itemCount === 0} onchange={() => toggleList(list.id)} /> {list.title} <small>{list.itemCount} words</small></label>{/each}</div>{/if}
		{/if}
	</div>
	<div class="training-form-section"><div class="training-field-heading"><span class="training-field-label">Mastery focus</span></div><div class="training-size-options" role="radiogroup" aria-label="Mastery focus">{#each modes as candidate}<button type="button" class="training-size-option" class:selected={mode === candidate} role="radio" aria-checked={mode === candidate} onclick={() => (mode = candidate)}>{candidate[0] + candidate.slice(1).toLowerCase()}</button>{/each}</div></div>
	<div class="training-form-section"><div class="training-field-heading"><span class="training-field-label">Tense</span></div><div class="training-size-options" role="radiogroup" aria-label="Tense">{#each tenses as candidate}<button type="button" class="training-size-option" class:selected={tense === candidate} role="radio" aria-checked={tense === candidate} onclick={() => (tense = candidate)}>{candidate[0] + candidate.slice(1).toLowerCase()} active</button>{/each}</div></div>
	<div class="training-form-section"><div class="training-field-heading"><span class="training-field-label">How many boards?</span></div><div class="training-size-options" role="radiogroup" aria-label="Board count">{#each sizes as candidate}<button type="button" class="training-size-option" class:selected={size === candidate} role="radio" aria-checked={size === candidate} onclick={() => (size = candidate)}>{candidate}</button>{/each}</div></div>
	{#if scope === 'LISTS' && selectedCount === 0}<p class="form-error-msg">Choose at least one list, or use all eligible verbs.</p>{/if}
	{#if eligibility.isPending}<p class="training-muted">Checking eligible verbs…</p>
	{:else if eligibility.isError}<p class="form-error-msg">Could not count eligible verbs.</p>
	{:else if notEnoughVerbs}<p class="form-error-msg">{availableVerbs} eligible verb{availableVerbs === 1 ? '' : 's'} available. Add at least {3 - availableVerbs} more to start.</p>
	{:else}<p class="training-muted">{availableVerbs} eligible verbs available.</p>{/if}
	{#if createSession.error}<p class="form-error-msg">{createSession.error.message}</p>{/if}
	<Button size="lg" onclick={start} disabled={createSession.isPending || eligibility.isPending || eligibility.isError || notEnoughVerbs || (scope === 'LISTS' && selectedCount === 0)}>{createSession.isPending ? 'Preparing boards…' : 'Start matching'}</Button>
</section>
