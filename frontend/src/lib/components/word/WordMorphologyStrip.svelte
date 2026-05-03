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
let editPattern = $state<'I' | 'II' | 'III' | 'IV' | 'V' | 'VI' | 'VII' | 'VIII' | 'IX' | 'X' | ''>(
	''
);
let saveError = $state('');

const hasData = $derived(!!morphology.data?.gender || !!morphology.data?.verbPattern);

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
			class="select-compact"
			bind:value={editGender}
			disabled={upsert.isPending}
		>
			<option value="">Gender —</option>
			<option value="MASCULINE">Masculine</option>
			<option value="FEMININE">Feminine</option>
		</select>

		<select
			class="select-compact"
			bind:value={editPattern}
			disabled={upsert.isPending}
		>
			<option value="">Form —</option>
			{#each VERB_PATTERNS as p}
				<option value={p}>Form {p}</option>
			{/each}
		</select>

		<button
			class="btn btn-primary btn-sm"
			onclick={handleSave}
			disabled={upsert.isPending}
		>{upsert.isPending ? 'Saving…' : 'Save'}</button>

		<button
			class="btn btn-sm"
			onclick={() => (editing = false)}
			disabled={upsert.isPending}
		>Cancel</button>

		{#if saveError}
			<span class="form-error-msg">{saveError}</span>
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
			class="btn btn-sm"
			onclick={startEdit}
		>+ Morphology</button>
	</div>
{/if}
