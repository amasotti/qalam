<script lang="ts">
import { goto } from '$app/navigation';
import type { WordListResponse } from '$lib/api/types.gen';
import { Button } from '$lib/components/ui/button';
import { useCreateSession } from '$lib/stores/training';

interface Props {
	wordLists?: WordListResponse[];
	isLoadingLists: boolean;
	isListError: boolean;
	totalVocabulary: number;
}

const modes = ['MIXED', 'NEW', 'LEARNING', 'KNOWN'] as const;
type Mode = (typeof modes)[number];
const sessionSizes = [5, 10, 15, 20, 25, 30, 50];
const modeCopy: Record<Mode, { label: string; description: string }> = {
	MIXED: { label: 'Mixed review', description: 'A balanced pass across your vocabulary.' },
	NEW: { label: 'New words', description: 'Meet words you have not practised yet.' },
	LEARNING: { label: 'Learning', description: 'Strengthen words still in progress.' },
	KNOWN: { label: 'Known', description: 'Keep familiar vocabulary active.' },
};

let { wordLists = [], isLoadingLists, isListError, totalVocabulary }: Props = $props();
const createSession = useCreateSession();
let selectedMode = $state<Mode>('MIXED');
let sessionSize = $state(15);
let selectedWordListIds = $state<string[]>([]);
const selectedListCount = $derived(selectedWordListIds.length);

function toggleWordList(id: string) {
	selectedWordListIds = selectedWordListIds.includes(id)
		? selectedWordListIds.filter((selectedId) => selectedId !== id)
		: [...selectedWordListIds, id];
}

function start() {
	createSession.mutate(
		{ mode: selectedMode, size: sessionSize, wordListIds: selectedWordListIds },
		{ onSuccess: (session) => goto(`/training/${session.id}`) }
	);
}
</script>

<section class="training-composer" aria-labelledby="new-session-heading">
	<div class="training-section-heading">
		<div><h2 id="new-session-heading">New session</h2><p>Set a scope and let the cards do the rest.</p></div>
		<span class="training-card-count">{sessionSize} cards</span>
	</div>

	<div class="training-form-section">
		<div class="training-field-heading">
			<span class="training-field-label">What should you review?</span>
			<span>{selectedListCount === 0 ? 'All vocabulary' : `${selectedListCount} list${selectedListCount === 1 ? '' : 's'} selected`}</span>
		</div>
		{#if isLoadingLists}
			<p class="training-muted">Loading word lists…</p>
		{:else if isListError}
			<p class="form-error-msg">Could not load word lists.</p>
		{:else if wordLists.length > 0}
			<div class="training-scope-options">
				<button type="button" class="training-scope-option" class:selected={selectedListCount === 0} onclick={() => (selectedWordListIds = [])}><span>All vocabulary</span><small>{totalVocabulary} words</small></button>
				{#each wordLists as list}
					<button type="button" class="training-scope-option" class:selected={selectedWordListIds.includes(list.id)} disabled={list.itemCount === 0} onclick={() => toggleWordList(list.id)}><span>{list.title}</span><small>{list.itemCount} words</small></button>
				{/each}
			</div>
		{:else}
			<p class="training-muted">No word lists yet. This session will use all vocabulary.</p>
		{/if}
	</div>

	<div class="training-form-section">
		<div class="training-field-heading"><span class="training-field-label">Choose a focus</span></div>
		<div class="training-mode-options" role="radiogroup" aria-label="Flashcard focus">
			{#each modes as mode}
				<button type="button" class="training-mode-option" class:selected={selectedMode === mode} role="radio" aria-checked={selectedMode === mode} onclick={() => (selectedMode = mode)}><span>{modeCopy[mode].label}</span><small>{modeCopy[mode].description}</small></button>
			{/each}
		</div>
	</div>

	<div class="training-form-section">
		<div class="training-field-heading"><span class="training-field-label">How many cards?</span></div>
		<div class="training-size-options" role="radiogroup" aria-label="Session size">
			{#each sessionSizes as size}
				<button type="button" class:selected={sessionSize === size} class="training-size-option" role="radio" aria-checked={sessionSize === size} onclick={() => (sessionSize = size)}>{size}</button>
			{/each}
		</div>
	</div>

	{#if createSession.error}<p class="form-error-msg">{createSession.error.message}</p>{/if}
	<Button size="lg" onclick={start} disabled={createSession.isPending}>{createSession.isPending ? 'Preparing session…' : 'Start flashcards'}</Button>
</section>
