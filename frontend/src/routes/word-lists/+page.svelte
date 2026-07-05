<script lang="ts">
import { Search } from 'lucide-svelte';
import { useAllWordLists } from '$lib/stores/wordLists';

const lists = useAllWordLists();

let search = $state('');

const filtered = $derived.by(() => {
	const items = lists.data ?? [];
	const q = search.trim().toLowerCase();
	if (!q) return items;
	return items.filter(
		(l) => l.title.toLowerCase().includes(q) || (l.description?.toLowerCase().includes(q) ?? false)
	);
});
</script>

<div class="list-page">
	<header class="list-page-header">
		<h1 class="list-page-title">Lists</h1>
		<a href="/word-lists/new" class="btn btn-primary">+ New list</a>
	</header>

	<div class="list-toolbar">
		<div class="search-wrap">
			<Search size={14} />
			<input
				class="search-input"
				type="text"
				placeholder="Search lists…"
				value={search}
				oninput={(e) => (search = e.currentTarget.value)}
			/>
		</div>
	</div>

	{#if lists.isPending}
		<p class="results-meta">Loading…</p>
	{:else if lists.isError}
		<p class="results-meta">Could not load lists — is the backend running?</p>
	{:else if filtered.length === 0}
		<div class="list-empty">
			<span class="list-empty-icon">قائمة</span>
			<p class="list-empty-label">
				{search ? 'No lists match your search.' : 'No lists yet.'}
			</p>
			{#if !search}
				<a href="/word-lists/new" class="btn">Create your first list</a>
			{/if}
		</div>
	{:else}
		<p class="results-meta">
			{filtered.length} list{filtered.length === 1 ? '' : 's'}
		</p>
		<div class="wordlists-grid stagger-children">
			{#each filtered as list (list.id)}
				<a class="wl-card" href="/word-lists/{list.id}">
					<div class="wl-card-head">
						<span class="wl-card-title">{list.title}</span>
						<span class="wl-card-count">
							{list.itemCount} word{list.itemCount === 1 ? '' : 's'}
						</span>
					</div>
					{#if list.description}
						<p class="wl-card-desc">{list.description}</p>
					{/if}
				</a>
			{/each}
		</div>
	{/if}
</div>
