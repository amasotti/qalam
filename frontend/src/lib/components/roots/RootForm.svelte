<script lang="ts">
import { untrack } from 'svelte';
import type { CreateRootRequest, NormalizeResponse, UpdateRootRequest } from '$lib/api/types.gen';
import { Button } from '$lib/components/ui/button';
import { useNormalizeRoot } from '$lib/stores/roots';

interface Props {
	/** Prefill values (edit mode) */
	initial?: { root?: string; meaning?: string | null; analysis?: string | null };
	/** If true, the root-letters field is hidden — only meaning/analysis are editable */
	isEdit?: boolean;
	isPending?: boolean;
	onSubmit: (req: CreateRootRequest | UpdateRootRequest) => Promise<void>;
	onCancel: () => void;
}

let { initial = {}, isEdit = false, isPending = false, onSubmit, onCancel }: Props = $props();

// untrack: initial values are one-shot seeds — edits are not synced back to the parent.
let rootInput = $state(untrack(() => initial.root ?? ''));
let meaning = $state(untrack(() => initial.meaning ?? ''));
let analysis = $state(untrack(() => initial.analysis ?? ''));
let preview = $state<NormalizeResponse | null>(null);
let normalizeError = $state<string | null>(null);
let submitError = $state<string | null>(null);

const normalize = useNormalizeRoot();

$effect(() => {
	const val = rootInput.trim();
	if (!val || isEdit) {
		preview = null;
		normalizeError = null;
		return;
	}
	const timer = setTimeout(() => {
		normalize.mutate(val, {
			onSuccess: (data) => {
				preview = data;
				normalizeError = null;
			},
			onError: () => {
				preview = null;
				normalizeError = 'Invalid root format — enter 2–6 Arabic consonants';
			},
		});
	}, 300);
	return () => clearTimeout(timer);
});

async function handleSubmit(e: SubmitEvent) {
	e.preventDefault();
	submitError = null;
	try {
		if (isEdit) {
			await onSubmit({
				meaning: meaning.trim() || null,
				analysis: analysis.trim() || null,
			} satisfies UpdateRootRequest);
		} else {
			if (!rootInput.trim()) return;
			await onSubmit({
				root: rootInput.trim(),
				meaning: meaning.trim() || null,
				analysis: analysis.trim() || null,
			} satisfies CreateRootRequest);
		}
	} catch (err) {
		submitError = err instanceof Error ? err.message : 'Something went wrong';
	}
}
</script>

<form class="root-form" onsubmit={handleSubmit} novalidate>
	{#if !isEdit}
		<div class="form-field">
			<label class="form-label" for="root-letters">Root letters</label>
			<input
				id="root-letters"
				class="form-input ar-input"
				type="text"
				placeholder="مثل: ك ت ب أو كتب"
				bind:value={rootInput}
				autocomplete="off"
				spellcheck="false"
				disabled={isPending}
				required
			/>
			<p class="form-hint">
				Space, dash, comma-separated — or concatenated Arabic consonants
			</p>
			{#if normalizeError}
				<p class="form-error">{normalizeError}</p>
			{/if}
		</div>

		{#if preview}
			<div class="root-preview">
				<span class="root-preview-ar">{preview.displayForm}</span>
				<div class="root-preview-meta">
					<span class="root-preview-normalized">{preview.normalizedForm}</span>
					<span class="root-badge">{preview.letterCount} letters</span>
				</div>
			</div>
		{/if}
	{/if}

	<div class="form-field">
		<label class="form-label" for="root-meaning">Meaning</label>
		<input
			id="root-meaning"
			class="form-input"
			type="text"
			placeholder="e.g. to write, writing"
			bind:value={meaning}
			disabled={isPending}
		/>
	</div>

	<div class="form-field">
		<label class="form-label" for="root-analysis">Analysis</label>
		<textarea
			id="root-analysis"
			class="form-textarea"
			placeholder="Morphological notes, semantic range… (Markdown supported)"
			rows={20}
			bind:value={analysis}
			disabled={isPending}
		></textarea>
		<p class="form-hint">Markdown is rendered on the detail page.</p>
	</div>

	{#if submitError}
		<p class="form-error">{submitError}</p>
	{/if}

	<div class="form-actions">
		<Button variant="ghost" type="button" onclick={onCancel} disabled={isPending}>
			Cancel
		</Button>
		<Button
			type="submit"
			disabled={isPending || (!isEdit && !rootInput.trim())}
		>
			{isPending ? 'Saving…' : isEdit ? 'Update root' : 'Create root'}
		</Button>
	</div>
</form>

<style>
.root-form {
	display: flex;
	flex-direction: column;
	gap: 1.5rem;
}

/* Normalize preview block */
.root-preview {
	display: flex;
	align-items: center;
	gap: 1rem;
	padding: 0.875rem 1.125rem;
	background: var(--bg-dark);
	border: 1px solid var(--border);
	border-radius: 8px;
}
.root-preview-ar {
	font-family: "Amiri", serif;
	font-size: 2.5rem;
	direction: rtl;
	color: var(--ink);
	line-height: 1;
}
.root-preview-meta {
	display: flex;
	flex-direction: column;
	gap: 0.25rem;
}
.root-preview-normalized {
	font-family: "Amiri", serif;
	font-size: 1rem;
	color: var(--ink-light);
	direction: rtl;
	font-style: italic;
}
</style>
