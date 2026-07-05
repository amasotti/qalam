<script lang="ts">
import { untrack } from 'svelte';
import type { DictionaryLookupItemResponse, PartOfSpeech } from '$lib/api/types.gen';
import { Button } from '$lib/components/ui/button';
import { Input } from '$lib/components/ui/input';
import { useAnalyzeWord, useAsdLookup, useCreateWord } from '$lib/stores/words';

interface Props {
	arabicText: string;
	onCreated: (wordId: string) => void;
	onCancel: () => void;
}

let { arabicText, onCreated, onCancel }: Props = $props();

const analyzeWord = useAnalyzeWord();
const asdLookup = useAsdLookup();
const createWord = useCreateWord();

let createArabicText = $state(untrack(() => arabicText));
let transliteration = $state('');
let translation = $state('');
let partOfSpeech = $state<PartOfSpeech>('UNKNOWN');
let lookupError = $state<string | null>(null);
let lookupItems = $state<DictionaryLookupItemResponse[]>([]);
let lookupQuery = $state('');

const posOptions: PartOfSpeech[] = [
	'UNKNOWN',
	'NOUN',
	'VERB',
	'ADJECTIVE',
	'ADVERB',
	'PREPOSITION',
	'PARTICLE',
	'INTERJECTION',
	'CONJUNCTION',
	'PRONOUN',
];

$effect(() => {
	if (arabicText === createArabicText) return;
	createArabicText = arabicText;
	lookupError = null;
	lookupItems = [];
	lookupQuery = '';
});

async function handleAnalyze() {
	const result = await analyzeWord.mutateAsync(createArabicText);
	transliteration = result.transliteration ?? '';
	translation = result.translation ?? '';
	partOfSpeech = (result.partOfSpeech as PartOfSpeech | null) ?? 'UNKNOWN';
}

async function handleAsdLookup() {
	const query = createArabicText.trim();
	if (!query) return;

	lookupError = null;
	lookupItems = [];
	lookupQuery = query;

	try {
		const result = await asdLookup.mutateAsync(query);
		lookupItems = result.items;
		if (result.items.length === 0) lookupError = 'No ASD results';
	} catch (err) {
		lookupError = err instanceof Error ? err.message : 'Could not look up this word';
	}
}

function applyAsdSuggestion(item: DictionaryLookupItemResponse) {
	createArabicText = item.arabicText;
	transliteration = item.transliteration ?? '';
	translation = item.translation ?? '';
	lookupItems = [];
	lookupError = null;
}

async function handleSubmit(e: SubmitEvent) {
	e.preventDefault();
	const result = await createWord.mutateAsync({
		arabicText: createArabicText,
		transliteration: transliteration.trim() || null,
		translation: translation.trim() || null,
		partOfSpeech,
	});
	onCreated(result.id);
}
</script>

<form class="quick-add-form" onsubmit={handleSubmit}>
	<div class="quick-add-arabic arabic-text">{createArabicText}</div>

	<div class="quick-add-ai">
		<Button
			type="button"
			variant="outline"
			size="sm"
			disabled={asdLookup.isPending || !createArabicText.trim()}
			onclick={handleAsdLookup}
		>
			{asdLookup.isPending ? 'Looking…' : 'Lookup ASD'}
		</Button>
		<Button
			type="button"
			variant="outline"
			size="sm"
			disabled={analyzeWord.isPending}
			onclick={handleAnalyze}
		>
			{analyzeWord.isPending ? 'Analyzing…' : 'Analyze with AI'}
		</Button>
		{#if analyzeWord.isError}
			<span class="quick-add-error">AI unavailable</span>
		{/if}
	</div>

	{#if lookupError}
		<p class="quick-add-error">{lookupError}</p>
	{/if}

	{#if lookupItems.length > 0}
		<div class="dictionary-lookup-results dictionary-lookup-results-compact" aria-live="polite">
			<p class="form-hint">ASD suggestions for {lookupQuery}</p>
			{#each lookupItems.slice(0, 3) as item (item.externalId)}
				<button
					class:dictionary-lookup-result-exact={item.hasExactWordMatch}
					class="dictionary-lookup-result"
					type="button"
					onclick={() => applyAsdSuggestion(item)}
					disabled={createWord.isPending}
				>
					<span class="arabic dictionary-lookup-arabic">{item.arabicText}</span>
					<span class="dictionary-lookup-main">
						{#if item.transliteration}
							<span>{item.transliteration}</span>
						{/if}
						{#if item.translation}
							<span>{item.translation}</span>
						{/if}
						{#if item.plural}
							<span class="form-hint">plural: {item.plural.arabicText}</span>
						{/if}
					</span>
				</button>
			{/each}
		</div>
	{/if}

	<div class="quick-add-field">
		<label class="quick-add-label" for="qa-translit">Transliteration</label>
		<Input id="qa-translit" bind:value={transliteration} placeholder="e.g. kataba" />
	</div>

	<div class="quick-add-field">
		<label class="quick-add-label" for="qa-translation">Translation</label>
		<Input id="qa-translation" bind:value={translation} placeholder="English meaning" />
	</div>

	<div class="quick-add-field">
		<label class="quick-add-label" for="qa-pos">Part of speech</label>
		<select id="qa-pos" class="quick-add-select" bind:value={partOfSpeech}>
			{#each posOptions as pos}
				<option value={pos}>{pos.charAt(0) + pos.slice(1).toLowerCase()}</option>
			{/each}
		</select>
	</div>

	<div class="quick-add-actions">
		<Button type="submit" size="sm" disabled={createWord.isPending || !createArabicText.trim()}>
			{createWord.isPending ? 'Saving…' : 'Add to vocabulary'}
		</Button>
		<Button type="button" variant="ghost" size="sm" onclick={onCancel}>Cancel</Button>
	</div>
</form>
