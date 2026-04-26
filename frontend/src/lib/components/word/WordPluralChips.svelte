<script lang="ts">
import type { WordPluralResponse } from '$lib/api/types.gen';
import { useAddWordPlural, useDeleteWordPlural, useWordPlurals } from '$lib/stores/words';

interface Props {
	wordId: string;
}

const { wordId }: Props = $props();

const plurals = useWordPlurals(() => wordId);
const addPlural = useAddWordPlural();
const deletePlural = useDeleteWordPlural();

let showForm = $state(false);
let newForm = $state('');
let newType = $state<WordPluralResponse['pluralType']>('BROKEN');
let addError = $state('');

const typeLabels: Record<WordPluralResponse['pluralType'], string> = {
	SOUND_MASC: 'sound m.',
	SOUND_FEM: 'sound f.',
	BROKEN: 'broken',
	PAUCAL: 'paucal',
	COLLECTIVE: 'collective',
	OTHER: 'other',
};

const PLURAL_TYPES: WordPluralResponse['pluralType'][] = [
	'BROKEN',
	'SOUND_MASC',
	'SOUND_FEM',
	'PAUCAL',
	'COLLECTIVE',
	'OTHER',
];

async function handleAdd() {
	if (!newForm.trim()) return;
	addError = '';

	addPlural.mutate(
		{ id: wordId, body: { pluralForm: newForm.trim(), pluralType: newType } },
		{
			onSuccess: () => {
				newForm = '';
				newType = 'BROKEN';
				showForm = false;
			},
			onError: (e) => {
				addError = e instanceof Error ? e.message : 'Failed to add plural';
			},
		}
	);
}

function handleDelete(pluralId: string) {
	deletePlural.mutate({ id: wordId, pluralId });
}
</script>

<div class="plurals-section">
	{#if plurals.isPending}
		<!-- silent -->
	{:else if (plurals.data ?? []).length > 0 || showForm}
		<div class="plurals-chips">
			{#each plurals.data ?? [] as p (p.id)}
				<div class="plural-chip">
					<span class="plural-chip-ar arabic-text" dir="rtl">{p.pluralForm}</span>
					<span class="plural-chip-type">({typeLabels[p.pluralType]})</span>
					<button
						class="chip-delete"
						onclick={() => handleDelete(p.id)}
						disabled={deletePlural.isPending}
						aria-label="Remove plural {p.pluralForm}"
					>×</button>
				</div>
			{/each}

			{#if !showForm}
				<button
					class="btn"
					style="font-size:0.75rem;padding:0.2rem 0.5rem;"
					onclick={() => (showForm = true)}
				>+ Add plural</button>
			{/if}
		</div>

		{#if showForm}
			<div class="plural-add-form">
				<input
					class="form-input input-ar"
					style="width:10rem"
					type="text"
					dir="rtl"
					placeholder="جمع…"
					bind:value={newForm}
					disabled={addPlural.isPending}
				/>
				<select
					class="select-compact"
					bind:value={newType}
					disabled={addPlural.isPending}
				>
					{#each PLURAL_TYPES as t}
						<option value={t}>{typeLabels[t]}</option>
					{/each}
				</select>
				<button
					class="btn btn-primary"
					style="font-size:0.75rem;padding:0.25rem 0.75rem;"
					onclick={handleAdd}
					disabled={addPlural.isPending || !newForm.trim()}
				>{addPlural.isPending ? 'Adding…' : 'Add'}</button>
				<button
					class="btn"
					style="font-size:0.75rem;padding:0.25rem 0.625rem;"
					onclick={() => { showForm = false; newForm = ''; addError = ''; }}
					disabled={addPlural.isPending}
				>Cancel</button>
				{#if addError}
					<span class="form-error-msg">{addError}</span>
				{/if}
			</div>
		{/if}
	{:else}
		<button
			class="btn"
			style="font-size:0.75rem;padding:0.2rem 0.5rem;"
			onclick={() => (showForm = true)}
		>+ Add plural</button>

		{#if showForm}
			<div class="plural-add-form" style="margin-top:0.5rem;">
				<input
					class="form-input input-ar"
					style="width:10rem"
					type="text"
					dir="rtl"
					placeholder="جمع…"
					bind:value={newForm}
					disabled={addPlural.isPending}
				/>
				<select
					class="select-compact"
					bind:value={newType}
					disabled={addPlural.isPending}
				>
					{#each PLURAL_TYPES as t}
						<option value={t}>{typeLabels[t]}</option>
					{/each}
				</select>
				<button
					class="btn btn-primary"
					style="font-size:0.75rem;padding:0.25rem 0.75rem;"
					onclick={handleAdd}
					disabled={addPlural.isPending || !newForm.trim()}
				>{addPlural.isPending ? 'Adding…' : 'Add'}</button>
				<button
					class="btn"
					style="font-size:0.75rem;padding:0.25rem 0.625rem;"
					onclick={() => { showForm = false; newForm = ''; addError = ''; }}
					disabled={addPlural.isPending}
				>Cancel</button>
				{#if addError}
					<span class="form-error-msg">{addError}</span>
				{/if}
			</div>
		{/if}
	{/if}
</div>

<style>
.plurals-section {
	margin-bottom: 1.25rem;
}

.plurals-chips {
	display: flex;
	flex-wrap: wrap;
	align-items: center;
	gap: 0.5rem;
	margin-bottom: 0.5rem;
}

.plural-chip {
	display: inline-flex;
	align-items: center;
	gap: 0.25rem;
	border: 1px solid rgba(30, 88, 152, 0.2);
	border-radius: 6px;
	padding: 0.2rem 0.5rem;
	background: var(--cerulean-pale, #ebf3ff);
	font-size: 0.8125rem;
}

.plural-chip-ar {
	font-size: 1rem;
	line-height: 1.4;
}

.plural-chip-type {
	color: var(--ink-mid, #4a5568);
	font-size: 0.75rem;
}

.plural-add-form {
	display: flex;
	align-items: center;
	gap: 0.5rem;
	flex-wrap: wrap;
}
</style>
