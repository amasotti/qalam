<script lang="ts">
import type { WordRelationResponse } from '$lib/api/types.gen';
import { useAddWordRelation, useDeleteWordRelation, useWordRelations } from '$lib/stores/words';

interface Props {
	wordId: string;
}

const { wordId }: Props = $props();

const relations = useWordRelations(() => wordId);
const addRelation = useAddWordRelation();
const deleteRelation = useDeleteWordRelation();

let showForm = $state(false);
let newRelatedId = $state('');
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
	if (!newRelatedId.trim()) return;
	addError = '';

	addRelation.mutate(
		{ id: wordId, body: { relatedWordId: newRelatedId.trim(), relationType: newType } },
		{
			onSuccess: () => {
				newRelatedId = '';
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
		<span class="relation-chip-ar" dir="rtl">{rel.relatedWordArabic}</span>
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
		<input
			class="relation-id-input"
			type="text"
			placeholder="Related word UUID…"
			bind:value={newRelatedId}
			disabled={addRelation.isPending}
		/>
		<select
			class="morph-select"
			bind:value={newType}
			disabled={addRelation.isPending}
		>
			<option value="RELATED">Related</option>
			<option value="SYNONYM">Synonym</option>
			<option value="ANTONYM">Antonym</option>
		</select>
		<button
			class="btn btn-primary"
			style="font-size:0.75rem;padding:0.25rem 0.75rem;"
			onclick={handleAdd}
			disabled={addRelation.isPending || !newRelatedId.trim()}
		>{addRelation.isPending ? 'Adding…' : 'Add'}</button>
		<button
			class="btn"
			style="font-size:0.75rem;padding:0.25rem 0.625rem;"
			onclick={() => { showForm = false; newRelatedId = ''; addError = ''; }}
			disabled={addRelation.isPending}
		>Cancel</button>
		{#if addError}
			<span style="font-size:0.75rem;color:var(--coral);">{addError}</span>
		{/if}
	</div>
{/snippet}

{#if !relations.isPending}
	{#if hasAny || showForm}
		<div class="relations-panel">
			{#if synonyms.length > 0}
				<div class="relations-group">
					<span class="relations-group-label">Synonyms</span>
					<div class="relations-chips">
						{#each synonyms as rel (rel.relatedWordId)}
							{@render relationChip(rel, () => handleDelete(rel.relatedWordId, 'SYNONYM'))}
						{/each}
					</div>
				</div>
			{/if}

			{#if antonyms.length > 0}
				<div class="relations-group">
					<span class="relations-group-label">Antonyms</span>
					<div class="relations-chips">
						{#each antonyms as rel (rel.relatedWordId)}
							{@render relationChip(rel, () => handleDelete(rel.relatedWordId, 'ANTONYM'))}
						{/each}
					</div>
				</div>
			{/if}

			{#if related.length > 0}
				<div class="relations-group">
					<span class="relations-group-label">Related</span>
					<div class="relations-chips">
						{#each related as rel (rel.relatedWordId)}
							{@render relationChip(rel, () => handleDelete(rel.relatedWordId, 'RELATED'))}
						{/each}
					</div>
				</div>
			{/if}

			{#if !showForm}
				<button
					class="btn"
					style="font-size:0.75rem;padding:0.2rem 0.5rem;"
					onclick={() => (showForm = true)}
				>+ Add relation</button>
			{:else}
				{@render addForm()}
			{/if}
		</div>
	{:else}
		<div style="margin-bottom:0.75rem;">
			<button
				class="btn"
				style="font-size:0.75rem;padding:0.2rem 0.5rem;"
				onclick={() => (showForm = true)}
			>+ Add relation</button>
			{#if showForm}
				<div style="margin-top:0.5rem;">
					{@render addForm()}
				</div>
			{/if}
		</div>
	{/if}
{/if}

<style>
.relations-panel {
	display: flex;
	flex-direction: column;
	gap: 0.75rem;
	margin-bottom: 1.25rem;
}

.relations-group {
	display: flex;
	flex-direction: column;
	gap: 0.375rem;
}

.relations-group-label {
	font-size: 0.75rem;
	font-weight: 600;
	text-transform: uppercase;
	letter-spacing: 0.05em;
	color: var(--ink-ghost, #a0aec0);
}

.relations-chips {
	display: flex;
	flex-wrap: wrap;
	gap: 0.5rem;
}

.relation-chip {
	display: inline-flex;
	align-items: center;
	gap: 0.375rem;
	border: 1px solid var(--border, #e2e8f0);
	border-radius: 8px;
	padding: 0.25rem 0.625rem;
	background: var(--white, #fff);
	text-decoration: none;
	color: inherit;
	transition: background 0.15s;
}

.relation-chip:hover {
	background: var(--bg-subtle, #f7fafc);
}

.relation-chip-ar {
	font-family: 'Noto Naskh Arabic', serif;
	font-size: 1rem;
	line-height: 1.4;
}

.relation-chip-tr {
	font-size: 0.75rem;
	color: var(--ink-mid, #4a5568);
}

.chip-delete {
	background: none;
	border: none;
	cursor: pointer;
	color: var(--coral, #e53e3e);
	font-size: 0.9rem;
	line-height: 1;
	padding: 0 0.125rem;
}

.chip-delete:disabled {
	opacity: 0.5;
	cursor: not-allowed;
}

.relation-add-form {
	display: flex;
	align-items: center;
	gap: 0.5rem;
	flex-wrap: wrap;
}

.relation-id-input {
	font-size: 0.8rem;
	padding: 0.25rem 0.5rem;
	border: 1px solid var(--border, #e2e8f0);
	border-radius: 6px;
	background: var(--white, #fff);
	width: 18rem;
	font-family: monospace;
}

.morph-select {
	font-size: 0.8rem;
	padding: 0.25rem 0.5rem;
	border: 1px solid var(--border, #e2e8f0);
	border-radius: 6px;
	background: var(--white, #fff);
	color: var(--ink, #1a1a1a);
	height: 2rem;
}
</style>
