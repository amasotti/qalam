<script lang="ts">
import { ExternalLink, Plus, X } from '@lucide/svelte';
import type {
	CreateDictionaryLinkRequest,
	DictionaryLinkResponse,
	DictionarySource,
} from '$lib/api/types.gen';
import {
	useAddDictionaryLink,
	useDeleteDictionaryLink,
	useDictionaryLinks,
} from '$lib/stores/words';
import { removeArabicDiacritics } from '$lib/utils/arabicUtils';

interface Props {
	wordId: string;
	arabicText: string;
}

const { wordId, arabicText }: Props = $props();

const linksQuery = useDictionaryLinks(() => wordId);
const addMutation = useAddDictionaryLink();
const deleteMutation = useDeleteDictionaryLink();

const URL_TEMPLATES: Partial<Record<DictionarySource, string>> = {
	ARABIC_STUDENT_DICTIONARY: 'https://www.arabicstudentsdictionary.com/search?q={word}',
	DERJA_NINJA: 'https://derja.ninja/search?search={word}&script=arabic',
	ALMANY: 'https://www.almaany.com/en/dict/ar-en/{word}',
	WIKTIONARY: 'https://en.wiktionary.org/wiki/{word}',
	LIVING_ARABIC: 'https://www.livingarabic.com/en/search?q={word}',
	REVERSO: 'https://dictionary.reverso.net/arabic-english/{word}',
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
	'ARABIC_STUDENT_DICTIONARY',
	'DERJA_NINJA',
	'ALMANY',
	'LIVING_ARABIC',
	'WIKTIONARY',
	'REVERSO',
	'LANGENSCHEIDT',
	'CUSTOM',
];

let showAdd = $state(false);
let selectedSource = $state<DictionarySource>('ALMANY');
let urlInput = $state('');
let addError = $state('');

const links = $derived<DictionaryLinkResponse[]>(linksQuery.data ?? []);
const existingSources = $derived(new Set(links.map((l) => l.source)));
const templateSources = $derived(sources.filter((s) => URL_TEMPLATES[s] !== undefined));
const allAdded = $derived(templateSources.every((s) => existingSources.has(s)));

// Auto-fill URL when source changes in custom add form
$effect(() => {
	if (!showAdd) return;
	const template = URL_TEMPLATES[selectedSource];
	if (template) {
		urlInput = template.replace('{word}', encodeURIComponent(arabicText));
	} else {
		urlInput = '';
	}
});

function buildTemplateUrl(source: DictionarySource): string {
	const template = URL_TEMPLATES[source];
	if (template === undefined) return '';

	const normalizedArabic = removeArabicDiacritics(arabicText);
	return template.replace('{word}', encodeURIComponent(normalizedArabic));
}

async function handleAddAll() {
	addError = '';
	for (const source of sources) {
		if (existingSources.has(source)) continue;

		const url = buildTemplateUrl(source);
		if (!url) continue;

		try {
			await addMutation.mutateAsync({
				id: wordId,
				body: { source, url },
			});
		} catch {
			addError = 'Failed to add some links';
		}
	}
}

async function handleQuickAdd(source: DictionarySource) {
	const url = buildTemplateUrl(source);
	if (!url) return;
	try {
		await addMutation.mutateAsync({
			id: wordId,
			body: { source, url },
		});
	} catch {
		addError = 'Failed to add';
	}
}

function startCustomAdd() {
	selectedSource = 'ALMANY';
	addError = '';
	showAdd = true;
}

async function handleAddCustom() {
	if (!urlInput.trim()) return;
	addError = '';
	try {
		await addMutation.mutateAsync({
			id: wordId,
			body: { source: selectedSource, url: urlInput } as CreateDictionaryLinkRequest,
		});
		selectedSource = 'ALMANY';
		urlInput = '';
		showAdd = false;
		addError = '';
	} catch (e) {
		addError = e instanceof Error ? e.message : 'Failed to add link';
	}
}
</script>

{#if linksQuery.isPending}
    <span class="annot-empty">Loading…</span>
{:else}
    <div class="sect-label with-action">
        <span>Dictionaries</span>
        {#if !allAdded}
            <button class="btn btn-xs" onclick={() => handleAddAll()} disabled={addMutation.isPending}>
                + Add all
            </button>
        {/if}
    </div>
    <div class="dict-links-list">
        <!-- Show already added dictionaries -->
        {#if links.length > 0}
            {#each links as link (link.id)}
                <a href={link.url} target="_blank" rel="noopener noreferrer" class="dict-link-row">
                    <span class="dict-link-label">{sourceLabels[link.source]}</span>
                    <ExternalLink size={11} class="dict-link-icon"/>
                    <button
                            class="dict-link-remove"
                            onclick={(e: MouseEvent) => {
							e.preventDefault();
							e.stopPropagation();
							deleteMutation.mutate({ id: wordId, linkId: link.id });
						}}
                            disabled={deleteMutation.isPending}
                            aria-label="Remove {sourceLabels[link.source]}"
                    >
                        <X size={10}/>
                    </button>
                </a>
            {/each}
        {/if}

        {#if showAdd}
            <div class="dict-add-form">
                <select
                        class="dict-add-select"
                        bind:value={selectedSource}
                        disabled={addMutation.isPending}
                >
                    {#each sources as source}
                        <option value={source}>{sourceLabels[source]}</option>
                    {/each}
                </select>
                <input
                        type="url"
                        class="dict-add-input"
                        placeholder="URL"
                        bind:value={urlInput}
                        disabled={addMutation.isPending}
                />
                <button
                        class="btn btn-xs"
                        onclick={handleAddCustom}
                        disabled={addMutation.isPending || !urlInput.trim()}
                >Add
                </button>
                <button class="btn btn-xs" onclick={() => (showAdd = false)}>Cancel</button>
                {#if addError}
                    <span class="dict-add-error">{addError}</span>
                {/if}
            </div>
        {/if}

        <div class="dict-add-actions">
            {#each sources.filter(s => URL_TEMPLATES[s] && !existingSources.has(s)).slice(0, 4) as source (source)}
                <button
                        class="dict-quick-add"
                        onclick={() => handleQuickAdd(source)}
                        disabled={addMutation.isPending}
                >
                    <Plus size={10}/>
                    {sourceLabels[source]}
                </button>
            {/each}
            <button class="dict-quick-add dict-quick-add-custom" onclick={startCustomAdd}>
                <Plus size={10}/>
                Custom
            </button>
        </div>
    </div>
{/if}
