<script lang="ts">
import { Search } from 'lucide-svelte';
import type { Dialect, Difficulty, MasteryLevel, PartOfSpeech } from '$lib/api/types.gen';
import { useWords } from '$lib/stores/words';

const PAGE_SIZE = 30;

let search = $state('');
let debouncedSearch = $state('');
let dialect = $state<Dialect | ''>('');
let difficulty = $state<Difficulty | ''>('');
let partOfSpeech = $state<PartOfSpeech | ''>('');
let masteryLevel = $state<MasteryLevel | ''>('');
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
	partOfSpeech: partOfSpeech || undefined,
	masteryLevel: masteryLevel || undefined,
	page,
	size: PAGE_SIZE,
});

const words = useWords(() => filters);

const total = $derived(words.data?.total ?? 0);
const totalPages = $derived(Math.max(1, Math.ceil(total / PAGE_SIZE)));

const hasActiveFilters = $derived(
	!!debouncedSearch.trim() || !!dialect || !!difficulty || !!partOfSpeech || !!masteryLevel
);

function formatEnum(value: string): string {
	return value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();
}
</script>

<div class="list-page">
	<header class="list-page-header">
		<h1 class="list-page-title">Words</h1>
		<a href="/words/new" class="btn btn-primary">+ New word</a>
	</header>

	<div class="list-toolbar">
		<div class="search-wrap">
			<Search size={14} />
			<input
				class="search-input"
				type="text"
				placeholder="Search words…"
				value={search}
				oninput={(e) => {
					search = e.currentTarget.value;
				}}
			/>
		</div>

		<div class="words-filters">
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

			<select
				class="filter-select"
				value={partOfSpeech}
				onchange={(e) => {
					partOfSpeech = e.currentTarget.value as PartOfSpeech | '';
					page = 1;
				}}
			>
				<option value="">All parts of speech</option>
				<option value="NOUN">Noun</option>
				<option value="VERB">Verb</option>
				<option value="ADJECTIVE">Adjective</option>
				<option value="ADVERB">Adverb</option>
				<option value="PREPOSITION">Preposition</option>
				<option value="PARTICLE">Particle</option>
				<option value="INTERJECTION">Interjection</option>
				<option value="CONJUNCTION">Conjunction</option>
				<option value="PRONOUN">Pronoun</option>
				<option value="UNKNOWN">Unknown</option>
			</select>

			<select
				class="filter-select"
				value={masteryLevel}
				onchange={(e) => {
					masteryLevel = e.currentTarget.value as MasteryLevel | '';
					page = 1;
				}}
			>
				<option value="">All mastery levels</option>
				<option value="NEW">New</option>
				<option value="LEARNING">Learning</option>
				<option value="KNOWN">Known</option>
				<option value="MASTERED">Mastered</option>
			</select>
		</div>
	</div>

	{#if words.isPending}
		<p class="results-meta">Loading…</p>
	{:else if words.isError}
		<p class="results-meta">Could not load words — is the backend running?</p>
	{:else if (words.data?.items ?? []).length === 0}
		<div class="list-empty">
			<span class="list-empty-icon">ك</span>
			<p class="list-empty-label">
				{hasActiveFilters ? 'No words match your filter.' : 'Add your first word'}
			</p>
			{#if !hasActiveFilters}
				<a href="/words/new" class="btn">Add your first word</a>
			{/if}
		</div>
	{:else}
		<p class="results-meta">
			{total} word{total === 1 ? '' : 's'}
		</p>

		<div class="words-grid stagger-children">
			{#each words.data?.items ?? [] as word (word.id)}
				<a class="word-card wcard-{word.masteryLevel.toLowerCase()}" href="/words/{word.id}">
					<div class="word-card-ar">{word.arabicText}</div>
					{#if word.transliteration}
						<div class="word-card-tr">{word.transliteration}</div>
					{/if}
					{#if word.translation}
						<div class="word-card-en">{word.translation}</div>
					{/if}
					<div class="word-card-badges">
						<span class="chip chip-sm c-coral">{formatEnum(word.masteryLevel)}</span>
						<span class="chip chip-sm c-cerulean">{word.dialect}</span>
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
