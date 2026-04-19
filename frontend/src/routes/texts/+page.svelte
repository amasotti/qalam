<script lang="ts">
import { BookOpen, Plus, Search } from 'lucide-svelte';
import type { Dialect, Difficulty } from '$lib/api/types.gen';
import { Badge } from '$lib/components/ui/badge';
import { Button } from '$lib/components/ui/button';
import { useTexts } from '$lib/stores/texts';

const PAGE_SIZE = 20;

let search = $state('');
let debouncedSearch = $state('');
let dialect = $state<Dialect | ''>('');
let difficulty = $state<Difficulty | ''>('');
let tag = $state('');
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

<div class="page-texts page-enter">
	<header class="texts-page-header">
		<h1 class="texts-page-title">Texts</h1>
		<Button href="/texts/new">
			<Plus size={14} />
			New text
		</Button>
	</header>

	<div class="texts-toolbar">
		<div class="texts-search-wrap">
			<Search size={14} />
			<input
				class="texts-search"
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
				class="texts-filter-select"
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
				class="texts-filter-select"
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
				class="texts-filter-select"
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
		<p class="texts-results-meta">Loading…</p>
	{:else if texts.isError}
		<p class="texts-results-meta">Could not load texts — is the backend running?</p>
	{:else if (texts.data?.items ?? []).length === 0}
		<div class="texts-empty">
			<BookOpen size={32} style="color: hsl(var(--muted-foreground)); opacity: 0.5;" />
			<p class="texts-empty-label">
				{hasActiveFilters ? 'No texts match your filter.' : 'Add your first text'}
			</p>
			{#if !hasActiveFilters}
				<Button href="/texts/new" variant="outline">Add your first text</Button>
			{/if}
		</div>
	{:else}
		<p class="texts-results-meta">
			{total} text{total === 1 ? '' : 's'}
		</p>

		<div class="texts-grid stagger-children">
			{#each texts.data?.items ?? [] as text (text.id)}
				<a class="text-card" href="/texts/{text.id}">
					<div class="text-card-title">{text.title}</div>
					{#if text.body}
						<div class="text-card-preview arabic-text">{text.body.slice(0, 120)}{text.body.length > 120 ? '…' : ''}</div>
					{/if}
					<div class="text-card-badges">
						<Badge class="difficulty-{text.difficulty.toLowerCase()}">
							{formatEnum(text.difficulty)}
						</Badge>
						<Badge class="dialect-{text.dialect.toLowerCase()}">
							{text.dialect}
						</Badge>
						{#each text.tags.slice(0, 3) as t}
							<Badge variant="outline">{t}</Badge>
						{/each}
					</div>
				</a>
			{/each}
		</div>

		{#if totalPages > 1}
			<div class="texts-pagination">
				<Button
					variant="outline"
					size="sm"
					disabled={page === 1}
					onclick={() => (page -= 1)}
				>
					Previous
				</Button>
				<span class="texts-pagination-info">Page {page} of {totalPages}</span>
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

<style>
.page-texts {
	max-width: 900px;
	margin: 0 auto;
	padding: 2rem 1.5rem;
}

.texts-page-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 1.5rem;
}

.texts-page-title {
	font-size: 1.5rem;
	font-weight: 600;
	letter-spacing: -0.01em;
}

.texts-toolbar {
	display: flex;
	flex-direction: column;
	gap: 0.75rem;
	margin-bottom: 1.5rem;
}

.texts-search-wrap {
	display: flex;
	align-items: center;
	gap: 0.5rem;
	border: 1px solid hsl(var(--border));
	border-radius: 0.375rem;
	padding: 0.375rem 0.75rem;
	background: hsl(var(--background));
	color: hsl(var(--muted-foreground));
}

.texts-search {
	flex: 1;
	border: none;
	outline: none;
	background: transparent;
	font-size: 0.875rem;
	color: hsl(var(--foreground));
}

.texts-filters {
	display: flex;
	gap: 0.5rem;
	flex-wrap: wrap;
}

.texts-filter-select {
	border: 1px solid hsl(var(--border));
	border-radius: 0.375rem;
	padding: 0.25rem 0.5rem;
	font-size: 0.8125rem;
	background: hsl(var(--background));
	color: hsl(var(--foreground));
	cursor: pointer;
	min-width: 140px;
}

.texts-results-meta {
	font-size: 0.8125rem;
	color: hsl(var(--muted-foreground));
	margin-bottom: 0.75rem;
}

.texts-empty {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 0.75rem;
	padding: 4rem 0;
	color: hsl(var(--muted-foreground));
}

.texts-empty-label {
	font-size: 0.9375rem;
}

.texts-grid {
	display: grid;
	grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
	gap: 1rem;
	margin-bottom: 1.5rem;
}

.text-card {
	display: flex;
	flex-direction: column;
	gap: 0.5rem;
	padding: 1rem;
	border: 1px solid hsl(var(--border));
	border-radius: 0.5rem;
	background: hsl(var(--card));
	text-decoration: none;
	color: inherit;
	transition: border-color 0.15s, box-shadow 0.15s;
}

.text-card:hover {
	border-color: hsl(var(--primary) / 0.4);
	box-shadow: 0 1px 6px hsl(var(--primary) / 0.08);
}

.text-card-title {
	font-weight: 600;
	font-size: 0.9375rem;
}

.text-card-preview {
	font-size: 1rem;
	line-height: 1.6;
	color: hsl(var(--muted-foreground));
	display: -webkit-box;
	-webkit-line-clamp: 2;
	line-clamp: 2;
	-webkit-box-orient: vertical;
	overflow: hidden;
}

.text-card-badges {
	display: flex;
	flex-wrap: wrap;
	gap: 0.25rem;
	margin-top: auto;
}

.texts-pagination {
	display: flex;
	align-items: center;
	gap: 0.75rem;
	justify-content: center;
}

.texts-pagination-info {
	font-size: 0.8125rem;
	color: hsl(var(--muted-foreground));
}
</style>
