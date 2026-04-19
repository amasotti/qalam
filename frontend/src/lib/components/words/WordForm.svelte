<script lang="ts">
import { untrack } from 'svelte';
import type {
	CreateWordRequest,
	Dialect,
	Difficulty,
	PartOfSpeech,
	RootResponse,
	UpdateWordRequest,
	WordAutocompleteResponse,
} from '$lib/api/types.gen';
import { Button } from '$lib/components/ui/button';
import { useAllRoots } from '$lib/stores/roots';
import { useWordAutocomplete } from '$lib/stores/words';

interface Props {
	initial?: Partial<{
		arabicText: string;
		transliteration: string | null;
		translation: string | null;
		partOfSpeech: PartOfSpeech;
		dialect: Dialect;
		difficulty: Difficulty;
		pronunciationUrl: string | null;
		rootId: string | null;
		derivedFromId: string | null;
	}>;
	isEdit?: boolean;
	selfId?: string;
	isPending?: boolean;
	onSubmit: (req: CreateWordRequest | UpdateWordRequest) => Promise<void>;
	onCancel: () => void;
}

let {
	initial = {},
	isEdit = false,
	selfId,
	isPending = false,
	onSubmit,
	onCancel,
}: Props = $props();

// One-shot seeds — edits not synced back to parent
let arabicText = $state(untrack(() => initial.arabicText ?? ''));
let transliteration = $state(untrack(() => initial.transliteration ?? ''));
let translation = $state(untrack(() => initial.translation ?? ''));
let partOfSpeech = $state<PartOfSpeech>(untrack(() => initial.partOfSpeech ?? 'UNKNOWN'));
let dialect = $state<Dialect>(untrack(() => initial.dialect ?? 'MSA'));
let difficulty = $state<Difficulty>(untrack(() => initial.difficulty ?? 'BEGINNER'));
let pronunciationUrl = $state(untrack(() => initial.pronunciationUrl ?? ''));
let pronunciationWasAutofilled = $state(false);
let rootId = $state<string | null>(untrack(() => initial.rootId ?? null));
let derivedFromId = $state<string | null>(untrack(() => initial.derivedFromId ?? null));

let submitError = $state<string | null>(null);

// Auto-fill Forvo pronunciation URL when arabic text is available
$effect(() => {
	const arabic = arabicText.trim();
	if (arabic && (pronunciationUrl === '' || pronunciationWasAutofilled)) {
		pronunciationUrl = `https://forvo.com/search/${encodeURIComponent(arabic)}`;
		pronunciationWasAutofilled = true;
	} else if (!arabic && pronunciationWasAutofilled) {
		pronunciationUrl = '';
		pronunciationWasAutofilled = false;
	}
});

// Root selector
const allRoots = useAllRoots();
let rootFilter = $state('');

const filteredRoots = $derived.by((): RootResponse[] => {
	const roots = allRoots.data ?? [];
	const f = rootFilter.trim().toLowerCase();
	if (!f) return roots;
	return roots.filter(
		(r: RootResponse) =>
			r.normalizedForm.toLowerCase().includes(f) ||
			(r.meaning ?? '').toLowerCase().includes(f) ||
			r.displayForm.includes(f),
	);
});

// DerivedFrom autocomplete
let derivedFromQuery = $state('');
let derivedFromDebounced = $state('');
let derivedFromSelected = $state<WordAutocompleteResponse | null>(
	untrack(() =>
		initial.derivedFromId
			? { id: initial.derivedFromId, arabicText: '', translation: null }
			: null,
	),
);
let showAutocompleteDropdown = $state(false);

$effect(() => {
	const val = derivedFromQuery;
	const timer = setTimeout(() => {
		derivedFromDebounced = val;
	}, 300);
	return () => clearTimeout(timer);
});

const autocompleteResults = useWordAutocomplete(() => derivedFromDebounced);

const visibleAutocompleteResults = $derived.by((): WordAutocompleteResponse[] => {
	if (!showAutocompleteDropdown) return [];
	const results = autocompleteResults.data ?? [];
	if (selfId) return results.filter((w: WordAutocompleteResponse) => w.id !== selfId);
	return results;
});

function selectDerivedFrom(word: WordAutocompleteResponse) {
	derivedFromSelected = word;
	derivedFromId = word.id;
	derivedFromQuery = word.arabicText;
	showAutocompleteDropdown = false;
}

function clearDerivedFrom() {
	derivedFromSelected = null;
	derivedFromId = null;
	derivedFromQuery = '';
	derivedFromDebounced = '';
	showAutocompleteDropdown = false;
}

async function handleSubmit(e: SubmitEvent) {
	e.preventDefault();
	submitError = null;
	try {
		if (isEdit) {
			await onSubmit({
				transliteration: transliteration.trim() || null,
				translation: translation.trim() || null,
				partOfSpeech,
				dialect,
				difficulty,
				pronunciationUrl: pronunciationUrl.trim() || null,
				rootId: rootId || null,
				derivedFromId: derivedFromId || null,
			} satisfies UpdateWordRequest);
		} else {
			if (!arabicText.trim()) return;
			await onSubmit({
				arabicText: arabicText.trim(),
				transliteration: transliteration.trim() || null,
				translation: translation.trim() || null,
				partOfSpeech,
				dialect,
				difficulty,
				pronunciationUrl: pronunciationUrl.trim() || null,
				rootId: rootId || null,
				derivedFromId: derivedFromId || null,
			} satisfies CreateWordRequest);
		}
	} catch (err) {
		submitError = err instanceof Error ? err.message : 'Something went wrong';
	}
}
</script>

<form class="word-form" onsubmit={handleSubmit} novalidate>
	<!-- Arabic text -->
	{#if isEdit}
		<div class="word-form-field">
			<p class="word-form-label">Arabic</p>
			<p class="word-form-arabic-display arabic-input">{arabicText}</p>
		</div>
	{:else}
		<div class="word-form-field">
			<label class="word-form-label" for="word-arabic">Arabic *</label>
			<input
				id="word-arabic"
				class="word-form-input arabic-input"
				type="text"
				placeholder="أدخل الكلمة بالعربية"
				bind:value={arabicText}
				autocomplete="off"
				spellcheck="false"
				disabled={isPending}
				required
				dir="rtl"
			/>
		</div>
	{/if}

	<!-- Transliteration -->
	<div class="word-form-field">
		<label class="word-form-label" for="word-transliteration">Transliteration</label>
		<input
			id="word-transliteration"
			class="word-form-input"
			type="text"
			placeholder="e.g. kataba"
			bind:value={transliteration}
			disabled={isPending}
		/>
	</div>

	<!-- Translation -->
	<div class="word-form-field">
		<label class="word-form-label" for="word-translation">Translation</label>
		<input
			id="word-translation"
			class="word-form-input"
			type="text"
			placeholder="e.g. to write"
			bind:value={translation}
			disabled={isPending}
		/>
	</div>

	<!-- POS / Dialect / Difficulty — 3-col grid -->
	<div class="word-form-section">
		<div class="word-form-field">
			<label class="word-form-label" for="word-pos">Part of speech</label>
			<select id="word-pos" class="word-form-select" bind:value={partOfSpeech} disabled={isPending}>
				<option value="UNKNOWN">Unknown</option>
				<option value="NOUN">Noun</option>
				<option value="VERB">Verb</option>
				<option value="ADJECTIVE">Adjective</option>
				<option value="ADVERB">Adverb</option>
				<option value="PREPOSITION">Preposition</option>
				<option value="PARTICLE">Particle</option>
				<option value="INTERJECTION">Interjection</option>
				<option value="CONJUNCTION">Conjunction</option>
				<option value="PRONOUN">Pronoun</option>
			</select>
		</div>

		<div class="word-form-field">
			<label class="word-form-label" for="word-dialect">Dialect</label>
			<select id="word-dialect" class="word-form-select" bind:value={dialect} disabled={isPending}>
				<option value="MSA">MSA</option>
				<option value="TUNISIAN">Tunisian</option>
				<option value="MOROCCAN">Moroccan</option>
				<option value="EGYPTIAN">Egyptian</option>
				<option value="GULF">Gulf</option>
				<option value="LEVANTINE">Levantine</option>
				<option value="IRAQI">Iraqi</option>
			</select>
		</div>

		<div class="word-form-field">
			<label class="word-form-label" for="word-difficulty">Difficulty</label>
			<select
				id="word-difficulty"
				class="word-form-select"
				bind:value={difficulty}
				disabled={isPending}
			>
				<option value="BEGINNER">Beginner</option>
				<option value="INTERMEDIATE">Intermediate</option>
				<option value="ADVANCED">Advanced</option>
			</select>
		</div>
	</div>

	<!-- Pronunciation URL -->
	<div class="word-form-field">
		<label class="word-form-label" for="word-pronunciation">
			Pronunciation URL
			{#if pronunciationWasAutofilled}
				<span class="word-form-hint-inline">· Forvo auto-filled</span>
			{/if}
		</label>
		<input
			id="word-pronunciation"
			class="word-form-input"
			type="url"
			placeholder="https://forvo.com/search/…"
			bind:value={pronunciationUrl}
			disabled={isPending}
			oninput={() => (pronunciationWasAutofilled = false)}
		/>
	</div>

	<!-- Root selector -->
	<div class="word-form-field">
		<label class="word-form-label" for="word-root-filter">Root</label>
		<input
			id="word-root-filter"
			class="word-form-root-filter"
			type="text"
			placeholder="Filter by form or meaning…"
			bind:value={rootFilter}
			disabled={isPending}
			autocomplete="off"
		/>
		<select
			class="word-form-select"
			bind:value={rootId}
			disabled={isPending || allRoots.isPending}
		>
			<option value={null}>— no root —</option>
			{#each filteredRoots as root (root.id)}
				<option value={root.id}>
					{root.displayForm} · {root.normalizedForm}{root.meaning ? ` — ${root.meaning}` : ''}
				</option>
			{/each}
		</select>
		{#if allRoots.isError}
			<p class="word-form-hint">Could not load roots.</p>
		{/if}
	</div>

	<!-- Derived from — autocomplete -->
	<div class="word-form-field">
		<label class="word-form-label" for="word-derived-from">Derived from</label>

		{#if derivedFromSelected && derivedFromSelected.arabicText}
			<div class="word-form-autocomplete-selected">
				<span class="arabic-input">{derivedFromSelected.arabicText}</span>
				{#if derivedFromSelected.translation}
					<span>— {derivedFromSelected.translation}</span>
				{/if}
				<Button variant="ghost" type="button" onclick={clearDerivedFrom} disabled={isPending}>
					Clear
				</Button>
			</div>
		{:else}
			<div class="word-form-autocomplete-wrap">
				<input
					id="word-derived-from"
					class="word-form-input arabic-input"
					type="text"
					placeholder="ابحث عن كلمة…"
					bind:value={derivedFromQuery}
					onfocus={() => {
						if (derivedFromDebounced.length >= 2) showAutocompleteDropdown = true;
					}}
					oninput={() => {
						showAutocompleteDropdown = derivedFromQuery.length >= 2;
					}}
					disabled={isPending}
					autocomplete="off"
					spellcheck="false"
					dir="rtl"
				/>
				{#if showAutocompleteDropdown && visibleAutocompleteResults.length > 0}
					<ul class="word-form-autocomplete-results">
						{#each visibleAutocompleteResults as word (word.id)}
							<li>
								<button
									class="word-form-autocomplete-item"
									type="button"
									onclick={() => selectDerivedFrom(word)}
								>
									<span class="arabic-input">{word.arabicText}</span>
									{#if word.translation}
										<span>— {word.translation}</span>
									{/if}
								</button>
							</li>
						{/each}
					</ul>
				{/if}
			</div>
		{/if}
		<p class="word-form-hint">The word this was derived from (creates a graph edge).</p>
	</div>

	{#if submitError}
		<p class="word-form-error">{submitError}</p>
	{/if}

	<div class="word-form-actions">
		<Button variant="outline" type="button" onclick={onCancel} disabled={isPending}>Cancel</Button>
		<Button type="submit" disabled={isPending || (!isEdit && !arabicText.trim())}>
			{isPending ? 'Saving…' : isEdit ? 'Update word' : 'Create word'}
		</Button>
	</div>
</form>
