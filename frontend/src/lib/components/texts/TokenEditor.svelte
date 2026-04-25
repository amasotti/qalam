<script lang="ts">
import { Cpu, Plus, Trash2, X } from 'lucide-svelte';
import { untrack } from 'svelte';
import type { SentenceResponse } from '$lib/api/types.gen';
import { Button } from '$lib/components/ui/button';
import { useAutoTokenize, useClearTokens, useReplaceTokens } from '$lib/stores/texts';

interface TokenDraft {
	id: string;
	arabic: string;
	transliteration: string;
	translation: string;
}

interface Props {
	sentence: SentenceResponse;
	textId: string;
	onClose: () => void;
}

let { sentence, textId, onClose }: Props = $props();

const replaceTokens = useReplaceTokens();
const clearTokens = useClearTokens();
const autoTokenize = useAutoTokenize();

let drafts = $state<TokenDraft[]>(
	untrack(() =>
		sentence.tokens.map((t) => ({
			id: t.id,
			arabic: t.arabic,
			transliteration: t.transliteration ?? '',
			translation: t.translation ?? '',
		}))
	)
);

// Re-sync when external update changes token set (e.g., auto-tokenize fires while editor is open)
$effect(() => {
	const incomingIds = sentence.tokens.map((t) => t.id).join(',');
	const draftIds = untrack(() => drafts.map((d) => d.id).join(','));
	if (incomingIds !== draftIds) {
		drafts = sentence.tokens.map((t) => ({
			id: t.id,
			arabic: t.arabic,
			transliteration: t.transliteration ?? '',
			translation: t.translation ?? '',
		}));
	}
});

let clearConfirm = $state(false);
let error = $state<string | null>(null);

const isEmpty = $derived(drafts.length === 0);

async function handleAutoTokenize() {
	error = null;
	try {
		const updated = await autoTokenize.mutateAsync({ textId, id: sentence.id });
		drafts = updated.tokens.map((t) => ({
			id: t.id,
			arabic: t.arabic,
			transliteration: t.transliteration ?? '',
			translation: t.translation ?? '',
		}));
	} catch (err) {
		error = err instanceof Error ? err.message : 'Auto-tokenize failed';
	}
}

function addToken() {
	drafts.push({ id: crypto.randomUUID(), arabic: '', transliteration: '', translation: '' });
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
		wordId: null,
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
		drafts = [];
		clearConfirm = false;
	} catch (err) {
		error = err instanceof Error ? err.message : 'Clear failed';
	}
}
</script>

<div class="token-editor">
	<div class="token-editor-header">
		<span class="token-editor-title">Token table</span>
		<button class="token-editor-close" onclick={onClose} aria-label="Close">
			<X size={14} />
		</button>
	</div>

	{#if isEmpty}
		<div class="token-editor-empty">
			<p class="token-editor-empty-hint">
				AI will split, transliterate, and translate each token.
			</p>
			<div class="token-editor-empty-actions">
				<Button disabled={autoTokenize.isPending} onclick={handleAutoTokenize}>
					<Cpu size={14} />
					{autoTokenize.isPending ? 'Tokenizing…' : 'Auto-tokenize'}
				</Button>
				<Button variant="outline" onclick={addToken}>Add row manually</Button>
			</div>
		</div>
	{:else}
		<!-- Token table: one row per token, three columns -->
		<div class="token-table" role="table">
			<div class="token-table-head" role="row">
				<span class="token-col-label">Arabic</span>
				<span class="token-col-label">Transliteration</span>
				<span class="token-col-label">Translation</span>
				<span></span>
			</div>
			{#each drafts as draft, i (draft.id)}
				<div class="token-table-row" role="row">
					<input
						class="token-cell-input arabic-text"
						type="text"
						placeholder="عربي"
						bind:value={draft.arabic}
						aria-label="Arabic"
					/>
					<input
						class="token-cell-input transliteration"
						type="text"
						placeholder="translit."
						bind:value={draft.transliteration}
						aria-label="Transliteration"
					/>
					<input
						class="token-cell-input"
						type="text"
						placeholder="translation"
						bind:value={draft.translation}
						aria-label="Translation"
					/>
					<button
						class="token-row-remove"
						onclick={() => removeToken(i)}
						aria-label="Remove row"
					>
						<Trash2 size={12} />
					</button>
				</div>
			{/each}
		</div>

		<Button variant="outline" size="sm" onclick={addToken}>
			<Plus size={12} />
			Add row
		</Button>

		{#if error}
			<p class="token-editor-error">{error}</p>
		{/if}

		<div class="token-editor-actions">
			<Button size="sm" disabled={replaceTokens.isPending} onclick={handleSave}>
				{replaceTokens.isPending ? 'Saving…' : 'Save'}
			</Button>
			<Button size="sm" variant="outline" disabled={autoTokenize.isPending} onclick={handleAutoTokenize}>
				<Cpu size={12} />
				{autoTokenize.isPending ? '…' : 'Re-tokenize'}
			</Button>
			<Button
				size="sm"
				variant="outline"
				class="btn-outline-danger"
				disabled={clearTokens.isPending}
				onclick={handleClear}
			>
				<Trash2 size={12} />
				{clearConfirm ? 'Confirm?' : 'Clear'}
			</Button>
			<Button size="sm" variant="ghost" onclick={onClose}>Cancel</Button>
		</div>
	{/if}
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

.token-editor-empty {
	display: flex;
	flex-direction: column;
	gap: 0.75rem;
}

.token-editor-empty-hint {
	font-size: 0.8125rem;
	color: hsl(var(--muted-foreground));
	line-height: 1.5;
}

.token-editor-empty-actions {
	display: flex;
	gap: 0.5rem;
}

/* Table layout */
.token-table {
	display: grid;
	grid-template-columns: 1fr 1fr 1fr auto;
	gap: 0;
	border: 1px solid hsl(var(--border));
	border-radius: 0.375rem;
	overflow: hidden;
}

.token-table-head {
	display: contents;
}

.token-col-label {
	padding: 0.25rem 0.5rem;
	font-size: 0.6875rem;
	font-weight: 600;
	text-transform: uppercase;
	letter-spacing: 0.05em;
	color: hsl(var(--muted-foreground));
	background: hsl(var(--muted) / 0.5);
	border-bottom: 1px solid hsl(var(--border));
}

.token-table-row {
	display: contents;
}

.token-cell-input {
	border: none;
	border-bottom: 1px solid hsl(var(--border) / 0.5);
	border-right: 1px solid hsl(var(--border) / 0.5);
	padding: 0.3125rem 0.5rem;
	font-size: 0.875rem;
	background: hsl(var(--background));
	color: hsl(var(--foreground));
	min-width: 0;
}

.token-cell-input:last-of-type {
	border-right: none;
}

.token-cell-input:focus {
	outline: none;
	background: hsl(var(--primary) / 0.04);
}

.token-cell-input.arabic-text {
	font-size: 1rem;
	direction: rtl;
	text-align: right;
}

.token-cell-input.transliteration {
	font-style: italic;
	color: hsl(var(--muted-foreground));
}

.token-row-remove {
	border: none;
	border-bottom: 1px solid hsl(var(--border) / 0.5);
	background: hsl(var(--background));
	cursor: pointer;
	color: hsl(var(--muted-foreground));
	padding: 0 0.375rem;
	display: flex;
	align-items: center;
}

.token-row-remove:hover {
	color: hsl(var(--destructive));
	background: hsl(var(--destructive) / 0.06);
}

.token-editor-error {
	font-size: 0.8125rem;
	color: hsl(var(--destructive));
}

.token-editor-actions {
	display: flex;
	gap: 0.375rem;
	flex-wrap: wrap;
}
</style>
