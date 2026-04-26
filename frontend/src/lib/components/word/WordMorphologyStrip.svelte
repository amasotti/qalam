<script lang="ts">
import type { UpsertWordMorphologyRequest } from '$lib/api/types.gen';
import { useMorphology, useUpsertMorphology } from '$lib/stores/words';

interface Props {
	wordId: string;
}

const { wordId }: Props = $props();

const morphology = useMorphology(() => wordId);
const upsert = useUpsertMorphology();

let editing = $state(false);
let editGender = $state<'MASCULINE' | 'FEMININE' | ''>('');
let editPattern = $state<'I' | 'II' | 'III' | 'IV' | 'V' | 'VI' | 'VII' | 'VIII' | 'IX' | 'X' | ''>('');
let saveError = $state('');

const hasData = $derived(
	!!morphology.data?.gender || !!morphology.data?.verbPattern
);

function startEdit() {
	editGender = morphology.data?.gender ?? '';
	editPattern = morphology.data?.verbPattern ?? '';
	saveError = '';
	editing = true;
}

async function handleSave() {
	saveError = '';
	const body: UpsertWordMorphologyRequest = {};
	if (editGender) body.gender = editGender;
	if (editPattern) body.verbPattern = editPattern;

	upsert.mutate(
		{ id: wordId, body },
		{
			onSuccess: () => {
				editing = false;
			},
			onError: (e) => {
				saveError = e instanceof Error ? e.message : 'Failed to save';
			},
		}
	);
}

const genderLabel: Record<string, string> = {
	MASCULINE: 'm.',
	FEMININE: 'f.',
};

const VERB_PATTERNS = ['I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X'] as const;
</script>

{#if morphology.isPending}
	<!-- silent -->
{:else if editing}
	<div class="morph-strip morph-strip-edit">
		<select
			class="morph-select"
			bind:value={editGender}
			disabled={upsert.isPending}
		>
			<option value="">Gender —</option>
			<option value="MASCULINE">Masculine</option>
			<option value="FEMININE">Feminine</option>
		</select>

		<select
			class="morph-select"
			bind:value={editPattern}
			disabled={upsert.isPending}
		>
			<option value="">Form —</option>
			{#each VERB_PATTERNS as p}
				<option value={p}>Form {p}</option>
			{/each}
		</select>

		<button
			class="btn btn-primary"
			style="font-size:0.75rem;padding:0.25rem 0.75rem;"
			onclick={handleSave}
			disabled={upsert.isPending}
		>{upsert.isPending ? 'Saving…' : 'Save'}</button>

		<button
			class="btn"
			style="font-size:0.75rem;padding:0.25rem 0.625rem;"
			onclick={() => (editing = false)}
			disabled={upsert.isPending}
		>Cancel</button>

		{#if saveError}
			<span style="font-size:0.75rem;color:var(--coral);">{saveError}</span>
		{/if}
	</div>
{:else if hasData}
	<div class="morph-strip">
		{#if morphology.data?.gender}
			<span class="chip c-olive">{genderLabel[morphology.data.gender] ?? morphology.data.gender}</span>
		{/if}
		{#if morphology.data?.verbPattern}
			<span class="chip c-cerulean">Form {morphology.data.verbPattern}</span>
		{/if}
		<button
			class="morph-edit-btn"
			onclick={startEdit}
			aria-label="Edit morphology"
			title="Edit morphology"
		>✏</button>
	</div>
{:else}
	<div class="morph-strip morph-strip-empty">
		<button
			class="btn"
			style="font-size:0.75rem;padding:0.25rem 0.625rem;"
			onclick={startEdit}
		>+ Morphology</button>
	</div>
{/if}

<style>
.morph-strip {
	display: flex;
	align-items: center;
	gap: 0.5rem;
	margin-bottom: 1rem;
	flex-wrap: wrap;
}

.morph-strip-edit {
	padding: 0.625rem 0;
}

.morph-strip-empty {
	min-height: 0;
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

.morph-edit-btn {
	background: none;
	border: none;
	cursor: pointer;
	font-size: 0.85rem;
	color: var(--ink-ghost, #a0aec0);
	padding: 0.125rem 0.25rem;
	line-height: 1;
	transition: color 0.15s;
}

.morph-edit-btn:hover {
	color: var(--ink-mid, #4a5568);
}
</style>
