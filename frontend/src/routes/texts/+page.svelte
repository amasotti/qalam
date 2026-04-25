<script lang="ts">
import { BookOpen, Search } from 'lucide-svelte';
import type { Dialect, Difficulty } from '$lib/api/types.gen';
import { useTexts, type TextSortField } from '$lib/stores/texts';

const PAGE_SIZE = 20;

let search = $state('');
let debouncedSearch = $state('');
let dialect = $state<Dialect | ''>('');
let difficulty = $state<Difficulty | ''>('');
let tag = $state('');
let sortBy = $state<TextSortField>('CREATED_AT');
let sortDesc = $state(true);
let page = $state(1);

$effect(() => {
	const s = search;
	const timer = setTimeout(() => {
		debouncedSearch = s;
		page = 1;
	}, 400);
	return () => clearTimeout(timer);
});

const filters = $derived({
	q: debouncedSearch.trim() || undefined,
	dialect: dialect || undefined,
	difficulty: difficulty || undefined,
	tag: tag.trim() || undefined,
	sortBy,
	sortDesc,
	page,
	size: PAGE_SIZE,
});

const texts = useTexts(() => filters);

const total = $derived(texts.data?.total ?? 0);
const totalPages = $derived(Math.max(1, Math.ceil(total / PAGE_SIZE)));

const hasActiveFilters = $derived(
	!!debouncedSearch.trim() || !!dialect || !!difficulty || !!tag.trim()
);

function formatEnum(value: string): string {
	return value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();
}
</script>

<div class="list-page">
	<header class="list-page-header">
		<h1 class="list-page-title">Texts</h1>
		<a href="/texts/new" class="btn btn-primary">+ New text</a>
	</header>

	<div class="list-toolbar">
		<div class="search-wrap">
			<Search size={14} />
			<input
				class="search-input"
				type="text"
				placeholder="Search texts…"
				value={search}
				oninput={(e) => {
					search = e.currentTarget.value;
				}}
			/>
		</div>

		<div class="texts-filters">
			<select
				class="filter-select"
				value={`${sortBy}:${sortDesc}`}
				onchange={(e) => {
					const [field, desc] = e.currentTarget.value.split(':');
					sortBy = field as TextSortField;
					sortDesc = desc === 'true';
					page = 1;
				}}
			>
				<option value="CREATED_AT:true">Newest first</option>
				<option value="CREATED_AT:false">Oldest first</option>
				<option value="UPDATED_AT:true">Recently updated</option>
				<option value="TITLE:false">Title A→Z</option>
			</select>

			<select
				class="filter-select"
				value={dialect}
				onchange={(e) => {
					dialect = e.currentTarget.value as Dialect | '';
					page = 1;
				}}
			>
				<option value="">All dialects</option>
				<option value="MSA">MSA</option>
				<option value="TUNISIAN">Tunisian</option>
				<option value="MOROCCAN">Moroccan</option>
				<option value="EGYPTIAN">Egyptian</option>
				<option value="GULF">Gulf</option>
				<option value="LEVANTINE">Levantine</option>
				<option value="IRAQI">Iraqi</option>
			</select>

			<select
				class="filter-select"
				value={difficulty}
				onchange={(e) => {
					difficulty = e.currentTarget.value as Difficulty | '';
					page = 1;
				}}
			>
				<option value="">All difficulties</option>
				<option value="BEGINNER">Beginner</option>
				<option value="INTERMEDIATE">Intermediate</option>
				<option value="ADVANCED">Advanced</option>
			</select>

			<input
				class="filter-select"
				type="text"
				placeholder="Filter by tag…"
				value={tag}
				oninput={(e) => {
					tag = e.currentTarget.value;
					page = 1;
				}}
			/>
		</div>
	</div>

	{#if texts.isPending}
		<p class="results-meta">Loading…</p>
	{:else if texts.isError}
		<p class="results-meta">Could not load texts — is the backend running?</p>
	{:else if (texts.data?.items ?? []).length === 0}
		<div class="list-empty">
			<BookOpen size={32} class="list-empty-icon" />
			<p class="list-empty-label">
				{hasActiveFilters ? 'No texts match your filter.' : 'Add your first text'}
			</p>
			{#if !hasActiveFilters}
				<a href="/texts/new" class="btn">Add your first text</a>
			{/if}
		</div>
	{:else}
		<p class="results-meta">
			{total} text{total === 1 ? '' : 's'}
		</p>

		<div class="texts-grid stagger-children">
			{#each texts.data?.items ?? [] as text (text.id)}
				<a class="text-card" href="/texts/{text.id}">
					<div class="text-card-title">{text.title}</div>
					{#if text.comments}
						<div class="text-card-desc">{text.comments}</div>
					{/if}
					<div class="text-card-badges">
						<span class="chip c-coral" style="font-size:0.65rem;padding:0.15rem 0.6rem;">{formatEnum(text.difficulty)}</span>
						<span class="chip c-cerulean" style="font-size:0.65rem;padding:0.15rem 0.6rem;">{text.dialect}</span>
						{#each text.tags as tag}
							<span class="chip c-muted" style="font-size:0.65rem;padding:0.15rem 0.6rem;">{tag}</span>
						{/each}
					</div>
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
