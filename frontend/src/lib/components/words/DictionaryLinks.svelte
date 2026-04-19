<script lang="ts">
	import { useAddDictionaryLink, useDeleteDictionaryLink, useDictionaryLinks } from '$lib/stores/words';
	import type { CreateDictionaryLinkRequest, DictionarySource } from '$lib/api/types.gen';
	import Button from '$lib/components/ui/button/button.svelte';
	import { Trash2, ExternalLink, Plus } from 'lucide-svelte';

	interface Props {
		wordId: string;
		arabicText: string;
	}

	const { wordId, arabicText }: Props = $props();

	// Queries and mutations
	const linksQuery = useDictionaryLinks(() => wordId);
	const addMutation = useAddDictionaryLink();
	const deleteMutation = useDeleteDictionaryLink();

	// Form state
	let selectedSource = $state<DictionarySource>('ALMANY');
	let urlInput = $state('');
	let addError = $state('');
	let urlWasAutofilled = $state(false);

	// URL templates for dictionary sources
	const URL_TEMPLATES: Partial<Record<DictionarySource, string>> = {
		ALMANY: 'https://www.almaany.com/en/dict/ar-en/{word}',
		LIVING_ARABIC: 'https://www.livingarabic.com/en/search?q={word}',
		DERJA_NINJA: 'https://derja.ninja/search?search={word}&script=arabic',
		REVERSO: 'https://dictionary.reverso.net/arabic-english/{word}',
		WIKTIONARY: 'https://en.wiktionary.org/wiki/{word}',
		ARABIC_STUDENT_DICTIONARY: 'https://www.arabicstudentsdictionary.com/search?q={word}',
		LANGENSCHEIDT: 'https://de.langenscheidt.com/arabisch-deutsch/{word}',
		// CUSTOM: no template
	};

	const sourceLabels: Record<DictionarySource, string> = {
		ALMANY: 'Almany',
		LIVING_ARABIC: 'Living Arabic',
		DERJA_NINJA: 'Derja Ninja',
		REVERSO: 'Reverso',
		WIKTIONARY: 'Wiktionary',
		ARABIC_STUDENT_DICTIONARY: 'Arabic Student Dictionary',
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

	// Autofill URL when source changes
	$effect(() => {
		const template = URL_TEMPLATES[selectedSource];
		if (template && (urlInput === '' || urlWasAutofilled)) {
			urlInput = template.replace('{word}', encodeURIComponent(arabicText));
			urlWasAutofilled = true;
		} else if (!template) {
			// CUSTOM — clear autofilled url but not manual
			if (urlWasAutofilled) {
				urlInput = '';
				urlWasAutofilled = false;
			}
		}
	});

	function truncateUrl(url: string, maxLength: number = 50): string {
		if (url.length <= maxLength) return url;
		return `${url.substring(0, maxLength)}…`;
	}

	function handleAddLink() {
		if (!urlInput.trim()) return;

		addError = '';
		const request: CreateDictionaryLinkRequest = {
			source: selectedSource,
			url: urlInput,
		};

		addMutation.mutate(
			{ id: wordId, body: request },
			{
				onSuccess: () => {
					urlInput = '';
					urlWasAutofilled = false;
					selectedSource = 'ALMANY';
				},
				onError: (error) => {
					addError = error instanceof Error ? error.message : 'Failed to add link';
				},
			}
		);
	}

	function handleDeleteLink(linkId: string) {
		deleteMutation.mutate({ id: wordId, linkId });
	}

	const isLoading = $derived(linksQuery.isPending);
	const isError = $derived(linksQuery.isError);
	const links = $derived(linksQuery.data ?? []);
	const isAddPending = $derived(addMutation.isPending);
	const isAddDisabled = $derived(isAddPending || !urlInput.trim());
</script>

<div class="dict-links">
	<h3 class="dict-links-title">Dictionary Links</h3>

	{#if isLoading}
		<p>Loading links…</p>
	{:else if isError}
		<p>Could not load links.</p>
	{:else if links.length === 0 && !isAddPending}
		<p class="dict-links-empty">No dictionary links yet.</p>
	{/if}

	{#if links.length > 0}
		<ul class="dict-links-list">
			{#each links as link (link.id)}
				<li class="dict-link-item">
					<span class="dict-link-source">{sourceLabels[link.source]}</span>
					<a href={link.url} target="_blank" class="dict-link-url">
						{truncateUrl(link.url)}
						<ExternalLink size={16} />
					</a>
					<button
						class="dict-link-delete"
						onclick={() => handleDeleteLink(link.id)}
						disabled={deleteMutation.isPending}
						aria-label="Delete link"
					>
						<Trash2 size={18} />
					</button>
				</li>
			{/each}
		</ul>
	{/if}

	<div class="dict-links-add">
		<div class="dict-links-add-row">
			<select bind:value={selectedSource} class="dict-links-add-select" disabled={isAddPending}>
				{#each sources as source}
					<option value={source}>{sourceLabels[source]}</option>
				{/each}
			</select>
			<input
				type="url"
				bind:value={urlInput}
				class="dict-links-add-input"
				placeholder="Enter URL"
				disabled={isAddPending}
				oninput={() => { urlWasAutofilled = false }}
			/>
		</div>

		{#if addError}
			<p class="dict-links-add-error">{addError}</p>
		{/if}

		<Button
			onclick={handleAddLink}
			disabled={isAddDisabled}
			size="sm"
		>
			<Plus size={16} />
			Add link
		</Button>
	</div>
</div>
