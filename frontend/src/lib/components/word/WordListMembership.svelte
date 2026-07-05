<script lang="ts">
import {
	useAddWordToList,
	useAllWordLists,
	useListsForWord,
	useRemoveWordFromList,
} from '$lib/stores/wordLists';

const { wordId }: { wordId: string } = $props();

const memberships = useListsForWord(() => wordId);
const allLists = useAllWordLists();
const addWord = useAddWordToList();
const removeWord = useRemoveWordFromList();

const memberIds = $derived(new Set((memberships.data ?? []).map((l) => l.id)));
const available = $derived((allLists.data ?? []).filter((l) => !memberIds.has(l.id)));

async function addToList(e: Event) {
	// Capture the element before awaiting — currentTarget is nulled once the event settles.
	const select = e.currentTarget as HTMLSelectElement;
	const listId = select.value;
	if (!listId) return;
	await addWord.mutateAsync({ listId, wordId });
	select.value = '';
}
</script>

<div class="meta-card">
	<div class="meta-card-title">Lists</div>

	{#if memberships.isPending}
		<p class="annot-empty">Loading…</p>
	{:else if (memberships.data ?? []).length === 0}
		<p class="annot-empty">Not in any list yet.</p>
	{:else}
		<div class="wl-membership">
			{#each memberships.data ?? [] as l (l.id)}
				<span class="wl-chip">
					<a class="wl-chip-link" href="/word-lists/{l.id}">{l.title}</a>
					<button
						class="wl-chip-remove"
						onclick={() => removeWord.mutate({ listId: l.id, wordId })}
						disabled={removeWord.isPending}
						aria-label="Remove from {l.title}"
					>×</button>
				</span>
			{/each}
		</div>
	{/if}

	{#if available.length > 0}
		<select class="form-select wl-add-select" onchange={addToList} disabled={addWord.isPending}>
			<option value="">+ Add to list…</option>
			{#each available as l (l.id)}
				<option value={l.id}>{l.title}</option>
			{/each}
		</select>
	{/if}
</div>
