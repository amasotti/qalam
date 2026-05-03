<script lang="ts">
import type { WordAutocompleteResponse, WordRelationResponse } from '$lib/api/types.gen';
import WordSearchCombobox from '$lib/components/annotations/WordSearchCombobox.svelte';
import { useAddWordRelation, useDeleteWordRelation, useWordRelations } from '$lib/stores/words';

interface Props {
	wordId: string;
}

const { wordId }: Props = $props();

const relations = useWordRelations(() => wordId);
const addRelation = useAddWordRelation();
const deleteRelation = useDeleteWordRelation();

let showForm = $state(false);
let selectedRelated = $state<WordAutocompleteResponse[]>([]);
let newType = $state<'SYNONYM' | 'ANTONYM' | 'RELATED'>('RELATED');
let addError = $state('');

const synonyms = $derived((relations.data ?? []).filter((r) => r.relationType === 'SYNONYM'));
const antonyms = $derived((relations.data ?? []).filter((r) => r.relationType === 'ANTONYM'));
const related = $derived((relations.data ?? []).filter((r) => r.relationType === 'RELATED'));

const hasAny = $derived(synonyms.length > 0 || antonyms.length > 0 || related.length > 0);

function handleDelete(relatedWordId: string, type: 'SYNONYM' | 'ANTONYM' | 'RELATED') {
	deleteRelation.mutate({ id: wordId, relatedWordId, type });
}

async function handleAdd() {
	const target = selectedRelated[0];
	if (!target) return;
	addError = '';

	addRelation.mutate(
		{ id: wordId, body: { relatedWordId: target.id, relationType: newType } },
		{
			onSuccess: () => {
				selectedRelated = [];
				newType = 'RELATED';
				showForm = false;
			},
			onError: (e) => {
				addError = e instanceof Error ? e.message : 'Failed to add relation';
			},
		}
	);
}
</script>

{#snippet relationChip(rel: WordRelationResponse, onDelete: () => void)}
	<a class="relation-chip" href="/words/{rel.relatedWordId}">
		<span class="relation-chip-ar arabic-text" dir="rtl">{rel.relatedWordArabic}</span>
		{#if rel.relatedWordTranslation}
			<span class="relation-chip-tr">{rel.relatedWordTranslation}</span>
		{/if}
		<button
			class="chip-delete"
			onclick={(e) => { e.preventDefault(); onDelete(); }}
			disabled={deleteRelation.isPending}
			aria-label="Remove relation"
		>×</button>
	</a>
{/snippet}

{#snippet addForm()}
	<div class="relation-add-form">
		<div class="relation-search-wrap">
			<WordSearchCombobox
				selectedWords={selectedRelated}
				onchange={(words) => { selectedRelated = words.slice(-1); }}
			/>
		</div>
		<select
			class="select-compact"
			bind:value={newType}
			disabled={addRelation.isPending}
		>
			<option value="RELATED">Related</option>
			<option value="SYNONYM">Synonym</option>
			<option value="ANTONYM">Antonym</option>
		</select>
		<button
			class="btn btn-primary btn-sm"
			onclick={handleAdd}
			disabled={addRelation.isPending || selectedRelated.length === 0}
		>{addRelation.isPending ? 'Adding…' : 'Add'}</button>
		<button
			class="btn btn-sm"
			onclick={() => { showForm = false; selectedRelated = []; addError = ''; }}
			disabled={addRelation.isPending}
		>Cancel</button>
		{#if addError}
			<span class="form-error-msg">{addError}</span>
		{/if}
	</div>
{/snippet}

{#if !relations.isPending}
	{#if hasAny || showForm}
		<div class="relations-panel">
			{#if synonyms.length > 0}
				<div class="relations-group">
					<span class="sect-label">Synonyms</span>
					<div class="relations-chips">
						{#each synonyms as rel (rel.relatedWordId)}
							{@render relationChip(rel, () => handleDelete(rel.relatedWordId, 'SYNONYM'))}
						{/each}
					</div>
				</div>
			{/if}

			{#if antonyms.length > 0}
				<div class="relations-group">
					<span class="sect-label">Antonyms</span>
					<div class="relations-chips">
						{#each antonyms as rel (rel.relatedWordId)}
							{@render relationChip(rel, () => handleDelete(rel.relatedWordId, 'ANTONYM'))}
						{/each}
					</div>
				</div>
			{/if}

			{#if related.length > 0}
				<div class="relations-group">
					<span class="sect-label">Related</span>
					<div class="relations-chips">
						{#each related as rel (rel.relatedWordId)}
							{@render relationChip(rel, () => handleDelete(rel.relatedWordId, 'RELATED'))}
						{/each}
					</div>
				</div>
			{/if}

			{#if !showForm}
				<button
					class="btn btn-xs"
					onclick={() => (showForm = true)}
				>+ Add relation</button>
			{:else}
				{@render addForm()}
			{/if}
		</div>
	{:else}
		<div class="section-block">
			<button
				class="btn btn-xs"
				onclick={() => (showForm = true)}
			>+ Add relation</button>
			{#if showForm}
				<div class="relation-add-form-offset">
					{@render addForm()}
				</div>
			{/if}
		</div>
	{/if}
{/if}
