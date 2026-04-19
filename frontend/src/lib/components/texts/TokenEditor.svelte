<script lang="ts">
import { untrack } from 'svelte';
import { GripVertical, Plus, Trash2, X } from 'lucide-svelte';
import type { AlignmentTokenResponse, SentenceResponse } from '$lib/api/types.gen';
import { Button } from '$lib/components/ui/button';
import { useClearTokens, useReplaceTokens } from '$lib/stores/texts';

interface TokenDraft {
	id: string;
	arabic: string;
	transliteration: string;
	translation: string;
	wordId: string | null;
}

interface Props {
	sentence: SentenceResponse;
	textId: string;
	onClose: () => void;
}

let { sentence, textId, onClose }: Props = $props();

const replaceTokens = useReplaceTokens();
const clearTokens = useClearTokens();

let drafts = $state<TokenDraft[]>(
	untrack(() =>
		sentence.tokens.map((t) => ({
			id: t.id,
			arabic: t.arabic,
			transliteration: t.transliteration ?? '',
			translation: t.translation ?? '',
			wordId: t.wordId ?? null,
		}))
	)
);

let clearConfirm = $state(false);
let error = $state<string | null>(null);

function addToken() {
	drafts.push({
		id: crypto.randomUUID(),
		arabic: '',
		transliteration: '',
		translation: '',
		wordId: null,
	});
}

function removeToken(index: number) {
	drafts.splice(index, 1);
}

async function handleSave() {
	error = null;
	const tokens = drafts.map((d, i) => ({
		position: i + 1,
		arabic: d.arabic.trim(),
		transliteration: d.transliteration.trim() || null,
		translation: d.translation.trim() || null,
		wordId: d.wordId || null,
	}));

	if (tokens.some((t) => !t.arabic)) {
		error = 'All tokens must have Arabic text';
		return;
	}

	try {
		await replaceTokens.mutateAsync({ textId, id: sentence.id, body: { tokens } });
		onClose();
	} catch (err) {
		error = err instanceof Error ? err.message : 'Save failed';
	}
}

async function handleClear() {
	if (!clearConfirm) {
		clearConfirm = true;
		setTimeout(() => (clearConfirm = false), 3000);
		return;
	}
	try {
		await clearTokens.mutateAsync({ textId, id: sentence.id });
		onClose();
	} catch (err) {
		error = err instanceof Error ? err.message : 'Clear failed';
	}
}
</script>

<div class="token-editor">
	<div class="token-editor-header">
		<span class="token-editor-title">Edit tokens</span>
		<button class="token-editor-close" onclick={onClose} aria-label="Close token editor">
			<X size={14} />
		</button>
	</div>

	<div class="token-editor-hint">Right-to-left order — first token is rightmost word.</div>

	<div class="token-list">
		{#each drafts as draft, i (draft.id)}
			<div class="token-draft">
				<span class="token-drag-handle" aria-hidden="true"><GripVertical size={12} /></span>
				<div class="token-draft-fields">
					<input
						class="token-draft-input arabic"
						type="text"
						placeholder="عربي"
						bind:value={draft.arabic}
						aria-label="Arabic"
					/>
					<input
						class="token-draft-input transliteration"
						type="text"
						placeholder="translit."
						bind:value={draft.transliteration}
						aria-label="Transliteration"
					/>
					<input
						class="token-draft-input"
						type="text"
						placeholder="translation"
						bind:value={draft.translation}
						aria-label="Translation"
					/>
				</div>
				<button
					class="token-draft-remove"
					onclick={() => removeToken(i)}
					aria-label="Remove token"
				>
					<Trash2 size={12} />
				</button>
			</div>
		{/each}
	</div>

	<Button variant="outline" size="sm" onclick={addToken}>
		<Plus size={12} />
		Add token
	</Button>

	{#if error}
		<p class="token-editor-error">{error}</p>
	{/if}

	<div class="token-editor-actions">
		<Button
			size="sm"
			disabled={replaceTokens.isPending}
			onclick={handleSave}
		>
			{replaceTokens.isPending ? 'Saving…' : 'Save tokens'}
		</Button>
		<Button
			size="sm"
			variant="outline"
			class="btn-outline-danger"
			disabled={clearTokens.isPending}
			onclick={handleClear}
		>
			{clearConfirm ? 'Confirm clear?' : 'Clear all'}
		</Button>
		<Button size="sm" variant="ghost" onclick={onClose}>Cancel</Button>
	</div>
</div>

<style>
.token-editor {
	display: flex;
	flex-direction: column;
	gap: 0.75rem;
	padding: 1rem;
	border: 1px solid hsl(var(--border));
	border-radius: 0.5rem;
	background: hsl(var(--muted) / 0.2);
}

.token-editor-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.token-editor-title {
	font-size: 0.8125rem;
	font-weight: 600;
}

.token-editor-close {
	border: none;
	background: none;
	cursor: pointer;
	color: hsl(var(--muted-foreground));
	padding: 0.125rem;
	border-radius: 0.25rem;
	display: flex;
	align-items: center;
}

.token-editor-close:hover {
	color: hsl(var(--foreground));
	background: hsl(var(--muted));
}

.token-editor-hint {
	font-size: 0.75rem;
	color: hsl(var(--muted-foreground));
}

.token-list {
	display: flex;
	flex-direction: column;
	gap: 0.375rem;
}

.token-draft {
	display: flex;
	align-items: center;
	gap: 0.375rem;
}

.token-drag-handle {
	color: hsl(var(--muted-foreground));
	cursor: grab;
	flex-shrink: 0;
}

.token-draft-fields {
	display: flex;
	gap: 0.25rem;
	flex: 1;
}

.token-draft-input {
	flex: 1;
	border: 1px solid hsl(var(--border));
	border-radius: 0.25rem;
	padding: 0.25rem 0.375rem;
	font-size: 0.8125rem;
	background: hsl(var(--background));
	color: hsl(var(--foreground));
	min-width: 0;
}

.token-draft-input:focus {
	outline: none;
	border-color: hsl(var(--primary));
}

.token-draft-remove {
	border: none;
	background: none;
	cursor: pointer;
	color: hsl(var(--muted-foreground));
	padding: 0.25rem;
	border-radius: 0.25rem;
	display: flex;
	align-items: center;
	flex-shrink: 0;
}

.token-draft-remove:hover {
	color: hsl(var(--destructive));
	background: hsl(var(--destructive) / 0.08);
}

.token-editor-error {
	font-size: 0.8125rem;
	color: hsl(var(--destructive));
}

.token-editor-actions {
	display: flex;
	gap: 0.375rem;
}
</style>
