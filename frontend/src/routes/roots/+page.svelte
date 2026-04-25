<script lang="ts">
import { Search } from 'lucide-svelte';
import { useAllRoots } from '$lib/stores/roots';

const roots = useAllRoots();

const LETTER_COUNTS = [2, 3, 4, 5, 6];
const PAGE_SIZE = 24;

let search = $state('');
let letterFilter = $state<number | null>(null);
let page = $state(1);

const filtered = $derived.by(() => {
	let items = roots.data ?? [];
	if (letterFilter !== null) {
		items = items.filter((r) => r.letterCount === letterFilter);
	}
	if (search.trim()) {
		const q = search.trim().toLowerCase();
		items = items.filter(
			(r) =>
				r.normalizedForm.toLowerCase().includes(q) ||
				r.displayForm.includes(q) ||
				(r.meaning?.toLowerCase().includes(q) ?? false)
		);
	}
	return items;
});

const totalPages = $derived(Math.max(1, Math.ceil(filtered.length / PAGE_SIZE)));
const paginated = $derived(filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE));
</script>

<div class="list-page">
	<header class="list-page-header">
		<h1 class="list-page-title">Roots</h1>
		<a href="/roots/new" class="btn btn-primary">+ New root</a>
	</header>

	<div class="list-toolbar">
		<div class="search-wrap">
			<Search size={14} />
			<input
				class="search-input"
				type="text"
				placeholder="Search roots…"
				value={search}
				oninput={(e) => {
					search = e.currentTarget.value
					page = 1
				}}
			/>
		</div>

		<div class="letter-tabs">
			<button
				class="letter-tab"
				class:active={letterFilter === null}
				onclick={() => {
					letterFilter = null
					page = 1
				}}
			>
				All
			</button>
			{#each LETTER_COUNTS as n}
				<button
					class="letter-tab"
					class:active={letterFilter === n}
					onclick={() => {
						letterFilter = n
						page = 1
					}}
				>
					{n}
				</button>
			{/each}
		</div>
	</div>

	{#if roots.isPending}
		<p class="results-meta">Loading…</p>
	{:else if roots.isError}
		<p class="results-meta">
			Could not load roots — is the backend running?
		</p>
	{:else if filtered.length === 0}
		<div class="list-empty">
			<span class="list-empty-icon">ج-ذ-ر</span>
			<p class="list-empty-label">
				{search || letterFilter !== null ? 'No roots match your filter.' : 'No roots yet.'}
			</p>
			{#if !search && letterFilter === null}
				<a href="/roots/new" class="btn">Add your first root</a>
			{/if}
		</div>
	{:else}
		<p class="results-meta">
			{filtered.length} root{filtered.length === 1 ? '' : 's'}
			{#if letterFilter !== null}({letterFilter}-letter){/if}
		</p>

		<div class="roots-grid stagger-children">
			{#each paginated as root (root.id)}
				<a class="root-card" href="/roots/{root.id}">
					<div class="root-card-ar">{root.displayForm}</div>
					<div class="root-card-meta">
						<span class="root-card-letters">{root.letterCount}L</span>
						{#if root.normalizedForm}
							<span class="root-card-normalized">{root.normalizedForm}</span>
						{/if}
					</div>
					{#if root.meaning}
						<div class="root-card-meaning">{root.meaning}</div>
					{/if}
				</a>
			{/each}
		</div>

		{#if totalPages > 1}
			<div class="pagination">
				<button class="btn" disabled={page === 1} onclick={() => (page -= 1)}>Previous</button>
				<span class="pagination-info">Page {page} of {totalPages}</span>
				<button class="btn" disabled={page === totalPages} onclick={() => (page += 1)}>Next</button>
			</div>
		{/if}
	{/if}
</div>
