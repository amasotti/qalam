<script lang="ts">
import { browser } from '$app/environment';
import { goto } from '$app/navigation';
import { page } from '$app/state';
import ViewToggle from '$lib/components/ViewToggle.svelte';
import {
	useAddWordToList,
	useDeleteWordList,
	useRemoveWordFromList,
	useUpdateWordList,
	useWordList,
} from '$lib/stores/wordLists';
import { useWordAutocomplete } from '$lib/stores/words';

const id = $derived(page.params.id ?? '');
const list = useWordList(() => id);
const updateList = useUpdateWordList();
const deleteList = useDeleteWordList();
const addWord = useAddWordToList();
const removeWord = useRemoveWordFromList();

const STORAGE_KEY = 'qalam:view:word-lists';
let view = $state<'grid' | 'table'>(
	browser ? ((localStorage.getItem(STORAGE_KEY) as 'grid' | 'table' | null) ?? 'grid') : 'grid'
);
function setView(v: 'grid' | 'table') {
	view = v;
	if (browser) localStorage.setItem(STORAGE_KEY, v);
}

let isEditing = $state(false);
let editedTitle = $state('');
let editedDesc = $state('');
let deleteConfirm = $state(false);

// Add-word search
let addQuery = $state('');
const search = useWordAutocomplete(() => addQuery.trim());

const memberIds = $derived(new Set((list.data?.words ?? []).map((w) => w.id)));
const searchResults = $derived((search.data ?? []).filter((w) => !memberIds.has(w.id)));

function startEdit() {
	editedTitle = list.data?.title ?? '';
	editedDesc = list.data?.description ?? '';
	deleteConfirm = false;
	isEditing = true;
}

async function saveEdit(e: SubmitEvent) {
	e.preventDefault();
	if (!editedTitle.trim()) return;
	await updateList.mutateAsync({
		id,
		body: { title: editedTitle.trim(), description: editedDesc.trim() || null },
	});
	isEditing = false;
}

async function handleDelete() {
	if (!deleteConfirm) {
		deleteConfirm = true;
		setTimeout(() => (deleteConfirm = false), 3000);
		return;
	}
	await deleteList.mutateAsync(id);
	goto('/word-lists');
}

async function handleAdd(wordId: string) {
	await addWord.mutateAsync({ listId: id, wordId });
	addQuery = '';
}

function remove(wordId: string) {
	removeWord.mutate({ listId: id, wordId });
}

function formatEnum(value: string): string {
	return value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();
}
</script>

{#if list.isPending}
	<p class="detail-status status-text status-text-muted">Loading…</p>
{:else if list.isError}
	<p class="detail-status status-text status-text-danger">List not found.</p>
{:else if list.data}
	<nav class="breadcrumb">
		<a href="/word-lists">Lists</a>
		<span class="bc-sep">/</span>
		<span class="bc-cur">{list.data.title}</span>
	</nav>

	<div class="list-page">
		{#if isEditing}
			<form class="form-shell section-block" onsubmit={saveEdit}>
				<div class="form-field">
					<label class="form-label" for="wl-edit-title">Title</label>
					<input id="wl-edit-title" class="form-input" type="text" bind:value={editedTitle} required />
				</div>
				<div class="form-field">
					<label class="form-label" for="wl-edit-desc">Description</label>
					<textarea id="wl-edit-desc" class="form-input" rows="3" bind:value={editedDesc}></textarea>
				</div>
				<div class="form-actions">
					<button type="button" class="btn" onclick={() => (isEditing = false)}>Cancel</button>
					<button
						type="submit"
						class="btn btn-primary"
						disabled={updateList.isPending || !editedTitle.trim()}
					>
						{updateList.isPending ? 'Saving…' : 'Save'}
					</button>
				</div>
			</form>
		{:else}
			<header class="list-page-header">
				<div>
					<h1 class="list-page-title">{list.data.title}</h1>
					{#if list.data.description}
						<p class="form-page-subtitle">{list.data.description}</p>
					{/if}
				</div>
				<div class="word-actions">
					<button class="btn btn-primary" onclick={startEdit}>Edit</button>
					<button class="btn btn-danger" onclick={handleDelete} disabled={deleteList.isPending}>
						{deleteConfirm ? 'Confirm delete' : 'Delete'}
					</button>
				</div>
			</header>
		{/if}

		<!-- Add a word -->
		<div class="section-block">
			<div class="sect-label">Add a word</div>
			<div class="wl-add">
				<input
					class="form-input"
					type="text"
					placeholder="Search your vocabulary…"
					bind:value={addQuery}
				/>
				{#if addQuery.trim() && searchResults.length > 0}
					<div class="wl-add-results">
						{#each searchResults as w (w.id)}
							<button
								class="wl-add-result"
								onclick={() => handleAdd(w.id)}
								disabled={addWord.isPending}
							>
								<span class="wl-add-result-ar">{w.arabicText}</span>
								{#if w.translation}<span class="wl-add-result-en">{w.translation}</span>{/if}
							</button>
						{/each}
					</div>
				{/if}
			</div>
		</div>

		<!-- Members -->
		{#if list.data.words.length === 0}
			<div class="list-empty">
				<span class="list-empty-icon">قائمة</span>
				<p class="list-empty-label">No words yet — search above to add some.</p>
			</div>
		{:else}
			<div class="results-meta-row">
				<p class="results-meta" style="margin-bottom:0">
					{list.data.words.length} word{list.data.words.length === 1 ? '' : 's'} in this list
				</p>
				<ViewToggle {view} onchange={setView} />
			</div>

			{#if view === 'grid'}
				<div class="words-grid stagger-children">
					{#each list.data.words as w (w.id)}
						<div class="word-card wcard-{w.masteryLevel.toLowerCase()}">
							<a class="wl-member-link" href="/words/{w.id}">
								<div class="word-card-ar">{w.arabicText}</div>
								{#if w.transliteration}<div class="word-card-tr">{w.transliteration}</div>{/if}
								{#if w.translation}<div class="word-card-en">{w.translation}</div>{/if}
								<div class="word-card-badges">
									<span class="chip c-olive">{formatEnum(w.partOfSpeech)}</span>
								</div>
							</a>
							<button
								class="wl-member-remove"
								onclick={() => remove(w.id)}
								disabled={removeWord.isPending}
								aria-label="Remove from list"
							>×</button>
						</div>
					{/each}
				</div>
			{:else}
				<table class="list-table words-table">
					<thead>
						<tr>
							<th>Word</th>
							<th>Translation</th>
							<th>POS</th>
							<th>Mastery</th>
							<th class="list-table-th-right">Remove</th>
						</tr>
					</thead>
					<tbody>
						{#each list.data.words as w (w.id)}
							<tr
								class="list-table-row"
								onclick={() => goto(`/words/${w.id}`)}
								onkeydown={(e) => e.key === 'Enter' && goto(`/words/${w.id}`)}
								tabindex="0"
								role="link"
								aria-label={w.arabicText}
							>
								<td class="list-table-td-word">
									<div class="list-table-ar">{w.arabicText}</div>
									{#if w.transliteration}
										<div class="list-table-tr">{w.transliteration}</div>
									{/if}
								</td>
								<td class="list-table-td-main">
									{#if w.translation}
										{w.translation}
									{:else}
										<span class="list-table-empty-cell">—</span>
									{/if}
								</td>
								<td>
									<span class="chip chip-sm c-muted">{formatEnum(w.partOfSpeech)}</span>
								</td>
								<td>
									<span class="chip chip-sm c-coral">{formatEnum(w.masteryLevel)}</span>
								</td>
								<td class="list-table-td-right">
									<button
										class="btn btn-xs"
										onclick={(e) => {
											e.stopPropagation();
											remove(w.id);
										}}
										disabled={removeWord.isPending}
										aria-label="Remove {w.arabicText} from list"
									>Remove</button>
								</td>
							</tr>
						{/each}
					</tbody>
				</table>
			{/if}
		{/if}
	</div>
{/if}
