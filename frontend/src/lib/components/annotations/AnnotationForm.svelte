<script lang="ts">
import { untrack } from 'svelte';
import type { AnnotationResponse, WordAutocompleteResponse } from '$lib/api/types.gen';
import { useCreateAnnotation, useUpdateAnnotation } from '$lib/stores/annotations';
import WordSearchCombobox from './WordSearchCombobox.svelte';

const TYPES = ['VOCAB', 'GRAMMAR', 'CULTURAL', 'STRUCTURE'] as const;
type AnnotationType = (typeof TYPES)[number];

interface Props {
	textId: string;
	anchor: string;
	initial?: AnnotationResponse;
	onSuccess: () => void;
	onCancel: () => void;
}

let { textId, anchor, initial, onSuccess, onCancel }: Props = $props();

let type = $state<AnnotationType>(untrack(() => (initial?.type as AnnotationType) ?? 'VOCAB'));
let content = $state(untrack(() => initial?.content ?? ''));
let linkedWords = $state<WordAutocompleteResponse[]>([]);

const createAnnotation = useCreateAnnotation();
const updateAnnotation = useUpdateAnnotation();

const isPending = $derived(createAnnotation.isPending || updateAnnotation.isPending);

async function handleSubmit(e: Event) {
	e.preventDefault();
	if (initial) {
		await updateAnnotation.mutateAsync({
			textId,
			id: initial.id,
			body: { type, content: content.trim() || null },
		});
	} else {
		await createAnnotation.mutateAsync({
			textId,
			body: {
				anchor,
				type,
				content: content.trim() || null,
				linkedWordIds: linkedWords.map((w) => w.id),
			},
		});
	}
	onSuccess();
}
</script>

<form class="annotation-form-shell" onsubmit={handleSubmit}>
	<div class="form-field">
		<div class="form-label">Type</div>
		<div class="toggle-group">
			{#each TYPES as t}
				<button
					type="button"
					class="toggle-btn"
					class:toggle-btn-active={type === t}
					onclick={() => (type = t)}
				>{t.charAt(0) + t.slice(1).toLowerCase()}</button>
			{/each}
		</div>
	</div>

	<div class="form-field">
		<label class="form-label" for="ann-content">Note</label>
		<textarea
			id="ann-content"
			class="form-textarea"
			rows="3"
			placeholder="Your observation…"
			bind:value={content}
		></textarea>
	</div>

	{#if !initial}
		<div class="form-field">
			<div class="form-label">Linked words</div>
			<WordSearchCombobox
				selectedWords={linkedWords}
				onchange={(words) => (linkedWords = words)}
			/>
		</div>
	{/if}

	<div class="form-actions">
		<button type="submit" class="btn btn-primary btn-sm" disabled={isPending}>
			{initial ? 'Save' : 'Add'}
		</button>
		<button type="button" class="btn-ghost" onclick={onCancel} disabled={isPending}>
			Cancel
		</button>
	</div>
</form>
