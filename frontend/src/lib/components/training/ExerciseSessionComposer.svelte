<script lang="ts">
import { goto } from '$app/navigation';
import type { WordListResponse } from '$lib/api/types.gen';
import { Button } from '$lib/components/ui/button';
import { useCreateExerciseSession } from '$lib/stores/exercises';

interface Props {
	wordLists?: WordListResponse[];
	isLoadingLists: boolean;
	isListError: boolean;
	totalVocabulary: number;
}

const modes = ['MIXED', 'NEW', 'LEARNING', 'KNOWN'] as const;
type Mode = (typeof modes)[number];
const sizes = [5, 10, 15, 20];
let { wordLists = [], isLoadingLists, isListError, totalVocabulary }: Props = $props();
const createSession = useCreateExerciseSession();
let mode = $state<Mode>('MIXED');
let size = $state(10);
let selectedListIds = $state<string[]>([]);
let scope = $state<'ALL' | 'LISTS'>('ALL');
let search = $state('');
let picker = $state<HTMLDialogElement | null>(null);
const selectedCount = $derived(selectedListIds.length);
const matches = $derived(
	wordLists.filter((list) =>
		list.title.toLocaleLowerCase().includes(search.trim().toLocaleLowerCase())
	)
);

function toggleList(id: string) {
	selectedListIds = selectedListIds.includes(id)
		? selectedListIds.filter((value) => value !== id)
		: [...selectedListIds, id];
}

function openPicker() {
	scope = 'LISTS';
	picker?.showModal();
}

function start() {
	if (scope === 'LISTS' && selectedCount === 0) return;
	createSession.mutate(
		{
			mode,
			size,
			wordListIds: scope === 'ALL' ? [] : selectedListIds,
			exerciseTypes: ['MULTIPLE_CHOICE_MEANING'],
			optionCount: 4,
		},
		{ onSuccess: (session) => goto(`/training/exercises/${session.id}`) }
	);
}
</script>

<section class="training-composer" aria-labelledby="exercise-session-heading">
	<div class="training-section-heading">
		<div><h2 id="exercise-session-heading">New multiple-choice exercise</h2><p>Recognise the right meaning, then see instant feedback.</p></div>
		<span class="training-card-count">{size} questions</span>
	</div>

	<div class="training-form-section">
		<div class="training-field-heading"><span class="training-field-label">What should you review?</span><span>{scope === 'ALL' ? 'All vocabulary' : `${selectedCount} list${selectedCount === 1 ? '' : 's'} selected`}</span></div>
		{#if isLoadingLists}<p class="training-muted">Loading word lists…</p>
		{:else if isListError}<p class="form-error-msg">Could not load word lists.</p>
		{:else if wordLists.length > 0}
			<div class="training-scope-options">
				<button type="button" class="training-scope-option" class:selected={scope === 'ALL'} onclick={() => (scope = 'ALL')}><span>All vocabulary</span><small>{totalVocabulary} words</small></button>
				<button type="button" class="training-scope-option" class:selected={scope === 'LISTS'} onclick={openPicker}><span>Specific lists</span><small>{selectedCount === 0 ? 'Choose lists' : `${selectedCount} selected`}</small></button>
			</div>
		{:else}<p class="training-muted">No word lists yet. This exercise will use all vocabulary.</p>{/if}
	</div>

	<div class="training-form-section">
		<div class="training-field-heading"><span class="training-field-label">Choose a focus</span></div>
		<div class="training-size-options" role="radiogroup" aria-label="Exercise focus">
			{#each modes as candidate}<button type="button" class:selected={mode === candidate} class="training-size-option" role="radio" aria-checked={mode === candidate} onclick={() => (mode = candidate)}>{candidate[0] + candidate.slice(1).toLowerCase()}</button>{/each}
		</div>
	</div>

	<div class="training-form-section">
		<div class="training-field-heading"><span class="training-field-label">How many questions?</span></div>
		<div class="training-size-options" role="radiogroup" aria-label="Exercise size">
			{#each sizes as candidate}<button type="button" class:selected={size === candidate} class="training-size-option" role="radio" aria-checked={size === candidate} onclick={() => (size = candidate)}>{candidate}</button>{/each}
		</div>
	</div>

	{#if scope === 'LISTS' && selectedCount === 0}<p class="form-error-msg">Choose at least one list, or use all vocabulary.</p>{/if}
	{#if createSession.error}<p class="form-error-msg">{createSession.error.message}</p>{/if}
	<Button size="lg" onclick={start} disabled={createSession.isPending || (scope === 'LISTS' && selectedCount === 0)}>{createSession.isPending ? 'Preparing exercise…' : 'Start exercise'}</Button>
</section>

<dialog class="training-list-picker" bind:this={picker} onclose={() => (search = '')} aria-labelledby="exercise-list-picker-heading">
	<div class="training-list-picker-header"><div><p class="training-home-kicker">Exercise scope</p><h2 id="exercise-list-picker-heading">Choose word lists</h2><p>Words from selected lists are combined for this exercise.</p></div><button class="training-list-picker-close" type="button" onclick={() => picker?.close()} aria-label="Close list picker">×</button></div>
	<label class="training-list-search"><span>Search lists</span><input type="search" bind:value={search} placeholder="Filter by name" /></label>
	<div class="training-list-picker-results">
		{#if matches.length === 0}<p class="training-muted">No lists match “{search}”.</p>
		{:else}{#each matches as list}<label class="training-list-picker-option" class:selected={selectedListIds.includes(list.id)} class:disabled={list.itemCount === 0}><input type="checkbox" checked={selectedListIds.includes(list.id)} disabled={list.itemCount === 0} onchange={() => toggleList(list.id)} /><span>{list.title}<small>{list.itemCount} words</small></span></label>{/each}{/if}
	</div>
	<div class="training-list-picker-actions"><span>{selectedCount} selected</span><Button onclick={() => picker?.close()}>Done</Button></div>
</dialog>
