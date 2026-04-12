<script lang="ts">
import { Button } from '$lib/components/ui/button';
import { useAllRoots } from '$lib/stores/roots';
import { Plus, Search } from 'lucide-svelte';

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

<div class="page-roots page-enter">
	<header class="roots-page-header">
		<h1 class="roots-page-title">Roots</h1>
		<Button href="/roots/new">
			<Plus size={14} />
			New root
		</Button>
	</header>

	<div class="roots-toolbar">
		<div class="roots-search-wrap">
			<Search size={14} />
			<input
				class="roots-search"
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
		<p class="roots-results-meta">Loading…</p>
	{:else if roots.isError}
		<p class="roots-results-meta">
			Could not load roots — is the backend running?
		</p>
	{:else if filtered.length === 0}
		<div class="roots-empty">
			<span class="roots-empty-icon">ج-ذ-ر</span>
			<p class="roots-empty-label">
				{search || letterFilter !== null ? 'No roots match your filter.' : 'No roots yet.'}
			</p>
			{#if !search && letterFilter === null}
				<Button href="/roots/new" variant="outline">Add your first root</Button>
			{/if}
		</div>
	{:else}
		<p class="roots-results-meta">
			{filtered.length} root{filtered.length === 1 ? '' : 's'}
			{#if letterFilter !== null}({letterFilter}-letter){/if}
		</p>

		<div class="roots-grid stagger-children">
			{#each paginated as root (root.id)}
				<a class="root-card" href="/roots/{root.id}">
					<div class="root-card-arabic">{root.displayForm}</div>
					<div class="root-card-meta">
						<span class="root-card-letters">{root.letterCount}ح</span>
						<span class="root-card-normalized">{root.normalizedForm}</span>
					</div>
					{#if root.meaning}
						<p class="root-card-meaning">{root.meaning}</p>
					{/if}
				</a>
			{/each}
		</div>

		{#if totalPages > 1}
			<div class="roots-pagination">
				<Button
					variant="outline"
					size="sm"
					disabled={page === 1}
					onclick={() => (page -= 1)}
				>
					Previous
				</Button>
				<span class="roots-pagination-info">Page {page} of {totalPages}</span>
				<Button
					variant="outline"
					size="sm"
					disabled={page === totalPages}
					onclick={() => (page += 1)}
				>
					Next
				</Button>
			</div>
		{/if}
	{/if}
</div>
