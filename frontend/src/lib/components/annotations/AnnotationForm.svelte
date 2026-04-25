<script lang="ts">
import type { AnnotationResponse, WordAutocompleteResponse } from '$lib/api/types.gen';
import { useCreateAnnotation, useUpdateAnnotation } from '$lib/stores/annotations';
import WordSearchCombobox from './WordSearchCombobox.svelte';

const TYPES = ['VOCABULARY', 'GRAMMAR', 'CULTURAL', 'OTHER'] as const;
type AnnotationType = (typeof TYPES)[number];

interface Props {
	textId: string;
	anchor: string;
	initial?: AnnotationResponse;
	onSuccess: () => void;
	onCancel: () => void;
}

let { textId, anchor, initial, onSuccess, onCancel }: Props = $props();

let type = $state<AnnotationType>((initial?.type as AnnotationType) ?? 'VOCABULARY');
let content = $state(initial?.content ?? '');
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

<form class="annotation-form" onsubmit={handleSubmit}>
	<div class="form-field">
		<label class="form-label">Type</label>
		<div class="type-selector">
			{#each TYPES as t}
				<button
					type="button"
					class="type-btn"
					class:type-btn-active={type === t}
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
			<label class="form-label">Linked words</label>
			<WordSearchCombobox
				selectedWords={linkedWords}
				onchange={(words) => (linkedWords = words)}
			/>
		</div>
	{/if}

	<div class="form-actions">
		<button type="submit" class="btn-primary" disabled={isPending}>
			{initial ? 'Save' : 'Add'}
		</button>
		<button type="button" class="btn-ghost" onclick={onCancel} disabled={isPending}>
			Cancel
		</button>
	</div>
</form>

<style>
.annotation-form { display: flex; flex-direction: column; gap: 0.875rem; }

.form-field { display: flex; flex-direction: column; gap: 0.3rem; }

.form-label {
	font-size: 0.7rem;
	font-weight: 600;
	text-transform: uppercase;
	letter-spacing: 0.05em;
	color: hsl(var(--muted-foreground));
}

.type-selector { display: flex; gap: 0.25rem; flex-wrap: wrap; }

.type-btn {
	padding: 0.25rem 0.625rem;
	border-radius: 0.375rem;
	font-size: 0.75rem;
	border: 1px solid hsl(var(--border));
	background: transparent;
	color: hsl(var(--muted-foreground));
	cursor: pointer;
}
.type-btn:hover { background: hsl(var(--muted)); color: hsl(var(--foreground)); }
.type-btn-active { background: hsl(var(--primary) / 0.15); border-color: hsl(var(--primary) / 0.5); color: hsl(var(--primary)); }

.form-textarea {
	padding: 0.375rem 0.625rem;
	background: hsl(var(--muted) / 0.5);
	border: 1px solid hsl(var(--border));
	border-radius: 0.375rem;
	font-size: 0.8125rem;
	color: hsl(var(--foreground));
	resize: vertical;
	outline: none;
	font-family: inherit;
	line-height: 1.5;
}
.form-textarea:focus { border-color: hsl(var(--primary) / 0.6); }

.form-actions { display: flex; gap: 0.5rem; justify-content: flex-end; }

.btn-primary {
	padding: 0.375rem 0.875rem;
	border-radius: 0.375rem;
	font-size: 0.8125rem;
	font-weight: 600;
	background: hsl(var(--primary));
	color: hsl(var(--primary-foreground));
	border: none;
	cursor: pointer;
}
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }

.btn-ghost {
	padding: 0.375rem 0.875rem;
	border-radius: 0.375rem;
	font-size: 0.8125rem;
	background: transparent;
	color: hsl(var(--muted-foreground));
	border: 1px solid hsl(var(--border));
	cursor: pointer;
}
.btn-ghost:hover { color: hsl(var(--foreground)); background: hsl(var(--muted)); }
</style>
