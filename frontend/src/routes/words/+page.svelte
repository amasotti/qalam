<script lang="ts">
import { Plus, Search } from 'lucide-svelte';
import type { Dialect, Difficulty, MasteryLevel, PartOfSpeech } from '$lib/api/types.gen';
import { Badge } from '$lib/components/ui/badge';
import { Button } from '$lib/components/ui/button';
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

<div class="page-words page-enter">
	<header class="words-page-header">
		<h1 class="words-page-title">Words</h1>
		<Button href="/words/new">
			<Plus size={14} />
			New word
		</Button>
	</header>

	<div class="words-toolbar">
		<div class="words-search-wrap">
			<Search size={14} />
			<input
				class="words-search"
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
				class="words-filter-select"
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
				class="words-filter-select"
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
				class="words-filter-select"
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
				class="words-filter-select"
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
		<p class="words-results-meta">Loading…</p>
	{:else if words.isError}
		<p class="words-results-meta">Could not load words — is the backend running?</p>
	{:else if (words.data?.items ?? []).length === 0}
		<div class="words-empty">
			<span class="words-empty-icon">ك</span>
			<p class="words-empty-label">
				{hasActiveFilters ? 'No words match your filter.' : 'Add your first word'}
			</p>
			{#if !hasActiveFilters}
				<Button href="/words/new" variant="outline">Add your first word</Button>
			{/if}
		</div>
	{:else}
		<p class="words-results-meta">
			{total} word{total === 1 ? '' : 's'}
		</p>

		<div class="words-grid stagger-children">
			{#each words.data?.items ?? [] as word (word.id)}
				<a class="word-card" href="/words/{word.id}">
					<div class="word-card-arabic arabic-display">{word.arabicText}</div>
					{#if word.transliteration}
						<div class="word-card-transliteration">{word.transliteration}</div>
					{/if}
					{#if word.translation}
						<div class="word-card-translation">{word.translation}</div>
					{/if}
					<div class="word-card-badges">
						<Badge class="mastery-{word.masteryLevel.toLowerCase()}">
							{formatEnum(word.masteryLevel)}
						</Badge>
						<Badge class="difficulty-{word.difficulty.toLowerCase()}">
							{formatEnum(word.difficulty)}
						</Badge>
						<Badge class="dialect-{word.dialect.toLowerCase()}">
							{word.dialect}
						</Badge>
					</div>
				</a>
			{/each}
		</div>

		{#if totalPages > 1}
			<div class="words-pagination">
				<Button
					variant="outline"
					size="sm"
					disabled={page === 1}
					onclick={() => (page -= 1)}
				>
					Previous
				</Button>
				<span class="words-pagination-info">Page {page} of {totalPages}</span>
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
