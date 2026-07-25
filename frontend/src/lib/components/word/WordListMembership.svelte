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
const hasAvailable = $derived((allLists.data ?? []).some((l) => !memberIds.has(l.id)));

let q = $state('');
let open = $state(false);

const filtered = $derived(
	(allLists.data ?? [])
		.filter((l) => !memberIds.has(l.id))
		.filter((l) => !q || l.title.toLowerCase().includes(q.toLowerCase()))
		.sort((a, b) => a.title.localeCompare(b.title))
);

async function select(listId: string) {
	open = false;
	q = '';
	await addWord.mutateAsync({ listId, wordId });
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

	{#if hasAvailable}
		<div class="wl-add wl-add-membership">
			<input
				type="text"
				class="form-input"
				placeholder="+ Add to list…"
				bind:value={q}
				onfocus={() => (open = true)}
				onblur={() => setTimeout(() => (open = false), 150)}
				disabled={addWord.isPending}
			/>
			{#if open && filtered.length > 0}
				<div class="wl-add-results">
					{#each filtered as l (l.id)}
						<button
							type="button"
							class="wl-add-result"
							onmousedown={() => select(l.id)}
						>
							{l.title}
						</button>
					{/each}
				</div>
			{/if}
		</div>
	{/if}
</div>
