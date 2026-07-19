<script lang="ts">
import type { UpsertVerbDetailsRequest } from '$lib/api/types.gen';
import { useUpsertVerbDetails, useVerbDetails } from '$lib/stores/words';

interface Props {
	wordId: string;
}

const { wordId }: Props = $props();
const verbDetails = useVerbDetails(() => wordId);
const upsert = useUpsertVerbDetails();

const VERB_FORMS: UpsertVerbDetailsRequest['verbForm'][] = [
	'I',
	'II',
	'III',
	'IV',
	'V',
	'VI',
	'VII',
	'VIII',
	'IX',
	'X',
];
const WEAKNESS_TYPES: NonNullable<UpsertVerbDetailsRequest['weaknessType']>[] = [
	'SOUND',
	'ASSIMILATED',
	'HOLLOW',
	'GEMINATE',
	'DEFECTIVE',
	'DOUBLY_WEAK',
];
const PAST_PATTERNS = ['fa3ala', 'fa3ila', 'fa3ula'];
const PRESENT_PATTERNS = ['yaf3ulu', 'yaf3ilu', 'yaf3alu'];

let editing = $state(false);
let verbForm = $state<UpsertVerbDetailsRequest['verbForm']>('I');
let weaknessType = $state<NonNullable<UpsertVerbDetailsRequest['weaknessType']>>('SOUND');
let pastPattern = $state('fa3ala');
let presentPattern = $state('yaf3ulu');
let saveError = $state('');

function startEdit() {
	const details = verbDetails.data;
	verbForm = details?.verbForm ?? 'I';
	weaknessType = details?.weaknessType ?? 'SOUND';
	pastPattern = details?.pastPattern ?? 'fa3ala';
	presentPattern = details?.presentPattern ?? 'yaf3ulu';
	saveError = '';
	editing = true;
}

function save() {
	saveError = '';
	upsert.mutate(
		{
			id: wordId,
			body: {
				verbForm,
				weaknessType,
				pastPattern: verbForm === 'I' ? pastPattern : null,
				presentPattern: verbForm === 'I' ? presentPattern : null,
			},
		},
		{
			onSuccess: () => (editing = false),
			onError: (error) => {
				saveError = error instanceof Error ? error.message : 'Failed to save verb details.';
			},
		}
	);
}
</script>

{#if verbDetails.isPending}
	<!-- silent -->
{:else if editing}
	<form class="morph-strip morph-strip-edit" onsubmit={(event) => { event.preventDefault(); save(); }}>
		<span class="morph-tag">Verb</span>
		<label>
			<select class="select-compact" aria-label="Verb form" bind:value={verbForm} disabled={upsert.isPending}>
				{#each VERB_FORMS as form}<option value={form}>Form {form}</option>{/each}
			</select>
		</label>
		<label>
			<select class="select-compact" aria-label="Weakness type" bind:value={weaknessType} disabled={upsert.isPending}>
				{#each WEAKNESS_TYPES as type}<option value={type}>{type.toLowerCase().replace('_', ' ')}</option>{/each}
			</select>
		</label>
		{#if verbForm === 'I'}
			<label>
				<select class="select-compact" aria-label="Past vowel pattern" bind:value={pastPattern} disabled={upsert.isPending}>
					{#each PAST_PATTERNS as pattern}<option value={pattern}>{pattern}</option>{/each}
				</select>
			</label>
			<label>
				<select class="select-compact" aria-label="Present vowel pattern" bind:value={presentPattern} disabled={upsert.isPending}>
					{#each PRESENT_PATTERNS as pattern}<option value={pattern}>{pattern}</option>{/each}
				</select>
			</label>
		{/if}
		<button class="btn btn-primary btn-sm" type="submit" disabled={upsert.isPending}>{upsert.isPending ? 'Saving…' : 'Save'}</button>
		<button class="btn btn-sm" type="button" onclick={() => (editing = false)} disabled={upsert.isPending}>Cancel</button>
		{#if saveError}<span class="form-error-msg">{saveError}</span>{/if}
	</form>
{:else if verbDetails.data}
	<div class="morph-strip">
		<span class="morph-tag">Verb</span>
		<span class="chip c-olive">Form {verbDetails.data.verbForm}</span>
		<span class="chip c-muted">{verbDetails.data.weaknessType.toLowerCase().replace('_', ' ')}</span>
		{#if verbDetails.data.pastPattern}<span class="chip c-muted">{verbDetails.data.pastPattern}</span>{/if}
		{#if verbDetails.data.presentPattern}<span class="chip c-muted">{verbDetails.data.presentPattern}</span>{/if}
		<a class="btn btn-xs" href={`/verbs/conjugation?wordId=${wordId}`}>View conjugation</a>
		<button class="morph-edit-btn" type="button" onclick={startEdit} aria-label="Edit verb details" title="Edit verb details">✏</button>
	</div>
{:else}
	<div class="morph-strip morph-strip-empty">
		<button class="btn btn-sm" type="button" onclick={startEdit}>+ Verb details</button>
	</div>
{/if}
