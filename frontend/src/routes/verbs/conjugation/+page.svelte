<script lang="ts">
import { goto } from '$app/navigation';
import { page } from '$app/state';
import type { ConjugationResponse } from '$lib/api/types.gen';
import ConjugationTable from '$lib/components/conjugation/ConjugationTable.svelte';
import VerbPicker from '$lib/components/conjugation/VerbPicker.svelte';
import { useAdHocConjugation, useConjugation } from '$lib/stores/conjugation';

// If a wordId is passed as query param, load conjugation for that word
const wordId = $derived(page.url.searchParams.get('wordId'));
const wordQuery = useConjugation(() => wordId);
let mode = $state<'saved' | 'adhoc'>('adhoc');

$effect(() => {
	if (wordId) mode = 'saved';
});

// Ad-hoc conjugation state
let rootInput = $state('');
let verbForm = $state('I');
let pastPattern = $state('fa3ala');
let presentPattern = $state('yaf3ulu');
let weaknessType = $state('SOUND');

const adHocMutation = useAdHocConjugation();

function parseRootLetters(input: string): string[] {
	return input
		.trim()
		.split(/[\s\-.]+/)
		.filter((l) => l.length > 0);
}

function handleCompute() {
	const letters = parseRootLetters(rootInput);
	if (letters.length < 3) return;
	adHocMutation.mutate({
		rootLetters: letters,
		verbForm: verbForm as 'I',
		pastPattern: verbForm === 'I' ? pastPattern : undefined,
		presentPattern: verbForm === 'I' ? presentPattern : undefined,
		weaknessType: weaknessType as 'SOUND',
		dialect: 'MSA',
	});
}

function selectSavedVerb(word: { id: string }) {
	mode = 'saved';
	void goto(`/verbs/conjugation?wordId=${word.id}`);
}

function selectMode(nextMode: 'saved' | 'adhoc') {
	mode = nextMode;
	if (nextMode === 'adhoc') void goto('/verbs/conjugation');
}

// Determine which result to display
const result: ConjugationResponse | undefined = $derived(
	wordId ? wordQuery.data : adHocMutation.data
);
const isPending = $derived(wordId ? wordQuery.isPending : adHocMutation.isPending);
const error = $derived(wordId ? wordQuery.error : adHocMutation.error);

const TABLE_KEYS: { key: string; title: string }[] = [
	{ key: 'present_active', title: 'Present Active' },
	{ key: 'past_active', title: 'Past Active' },
	{ key: 'present_passive', title: 'Present Passive' },
	{ key: 'past_passive', title: 'Past Passive' },
];

const VERB_FORMS = ['I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X'];
const WEAKNESS_TYPES = ['SOUND', 'ASSIMILATED', 'HOLLOW', 'GEMINATE', 'DEFECTIVE', 'DOUBLY_WEAK'];
const PAST_PATTERNS = ['fa3ala', 'fa3ila', 'fa3ula'];
const PRESENT_PATTERNS = ['yaf3ulu', 'yaf3ilu', 'yaf3alu'];
</script>

<div class="conj-page">
	<h1 class="conj-page-title">Verb Conjugation</h1>
	<div class="conj-mode-toggle" role="group" aria-label="Conjugation source">
		<button class:active={mode === 'saved'} type="button" onclick={() => selectMode('saved')}>Saved verb</button>
		<button class:active={mode === 'adhoc'} type="button" onclick={() => selectMode('adhoc')}>Ad-hoc root</button>
	</div>

	{#if mode === 'saved'}
		<div class="conj-header">
			<VerbPicker onselect={selectSavedVerb} />
		</div>
	{:else}
		<!-- Ad-hoc input form -->
		<div class="conj-header">
			<form class="conj-adhoc-form" onsubmit={(e) => { e.preventDefault(); handleCompute(); }}>
				<div class="conj-adhoc-row">
					<label class="conj-adhoc-field">
						<span class="conj-adhoc-label">Root letters</span>
						<input
							type="text"
							class="conj-adhoc-input arabic"
							dir="rtl"
							placeholder="ك ت ب"
							bind:value={rootInput}
						/>
						<span class="conj-adhoc-hint">Space-separated (e.g. ك ت ب)</span>
					</label>

					<label class="conj-adhoc-field">
						<span class="conj-adhoc-label">Form</span>
						<select class="conj-adhoc-select" bind:value={verbForm}>
							{#each VERB_FORMS as f}
								<option value={f}>{f}</option>
							{/each}
						</select>
					</label>

					<label class="conj-adhoc-field">
						<span class="conj-adhoc-label">Weakness</span>
						<select class="conj-adhoc-select" bind:value={weaknessType}>
							{#each WEAKNESS_TYPES as w}
								<option value={w}>{w}</option>
							{/each}
						</select>
					</label>
				</div>

				{#if verbForm === 'I'}
					<div class="conj-adhoc-row">
						<label class="conj-adhoc-field">
							<span class="conj-adhoc-label">Past pattern</span>
							<select class="conj-adhoc-select" bind:value={pastPattern}>
								{#each PAST_PATTERNS as p}
									<option value={p}>{p}</option>
								{/each}
							</select>
						</label>

						<label class="conj-adhoc-field">
							<span class="conj-adhoc-label">Present pattern</span>
							<select class="conj-adhoc-select" bind:value={presentPattern}>
								{#each PRESENT_PATTERNS as p}
									<option value={p}>{p}</option>
								{/each}
							</select>
						</label>
					</div>
				{/if}

				<button type="submit" class="conj-adhoc-btn" disabled={parseRootLetters(rootInput).length < 3}>
					Conjugate
				</button>
			</form>
		</div>
	{/if}

	{#if isPending}
		<div class="conj-empty">Loading conjugation...</div>
	{:else if error}
		<div class="conj-empty">Error: {error.message ?? 'Failed to load conjugation'}</div>
	{:else if result}
		<!-- Result header -->
		<div class="conj-header">
			{#if result.word}
				<div class="conj-root-display">{result.word.arabicText}</div>
				{#if result.word.translation}
					<div style="color: var(--ink-mid); margin-top: 0.25rem;">{result.word.translation}</div>
				{/if}
			{:else}
				<div class="conj-root-display">{result.root.letters.join(' ')}</div>
			{/if}
			<div class="conj-meta">
				<span class="conj-meta-badge">Form {result.verbDetails.verbForm}</span>
				<span class="conj-meta-badge">{result.verbDetails.weaknessType}</span>
				{#if result.verbDetails.pastPattern}
					<span class="conj-meta-badge">{result.verbDetails.pastPattern}</span>
				{/if}
				{#if result.verbDetails.presentPattern}
					<span class="conj-meta-badge">{result.verbDetails.presentPattern}</span>
				{/if}
			</div>
		</div>

		<!-- Segment color legend -->
		<div class="conj-legend">
			<span class="conj-legend-item"><span class="conj-legend-swatch conj-legend-swatch--prefix"></span> Prefix</span>
			<span class="conj-legend-item"><span class="conj-legend-swatch conj-legend-swatch--root"></span> Root</span>
			<span class="conj-legend-item"><span class="conj-legend-swatch conj-legend-swatch--vowel"></span> Pattern vowel</span>
			<span class="conj-legend-item"><span class="conj-legend-swatch conj-legend-swatch--suffix"></span> Suffix</span>
		</div>

		<!-- 2×2 conjugation tables -->
		<div class="conj-tables-grid">
			{#each TABLE_KEYS as { key, title }}
				{@const forms = result.conjugations[key]}
				{#if forms}
					<ConjugationTable {title} {forms} />
				{/if}
			{/each}
		</div>
	{:else if !wordId}
		<div class="conj-empty">Enter root letters and press Conjugate</div>
	{/if}
</div>
