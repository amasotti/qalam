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
let saveError = $state('');

const hasData = $derived(!!morphology.data?.gender);

function startEdit() {
	editGender = morphology.data?.gender ?? '';
	saveError = '';
	editing = true;
}

async function handleSave() {
	saveError = '';
	const body: UpsertWordMorphologyRequest = {};
	if (editGender) body.gender = editGender;

	upsert.mutate(
		{ id: wordId, body },
		{
			onSuccess: () => {
				editing = false;
			},
			onError: (e: unknown) => {
				saveError = e instanceof Error ? e.message : 'Failed to save';
			},
		}
	);
}

const genderLabel: Record<string, string> = {
	MASCULINE: 'm.',
	FEMININE: 'f.',
};
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
			<span class="morph-tag">Gender</span>
			<span class="chip c-olive">{genderLabel[morphology.data.gender] ?? morphology.data.gender}</span>
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
