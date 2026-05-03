<script lang="ts">
import { untrack } from 'svelte';
import type { CreateTextRequest, Dialect, Difficulty, UpdateTextRequest } from '$lib/api/types.gen';
import { Button } from '$lib/components/ui/button';

interface Props {
	initial?: Partial<{
		title: string;
		body: string;
		transliteration: string | null;
		translation: string | null;
		dialect: Dialect;
		difficulty: Difficulty;
		comments: string | null;
		tags: string[];
	}>;
	isEdit?: boolean;
	isPending?: boolean;
	onSubmit: (req: CreateTextRequest | UpdateTextRequest) => Promise<void>;
	onCancel: () => void;
}

let { initial = {}, isEdit = false, isPending = false, onSubmit, onCancel }: Props = $props();

let title = $state(untrack(() => initial.title ?? ''));
let body = $state(untrack(() => initial.body ?? ''));
let transliteration = $state(untrack(() => initial.transliteration ?? ''));
let translation = $state(untrack(() => initial.translation ?? ''));
let dialect = $state<Dialect>(untrack(() => initial.dialect ?? 'MSA'));
let difficulty = $state<Difficulty>(untrack(() => initial.difficulty ?? 'BEGINNER'));
let comments = $state(untrack(() => initial.comments ?? ''));
let tagsRaw = $state(untrack(() => (initial.tags ?? []).join(', ')));

let submitError = $state<string | null>(null);

const tags = $derived(
	tagsRaw
		.split(',')
		.map((t) => t.trim())
		.filter(Boolean)
);

async function handleSubmit(e: SubmitEvent) {
	e.preventDefault();
	submitError = null;

	if (!title.trim()) {
		submitError = 'Title is required';
		return;
	}

	try {
		const req: CreateTextRequest | UpdateTextRequest = {
			title: title.trim(),
			body: isEdit ? body.trim() : '',
			transliteration: isEdit ? transliteration.trim() || null : null,
			translation: isEdit ? translation.trim() || null : null,
			dialect,
			difficulty,
			comments: comments.trim() || null,
			tags,
		};
		await onSubmit(req);
	} catch (err) {
		submitError = err instanceof Error ? err.message : 'Save failed';
	}
}
</script>

<form class="form-shell" onsubmit={handleSubmit}>
	<div class="form-field">
		<label class="form-label" for="tf-title">Title</label>
		<input
			id="tf-title"
			class="form-input"
			type="text"
			placeholder="Text title"
			bind:value={title}
			required
		/>
	</div>

	<div class="form-row-2">
		<div class="form-field">
			<label class="form-label" for="tf-dialect">Dialect</label>
			<select id="tf-dialect" class="form-select" bind:value={dialect}>
				<option value="MSA">MSA</option>
				<option value="TUNISIAN">Tunisian</option>
				<option value="MOROCCAN">Moroccan</option>
				<option value="EGYPTIAN">Egyptian</option>
				<option value="GULF">Gulf</option>
				<option value="LEVANTINE">Levantine</option>
				<option value="IRAQI">Iraqi</option>
			</select>
		</div>

		<div class="form-field">
			<label class="form-label" for="tf-difficulty">Difficulty</label>
			<select id="tf-difficulty" class="form-select" bind:value={difficulty}>
				<option value="BEGINNER">Beginner</option>
				<option value="INTERMEDIATE">Intermediate</option>
				<option value="ADVANCED">Advanced</option>
			</select>
		</div>
	</div>

	{#if isEdit}
		<div class="form-field">
			<label class="form-label" for="tf-body">Arabic body</label>
			<textarea
				id="tf-body"
				class="form-textarea arabic-text"
				rows={5}
				placeholder="Full Arabic text (optional — used as reading reference)"
				bind:value={body}
			></textarea>
		</div>

		<div class="form-field">
			<label class="form-label" for="tf-translit">Transliteration</label>
			<textarea
				id="tf-translit"
				class="form-textarea transliteration"
				rows={3}
				placeholder="Latin transliteration of the full text…"
				bind:value={transliteration}
			></textarea>
		</div>

		<div class="form-field">
			<label class="form-label" for="tf-translation">Translation</label>
			<textarea
				id="tf-translation"
				class="form-textarea"
				rows={3}
				placeholder="Free translation of the full text…"
				bind:value={translation}
			></textarea>
		</div>
	{/if}

	<div class="form-field">
		<label class="form-label" for="tf-tags">Tags</label>
		<input
			id="tf-tags"
			class="form-input"
			type="text"
			placeholder="poetry, classical, Naguib Mahfouz (comma-separated)"
			bind:value={tagsRaw}
		/>
		{#if tags.length > 0}
			<p class="form-hint">{tags.join(' · ')}</p>
		{/if}
	</div>

	<div class="form-field">
		<label class="form-label" for="tf-comments">Comments</label>
		<textarea
			id="tf-comments"
			class="form-textarea"
			rows={2}
			placeholder="Notes about this text…"
			bind:value={comments}
		></textarea>
	</div>

	{#if submitError}
		<p class="form-error">{submitError}</p>
	{/if}

	<div class="form-actions">
		<Button type="submit" disabled={isPending}>
			{isPending ? 'Saving…' : isEdit ? 'Save changes' : 'Create text'}
		</Button>
		<Button type="button" variant="outline" onclick={onCancel}>Cancel</Button>
	</div>
</form>
