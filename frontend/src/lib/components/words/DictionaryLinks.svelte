<script lang="ts">
import { BookOpen, ExternalLink, Plus } from 'lucide-svelte';
import type { CreateDictionaryLinkRequest, DictionarySource } from '$lib/api/types.gen';
import Button from '$lib/components/ui/button/button.svelte';
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
			const template = URL_TEMPLATES[source]!;
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
	links.forEach((link, i) => {
		setTimeout(() => window.open(link.url, '_blank', 'noopener,noreferrer'), i * 120);
	});
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

<div class="dict-links">
	<div class="dict-links-header">
		<h3 class="dict-links-title">Dictionaries</h3>
		<div class="dict-links-header-actions">
			{#if links.length > 0}
				<Button size="sm" variant="ghost" onclick={handleOpenAll}>
					<BookOpen size={14} />
					Open all
				</Button>
			{/if}
			{#if missingTemplated.length > 0}
				<Button size="sm" variant="ghost" onclick={handleAddAll} disabled={isAddingAll}>
					<Plus size={14} />
					{isAddingAll ? 'Adding…' : 'Add all'}
				</Button>
			{/if}
		</div>
	</div>

	{#if linksQuery.isPending}
		<p class="dict-links-empty">Loading…</p>
	{:else if linksQuery.isError}
		<p class="dict-links-empty">Could not load links.</p>
	{:else if links.length === 0}
		<p class="dict-links-empty">No dictionary links yet.</p>
	{:else}
		<div class="dict-badges">
			{#each links as link (link.id)}
				<div class="dict-badge dict-badge-{link.source.toLowerCase()}">
					<a
						href={link.url}
						target="_blank"
						rel="noopener noreferrer"
						class="dict-badge-link"
						title={link.url}
					>
						{sourceLabels[link.source]}
						<ExternalLink size={11} />
					</a>
					<button
						class="dict-badge-delete"
						onclick={() => deleteMutation.mutate({ id: wordId, linkId: link.id })}
						disabled={deleteMutation.isPending}
						aria-label="Remove {sourceLabels[link.source]}"
					>×</button>
				</div>
			{/each}
		</div>
	{/if}

	<div class="dict-links-manual">
		<button
			class="dict-links-manual-toggle"
			onclick={() => (showManual = !showManual)}
		>
			{showManual ? '− Hide manual add' : '+ Custom / single'}
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
				<Button
					onclick={handleAddOne}
					disabled={addMutation.isPending || !urlInput.trim()}
					size="sm"
				>
					<Plus size={14} />
					Add
				</Button>
			</div>
		{/if}
	</div>
</div>
