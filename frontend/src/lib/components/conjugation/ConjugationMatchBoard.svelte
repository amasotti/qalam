<script lang="ts">
import type {
	ConjugationExerciseItemResponse,
	ConjugationExerciseMappingResponse,
} from '$lib/api/types.gen';
import ConjugatedForm from '$lib/components/conjugation/ConjugatedForm.svelte';
import { Button } from '$lib/components/ui/button';

interface Props {
	item: ConjugationExerciseItemResponse;
	isSubmitting: boolean;
	onsubmit: (mappings: ConjugationExerciseMappingResponse[]) => void;
}

let { item, isSubmitting, onsubmit }: Props = $props();
let selectedFormId = $state<string | null>(null);
let mappings = $state<Record<string, string>>({});
let initializedItemId = $state<string | null>(null);
const isAnswered = $derived(item.result != null);
const allMapped = $derived(Object.keys(mappings).length === item.forms.length);
const correctByForm = $derived(
	new Map((item.correctMappings ?? []).map((mapping) => [mapping.formId, mapping.labelId]))
);
const submittedByLabel = $derived(
	new Map((item.submittedMappings ?? []).map((m) => [m.labelId, m.formId]))
);
const labelCorrectness = $derived(
	new Map(
		Array.from(submittedByLabel.entries()).map(([labelId, formId]) => {
			return [labelId, correctByForm.get(formId) === labelId];
		})
	)
);

$effect(() => {
	if (initializedItemId === item.itemId) return;
	initializedItemId = item.itemId;
	selectedFormId = null;
	mappings = Object.fromEntries(
		(item.submittedMappings ?? []).map((mapping) => [mapping.formId, mapping.labelId])
	);
});

function selectForm(formId: string) {
	if (isAnswered || isSubmitting) return;
	selectedFormId = selectedFormId === formId ? null : formId;
}

function selectLabel(labelId: string) {
	if (!selectedFormId || isAnswered || isSubmitting) return;
	mappings = {
		...Object.fromEntries(
			Object.entries(mappings).filter(([formId, pairedLabelId]) =>
				formId === selectedFormId ? true : pairedLabelId !== labelId
			)
		),
		[selectedFormId]: labelId,
	};
	selectedFormId = null;
}

function clearPair(formId: string) {
	if (isAnswered || isSubmitting) return;
	const { [formId]: _, ...remaining } = mappings;
	mappings = remaining;
}

function submit() {
	if (!allMapped || isAnswered) return;
	onsubmit(Object.entries(mappings).map(([formId, labelId]) => ({ formId, labelId })));
}
</script>

<section class="conj-exercise-board" aria-labelledby="conj-exercise-lemma">
    <header class="conj-exercise-board-header">
        <p>Match each form to its morphology</p>
        <h1 id="conj-exercise-lemma" class="arabic" lang="ar" dir="rtl">{item.lemma}</h1>
        <span>{item.translation ?? 'No translation'} · Form {item.verbForm} · {item.tense.toLowerCase()} active</span>
    </header>

    <div class="conj-exercise-columns">
        <div class="conj-exercise-column" role="group" aria-label="Arabic forms">
            <h2>Forms</h2>
            {#each item.forms as form (form.formId)}
                <button type="button" class="conj-exercise-form" class:selected={selectedFormId === form.formId}
                        class:paired={mappings[form.formId] !== undefined && !isAnswered}
                        class:correct={isAnswered && correctByForm.get(form.formId) === (item.submittedMappings ?? []).find((mapping) => mapping.formId === form.formId)?.labelId}
                        class:incorrect={isAnswered && correctByForm.get(form.formId) !== (item.submittedMappings ?? []).find((mapping) => mapping.formId === form.formId)?.labelId}
                        aria-pressed={selectedFormId === form.formId} disabled={isAnswered || isSubmitting}
                        onclick={() => selectForm(form.formId)}>
                    <ConjugatedForm arabic={form.arabic} segments={form.segments}/>
                </button>
            {/each}
        </div>
        <div class="conj-exercise-column" role="group" aria-label="Morphology labels">
            <h2>Labels</h2>
            {#each item.labels as label (label.labelId)}
                <button type="button" class="conj-exercise-label"
                        class:paired={Object.values(mappings).includes(label.labelId) && !isAnswered}
                        class:correct={isAnswered && labelCorrectness.get(label.labelId) === true}
                        class:incorrect={isAnswered && labelCorrectness.get(label.labelId) === false}
                        disabled={!selectedFormId || isAnswered || isSubmitting}
                        onclick={() => selectLabel(label.labelId)}>{label.label}</button>
            {/each}
        </div>
    </div>

    {#if !isAnswered && Object.keys(mappings).length > 0}
        <div class="conj-exercise-pairs" aria-label="Current pairings">
            {#each item.forms.filter((form) => mappings[form.formId]) as form (form.formId)}
                <button type="button" onclick={() => clearPair(form.formId)}>{form.arabic}
                    → {item.labels.find((label) => label.labelId === mappings[form.formId])?.label} <span
                            aria-hidden="true">×</span></button>
            {/each}
        </div>
    {/if}

    {#if isAnswered}
        <p class:correct={item.result === 'CORRECT'} class:incorrect={item.result === 'INCORRECT'}
           class="conj-exercise-feedback">{item.result === 'CORRECT' ? 'All four matches are correct.' : 'Review the highlighted forms, then continue.'}</p>
    {:else}
        <div class="conj-exercise-actions">
            <Button onclick={submit}
                    disabled={!allMapped || isSubmitting}>{isSubmitting ? 'Checking…' : 'Check answers'}</Button>
        </div>
    {/if}
</section>
