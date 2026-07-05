<script lang="ts">
import { BookOpen, Search } from 'lucide-svelte';
import { browser } from '$app/environment';
import { goto } from '$app/navigation';
import type { Dialect, Difficulty } from '$lib/api/types.gen';
import ViewToggle from '$lib/components/ViewToggle.svelte';
import { type TextSortField, useTexts } from '$lib/stores/texts';

const PAGE_SIZE = 20;
const STORAGE_KEY = 'qalam:view:texts';

let view = $state<'grid' | 'table'>(
	browser ? ((localStorage.getItem(STORAGE_KEY) as 'grid' | 'table' | null) ?? 'table') : 'table'
);

function setView(v: 'grid' | 'table') {
	view = v;
	if (browser) localStorage.setItem(STORAGE_KEY, v);
}

let search = $state('');
let debouncedSearch = $state('');
let dialect = $state<Dialect | ''>('');
let difficulty = $state<Difficulty | ''>('');
let tag = $state('');
let sortBy = $state<TextSortField>('UPDATED_AT');
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

function formatDate(iso: string): string {
	return new Date(iso).toLocaleDateString('en-GB', {
		day: 'numeric',
		month: 'short',
		year: 'numeric',
	});
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
		<div class="results-meta-row">
			<p class="results-meta" style="margin-bottom:0">
				{total} text{total === 1 ? '' : 's'}
			</p>
			<ViewToggle {view} onchange={setView} />
		</div>

		{#if view === 'grid'}
			<div class="texts-grid stagger-children">
				{#each texts.data?.items ?? [] as text (text.id)}
					<a class="text-card" href="/texts/{text.id}">
						<div class="text-card-title">{text.title}</div>
						{#if text.comments}
							<div class="text-card-desc">{text.comments}</div>
						{/if}
						<div class="text-card-badges">
							<span class="chip chip-sm c-coral">{formatEnum(text.difficulty)}</span>
							<span class="chip chip-sm c-cerulean">{text.dialect}</span>
							{#each text.tags as t}
								<span class="chip chip-sm c-muted">{t}</span>
							{/each}
						</div>
					</a>
				{/each}
			</div>
		{:else}
			<table class="list-table texts-table">
				<thead>
					<tr>
						<th>Title</th>
						<th>Dialect</th>
						<th>Difficulty</th>
						<th>Tags</th>
						<th class="list-table-th-right">Updated</th>
					</tr>
				</thead>
				<tbody>
					{#each texts.data?.items ?? [] as text (text.id)}
						<tr
							class="list-table-row"
							onclick={() => goto(`/texts/${text.id}`)}
							onkeydown={(e) => e.key === 'Enter' && goto(`/texts/${text.id}`)}
							tabindex="0"
							role="link"
							aria-label={text.title}
						>
							<td class="list-table-td-main">
								<div class="list-table-title">{text.title}</div>
								{#if text.comments}
									<div class="list-table-sub">{text.comments}</div>
								{/if}
							</td>
							<td>
								<span class="chip chip-sm c-cerulean">{text.dialect}</span>
							</td>
							<td>
								<span class="chip chip-sm c-coral">{formatEnum(text.difficulty)}</span>
							</td>
							<td>
								{#if text.tags.length}
									<div class="list-table-badges">
										{#each text.tags as t}
											<span class="chip chip-sm c-muted">{t}</span>
										{/each}
									</div>
								{:else}
									<span class="list-table-empty-cell">—</span>
								{/if}
							</td>
							<td class="list-table-td-right">
								<span class="list-table-date">{formatDate(text.updatedAt)}</span>
							</td>
						</tr>
					{/each}
				</tbody>
			</table>
		{/if}

		{#if totalPages > 1}
			<div class="pagination">
				<button class="btn" disabled={page === 1} onclick={() => (page -= 1)}>Previous</button>
				<span class="pagination-info">Page {page} of {totalPages}</span>
				<button class="btn" disabled={page === totalPages} onclick={() => (page += 1)}>Next</button>
			</div>
		{/if}
	{/if}
</div>
