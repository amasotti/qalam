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

let {
	initial = {},
	isEdit = false,
	isPending = false,
	onSubmit,
	onCancel,
}: Props = $props();

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
	if (!body.trim()) {
		submitError = 'Arabic body is required';
		return;
	}

	try {
		const req: CreateTextRequest | UpdateTextRequest = {
			title: title.trim(),
			body: body.trim(),
			transliteration: transliteration.trim() || null,
			translation: translation.trim() || null,
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

<form class="text-form" onsubmit={handleSubmit}>
	<div class="text-form-field">
		<label class="text-form-label" for="tf-title">Title</label>
		<input
			id="tf-title"
			class="text-form-input"
			type="text"
			placeholder="Text title"
			bind:value={title}
			required
		/>
	</div>

	<div class="text-form-row">
		<div class="text-form-field">
			<label class="text-form-label" for="tf-dialect">Dialect</label>
			<select id="tf-dialect" class="text-form-select" bind:value={dialect}>
				<option value="MSA">MSA</option>
				<option value="TUNISIAN">Tunisian</option>
				<option value="MOROCCAN">Moroccan</option>
				<option value="EGYPTIAN">Egyptian</option>
				<option value="GULF">Gulf</option>
				<option value="LEVANTINE">Levantine</option>
				<option value="IRAQI">Iraqi</option>
			</select>
		</div>

		<div class="text-form-field">
			<label class="text-form-label" for="tf-difficulty">Difficulty</label>
			<select id="tf-difficulty" class="text-form-select" bind:value={difficulty}>
				<option value="BEGINNER">Beginner</option>
				<option value="INTERMEDIATE">Intermediate</option>
				<option value="ADVANCED">Advanced</option>
			</select>
		</div>
	</div>

	<div class="text-form-field">
		<label class="text-form-label" for="tf-body">Arabic body</label>
		<textarea
			id="tf-body"
			class="text-form-textarea arabic-text"
			rows={6}
			placeholder="أدخل النص العربي هنا…"
			bind:value={body}
			required
		></textarea>
	</div>

	<div class="text-form-field">
		<label class="text-form-label" for="tf-translit">Transliteration</label>
		<textarea
			id="tf-translit"
			class="text-form-textarea transliteration"
			rows={3}
			placeholder="Latin transliteration…"
			bind:value={transliteration}
		></textarea>
	</div>

	<div class="text-form-field">
		<label class="text-form-label" for="tf-translation">Translation</label>
		<textarea
			id="tf-translation"
			class="text-form-textarea"
			rows={3}
			placeholder="Free translation…"
			bind:value={translation}
		></textarea>
	</div>

	<div class="text-form-field">
		<label class="text-form-label" for="tf-tags">Tags</label>
		<input
			id="tf-tags"
			class="text-form-input"
			type="text"
			placeholder="poetry, classical, Naguib Mahfouz (comma-separated)"
			bind:value={tagsRaw}
		/>
		{#if tags.length > 0}
			<p class="text-form-hint">{tags.join(' · ')}</p>
		{/if}
	</div>

	<div class="text-form-field">
		<label class="text-form-label" for="tf-comments">Comments</label>
		<textarea
			id="tf-comments"
			class="text-form-textarea"
			rows={2}
			placeholder="Notes about this text…"
			bind:value={comments}
		></textarea>
	</div>

	{#if submitError}
		<p class="text-form-error">{submitError}</p>
	{/if}

	<div class="text-form-actions">
		<Button type="submit" disabled={isPending}>
			{isPending ? 'Saving…' : isEdit ? 'Save changes' : 'Create text'}
		</Button>
		<Button type="button" variant="outline" onclick={onCancel}>Cancel</Button>
	</div>
</form>

<style>
.text-form {
	display: flex;
	flex-direction: column;
	gap: 1rem;
	max-width: 680px;
}

.text-form-row {
	display: grid;
	grid-template-columns: 1fr 1fr;
	gap: 1rem;
}

.text-form-field {
	display: flex;
	flex-direction: column;
	gap: 0.375rem;
}

.text-form-label {
	font-size: 0.8125rem;
	font-weight: 500;
	color: hsl(var(--foreground));
}

.text-form-input,
.text-form-select {
	border: 1px solid hsl(var(--border));
	border-radius: 0.375rem;
	padding: 0.375rem 0.625rem;
	font-size: 0.875rem;
	background: hsl(var(--background));
	color: hsl(var(--foreground));
}

.text-form-input:focus,
.text-form-select:focus {
	outline: none;
	border-color: hsl(var(--primary));
}

.text-form-textarea {
	border: 1px solid hsl(var(--border));
	border-radius: 0.375rem;
	padding: 0.5rem 0.625rem;
	font-size: 0.9375rem;
	background: hsl(var(--background));
	color: hsl(var(--foreground));
	resize: vertical;
	font-family: inherit;
}

.text-form-textarea:focus {
	outline: none;
	border-color: hsl(var(--primary));
}

.text-form-hint {
	font-size: 0.75rem;
	color: hsl(var(--muted-foreground));
}

.text-form-error {
	font-size: 0.875rem;
	color: hsl(var(--destructive));
}

.text-form-actions {
	display: flex;
	gap: 0.5rem;
}
</style>
