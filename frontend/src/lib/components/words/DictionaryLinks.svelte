<script lang="ts">
import type { CreateDictionaryLinkRequest, DictionarySource } from '$lib/api/types.gen';
import {
	useAddDictionaryLink,
	useDeleteDictionaryLink,
	useDictionaryLinks,
} from '$lib/stores/words';

interface Props {
	wordId: string;
	arabicText: string;
}

const { wordId, arabicText }: Props = $props();

const linksQuery = useDictionaryLinks(() => wordId);
const addMutation = useAddDictionaryLink();
const deleteMutation = useDeleteDictionaryLink();

const URL_TEMPLATES: Partial<Record<DictionarySource, string>> = {
	ALMANY: 'https://www.almaany.com/en/dict/ar-en/{word}',
	LIVING_ARABIC: 'https://www.livingarabic.com/en/search?q={word}',
	DERJA_NINJA: 'https://derja.ninja/search?search={word}&script=arabic',
	REVERSO: 'https://dictionary.reverso.net/arabic-english/{word}',
	WIKTIONARY: 'https://en.wiktionary.org/wiki/{word}',
	ARABIC_STUDENT_DICTIONARY: 'https://www.arabicstudentsdictionary.com/search?q={word}',
	LANGENSCHEIDT: 'https://de.langenscheidt.com/arabisch-deutsch/{word}',
};

const sourceLabels: Record<DictionarySource, string> = {
	ALMANY: 'Almaany',
	LIVING_ARABIC: 'Living Arabic',
	DERJA_NINJA: 'Derja Ninja',
	REVERSO: 'Reverso',
	WIKTIONARY: 'Wiktionary',
	ARABIC_STUDENT_DICTIONARY: 'ASD',
	LANGENSCHEIDT: 'Langenscheidt',
	CUSTOM: 'Custom',
};

const sources: DictionarySource[] = [
	'ALMANY',
	'LIVING_ARABIC',
	'DERJA_NINJA',
	'REVERSO',
	'WIKTIONARY',
	'ARABIC_STUDENT_DICTIONARY',
	'LANGENSCHEIDT',
	'CUSTOM',
];

let showManual = $state(false);
let selectedSource = $state<DictionarySource>('ALMANY');
let urlInput = $state('');
let urlWasAutofilled = $state(false);
let addError = $state('');
let isAddingAll = $state(false);

$effect(() => {
	const template = URL_TEMPLATES[selectedSource];
	if (template && (urlInput === '' || urlWasAutofilled)) {
		urlInput = template.replace('{word}', encodeURIComponent(arabicText));
		urlWasAutofilled = true;
	} else if (!template && urlWasAutofilled) {
		urlInput = '';
		urlWasAutofilled = false;
	}
});

const links = $derived(linksQuery.data ?? []);
const existingSources = $derived(new Set(links.map((l) => l.source)));
const missingTemplated = $derived(
	(Object.keys(URL_TEMPLATES) as DictionarySource[]).filter((s) => !existingSources.has(s))
);

async function handleAddAll() {
	isAddingAll = true;
	addError = '';
	try {
		for (const source of missingTemplated) {
			const template = URL_TEMPLATES[source];

			if (!template) {
				throw new Error(`No URL template for source ${source}`);
			}

			await addMutation.mutateAsync({
				id: wordId,
				body: { source, url: template.replace('{word}', encodeURIComponent(arabicText)) },
			});
		}
	} catch {
		addError = 'Failed to add some dictionaries';
	} finally {
		isAddingAll = false;
	}
}

function handleOpenAll() {
	for (const link of links) {
		const a = document.createElement('a');
		a.href = link.url;
		a.target = '_blank';
		a.rel = 'noopener noreferrer';
		a.click();
	}
}

function handleAddOne() {
	if (!urlInput.trim()) return;
	addError = '';
	addMutation.mutate(
		{ id: wordId, body: { source: selectedSource, url: urlInput } as CreateDictionaryLinkRequest },
		{
			onSuccess: () => {
				urlInput = '';
				urlWasAutofilled = false;
				selectedSource = 'ALMANY';
				showManual = false;
			},
			onError: (e) => {
				addError = e instanceof Error ? e.message : 'Failed to add link';
			},
		}
	);
}
</script>

<div>
	{#if links.length > 0 || missingTemplated.length > 0}
		<div style="display:flex;gap:0.5rem;margin-bottom:0.75rem;">
			{#if links.length > 0}
				<button class="btn" style="font-size:0.75rem;padding:0.25rem 0.625rem;" onclick={handleOpenAll}>
					Open all ↗
				</button>
			{/if}
			{#if missingTemplated.length > 0}
				<button class="btn" style="font-size:0.75rem;padding:0.25rem 0.625rem;" onclick={handleAddAll} disabled={isAddingAll}>
					{isAddingAll ? 'Adding…' : '+ Add all dictionaries'}
				</button>
			{/if}
		</div>
	{/if}

	{#if linksQuery.isPending}
		<p class="annot-empty">Loading…</p>
	{:else if linksQuery.isError}
		<p class="annot-empty">Could not load links.</p>
	{:else if links.length === 0}
		<p class="annot-empty">No dictionary links yet.</p>
	{:else}
		<div class="dict-pills">
			{#each links as link (link.id)}
				<div style="display:inline-flex;align-items:center;gap:0;border-radius:6px;overflow:hidden;border:1px solid rgba(30,88,152,0.2);">
					<a
						href={link.url}
						target="_blank"
						rel="noopener noreferrer"
						class="dict-pill"
						style="border-radius:0;border:none;"
					>{sourceLabels[link.source]} ↗</a>
					<button
						style="padding:0.28rem 0.5rem;background:var(--cerulean-pale);border:none;border-left:1px solid rgba(30,88,152,0.2);cursor:pointer;font-size:0.8rem;color:var(--cerulean);line-height:1;"
						onclick={() => deleteMutation.mutate({ id: wordId, linkId: link.id })}
						disabled={deleteMutation.isPending}
						aria-label="Remove {sourceLabels[link.source]}"
					>×</button>
				</div>
			{/each}
		</div>
	{/if}

	<div style="margin-top:0.75rem;">
		<button
			class="dict-link-add-toggle"
			onclick={() => (showManual = !showManual)}
		>
			{showManual ? '− Hide' : '+ Custom link'}
		</button>

		{#if showManual}
			<div class="dict-links-add">
				<div class="dict-links-add-row">
					<select
						bind:value={selectedSource}
						class="dict-links-add-select"
						disabled={addMutation.isPending}
					>
						{#each sources as source}
							<option value={source}>{sourceLabels[source]}</option>
						{/each}
					</select>
					<input
						type="url"
						bind:value={urlInput}
						class="dict-links-add-input"
						placeholder="URL"
						disabled={addMutation.isPending}
						oninput={() => (urlWasAutofilled = false)}
					/>
				</div>
				{#if addError}
					<p class="dict-links-add-error">{addError}</p>
				{/if}
				<button
					class="btn btn-primary"
					style="font-size:0.75rem;padding:0.25rem 0.75rem;align-self:flex-start;"
					onclick={handleAddOne}
					disabled={addMutation.isPending || !urlInput.trim()}
				>Add</button>
			</div>
		{/if}
	</div>
</div>
