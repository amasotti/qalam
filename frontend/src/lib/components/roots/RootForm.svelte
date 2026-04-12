<script lang="ts">
import type { CreateRootRequest, NormalizeResponse, UpdateRootRequest } from '$lib/api/types.gen';
import { Button } from '$lib/components/ui/button';
import { useNormalizeRoot } from '$lib/stores/roots';
import { untrack } from 'svelte';

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
		<div class="root-form-field">
			<label class="root-form-label" for="root-letters">Root letters</label>
			<input
				id="root-letters"
				class="root-form-input arabic-input"
				type="text"
				placeholder="مثل: ك ت ب أو كتب"
				bind:value={rootInput}
				autocomplete="off"
				spellcheck="false"
				disabled={isPending}
				required
			/>
			<p class="root-form-hint">
				Space, dash, comma-separated — or concatenated Arabic consonants
			</p>
			{#if normalizeError}
				<p class="root-form-error">{normalizeError}</p>
			{/if}
		</div>

		{#if preview}
			<div class="root-form-preview">
				<span class="root-form-preview-arabic">{preview.displayForm}</span>
				<div class="root-form-preview-meta">
					<span class="root-form-preview-normalized">{preview.normalizedForm}</span>
					<span class="root-form-preview-count">{preview.letterCount} letters</span>
				</div>
			</div>
		{/if}
	{/if}

	<div class="root-form-field">
		<label class="root-form-label" for="root-meaning">Meaning</label>
		<input
			id="root-meaning"
			class="root-form-input"
			type="text"
			placeholder="e.g. to write, writing"
			bind:value={meaning}
			disabled={isPending}
		/>
	</div>

	<div class="root-form-field">
		<label class="root-form-label" for="root-analysis">Analysis</label>
		<textarea
			id="root-analysis"
			class="root-form-textarea"
			placeholder="Morphological notes, semantic range… (Markdown supported)"
			rows={20}
			bind:value={analysis}
			disabled={isPending}
		></textarea>
		<p class="root-form-hint">Markdown is rendered on the detail page.</p>
	</div>

	{#if submitError}
		<p class="root-form-error">{submitError}</p>
	{/if}

	<div class="root-form-actions">
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
