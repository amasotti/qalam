<script lang="ts">
import { Check, ChevronDown, ChevronUp, Pencil, Plus, Trash2, X } from 'lucide-svelte';
import type { SentenceResponse } from '$lib/api/types.gen';
import { Button } from '$lib/components/ui/button';
import {
	useAutoTokenize,
	useCreateSentence,
	useDeleteSentence,
	useMarkTokensValid,
	useReorderSentences,
	useUpdateSentence,
} from '$lib/stores/texts';
import StaleTokenBanner from './StaleTokenBanner.svelte';
import TokenEditor from './TokenEditor.svelte';
import TokenGrid from './TokenGrid.svelte';

interface Props {
	sentences: SentenceResponse[];
	textId: string;
}

let { sentences, textId }: Props = $props();

const createSentence = useCreateSentence();
const updateSentence = useUpdateSentence();
const deleteSentence = useDeleteSentence();
const reorderSentences = useReorderSentences();
const autoTokenize = useAutoTokenize();
const markValid = useMarkTokensValid();

let editingId = $state<string | null>(null);
let tokenEditingId = $state<string | null>(null);
let deleteConfirm = $state<string | null>(null);
let deleteError = $state<string | null>(null);

let editArabic = $state('');
let editTranslit = $state('');
let editFreeTranslation = $state('');
let editNotes = $state('');
let editError = $state<string | null>(null);

let addingNew = $state(false);
let newArabic = $state('');
let newTranslit = $state('');
let newFreeTranslation = $state('');
let newNotes = $state('');
let newError = $state<string | null>(null);

function startEdit(s: SentenceResponse) {
	editingId = s.id;
	editArabic = s.arabicText;
	editTranslit = s.transliteration ?? '';
	editFreeTranslation = s.freeTranslation ?? '';
	editNotes = s.notes ?? '';
	editError = null;
}

function cancelEdit() {
	editingId = null;
	editError = null;
}

async function handleUpdate(sentence: SentenceResponse) {
	editError = null;
	if (!editArabic.trim()) {
		editError = 'Arabic text is required';
		return;
	}
	try {
		await updateSentence.mutateAsync({
			textId,
			id: sentence.id,
			body: {
				arabicText: editArabic.trim(),
				transliteration: editTranslit.trim() || null,
				freeTranslation: editFreeTranslation.trim() || null,
				notes: editNotes.trim() || null,
			},
		});
		editingId = null;
	} catch (err) {
		editError = err instanceof Error ? err.message : 'Update failed';
	}
}

async function handleDelete(id: string) {
	if (deleteConfirm !== id) {
		deleteConfirm = id;
		deleteError = null;
		setTimeout(() => (deleteConfirm = null), 3000);
		return;
	}
	try {
		await deleteSentence.mutateAsync({ textId, id });
	} catch (err) {
		deleteError = err instanceof Error ? err.message : 'Delete failed';
	}
	deleteConfirm = null;
}

async function handleAdd() {
	newError = null;
	if (!newArabic.trim()) {
		newError = 'Arabic text is required';
		return;
	}
	try {
		await createSentence.mutateAsync({
			textId,
			body: {
				arabicText: newArabic.trim(),
				transliteration: newTranslit.trim() || null,
				freeTranslation: newFreeTranslation.trim() || null,
				notes: newNotes.trim() || null,
			},
		});
		newArabic = '';
		newTranslit = '';
		newFreeTranslation = '';
		newNotes = '';
		addingNew = false;
	} catch (err) {
		newError = err instanceof Error ? err.message : 'Create failed';
	}
}

async function handleMarkValid(s: SentenceResponse) {
	await markValid.mutateAsync({ textId, id: s.id, currentTokens: s.tokens });
}

async function handleMoveUp(s: SentenceResponse) {
	if (s.position <= 1) return;
	const ordered = [...sentences].sort((a, b) => a.position - b.position);
	const idx = ordered.findIndex((x) => x.id === s.id);
	if (idx <= 0) return;
	[ordered[idx - 1], ordered[idx]] = [ordered[idx], ordered[idx - 1]];
	await reorderSentences.mutateAsync({ textId, orderedIds: ordered.map((x) => x.id) });
}

async function handleMoveDown(s: SentenceResponse, total: number) {
	if (s.position >= total) return;
	const ordered = [...sentences].sort((a, b) => a.position - b.position);
	const idx = ordered.findIndex((x) => x.id === s.id);
	if (idx < 0 || idx >= ordered.length - 1) return;
	[ordered[idx], ordered[idx + 1]] = [ordered[idx + 1], ordered[idx]];
	await reorderSentences.mutateAsync({ textId, orderedIds: ordered.map((x) => x.id) });
}
</script>

<div class="sentence-editor">
	{#each sentences as sentence (sentence.id)}
		<div class="sentence-edit-block">
			<div class="sentence-edit-meta">
				<span class="sentence-edit-pos">{sentence.position}</span>
				<div class="sentence-edit-order">
					<button
						class="order-btn"
						onclick={() => handleMoveUp(sentence)}
						disabled={sentence.position === 1 || reorderSentences.isPending}
						aria-label="Move sentence up"
					>
						<ChevronUp size={12} />
					</button>
					<button
						class="order-btn"
						onclick={() => handleMoveDown(sentence, sentences.length)}
						disabled={sentence.position === sentences.length || reorderSentences.isPending}
						aria-label="Move sentence down"
					>
						<ChevronDown size={12} />
					</button>
				</div>
			</div>

			<div class="sentence-edit-content">
				{#if editingId === sentence.id}
					<!-- Edit fields -->
					<div class="sentence-edit-form">
						<textarea
							class="sentence-edit-textarea arabic-text"
							rows={3}
							bind:value={editArabic}
							aria-label="Arabic text"
						></textarea>
						<input
							class="sentence-edit-input transliteration"
							type="text"
							placeholder="Transliteration"
							bind:value={editTranslit}
						/>
						<input
							class="sentence-edit-input"
							type="text"
							placeholder="Free translation"
							bind:value={editFreeTranslation}
						/>
						<input
							class="sentence-edit-input"
							type="text"
							placeholder="Notes"
							bind:value={editNotes}
						/>
						{#if editError}
							<p class="sentence-edit-error">{editError}</p>
						{/if}
						<div class="sentence-edit-form-actions">
							<Button
								size="sm"
								disabled={updateSentence.isPending}
								onclick={() => handleUpdate(sentence)}
							>
								<Check size={12} />
								Save
							</Button>
							<Button size="sm" variant="ghost" onclick={cancelEdit}>
								<X size={12} />
								Cancel
							</Button>
						</div>
					</div>
				{:else}
					<!-- Display in edit mode -->
					<div class="sentence-display">
						<div class="arabic-text sentence-arabic-display">{sentence.arabicText}</div>
						{#if sentence.transliteration}
							<div class="transliteration">{sentence.transliteration}</div>
						{/if}

						{#if !sentence.tokensValid && sentence.tokens.length > 0}
							<StaleTokenBanner
								onRetokenize={async () => { await autoTokenize.mutateAsync({ textId, id: sentence.id }); }}
								onMarkValid={() => handleMarkValid(sentence)}
								isPending={autoTokenize.isPending || markValid.isPending}
							/>
						{/if}

						{#if sentence.tokens.length > 0}
							<div class="sentence-tokens-wrap">
								<TokenGrid tokens={sentence.tokens} />
							</div>
						{/if}

						{#if sentence.freeTranslation}
							<div class="sentence-free-small">
								<span class="sentence-label">Trans.</span> {sentence.freeTranslation}
							</div>
						{/if}
						{#if sentence.notes}
							<div class="sentence-notes-small">
								<span class="sentence-label">Notes.</span> {sentence.notes}
							</div>
						{/if}
					</div>

					<div class="sentence-edit-toolbar">
						<Button size="sm" variant="outline" onclick={() => startEdit(sentence)}>
							<Pencil size={12} />
							Edit
						</Button>
						<Button
							size="sm"
							variant="outline"
							onclick={() => (tokenEditingId = tokenEditingId === sentence.id ? null : sentence.id)}
						>
							Tokens
						</Button>
						<Button
							size="sm"
							variant="outline"
							class="btn-outline-danger"
							disabled={deleteSentence.isPending}
							onclick={() => handleDelete(sentence.id)}
						>
							<Trash2 size={12} />
							{deleteConfirm === sentence.id ? 'Confirm?' : 'Delete'}
						</Button>
					</div>
				{/if}
				{#if deleteError}
					<p class="sentence-delete-error">{deleteError}</p>
				{/if}

				{#if tokenEditingId === sentence.id}
					<TokenEditor
						{sentence}
						{textId}
						onClose={() => (tokenEditingId = null)}
					/>
				{/if}
			</div>
		</div>
	{/each}

	<!-- Add new sentence -->
	{#if addingNew}
		<div class="new-sentence-form">
			<h4 class="new-sentence-heading">New sentence</h4>
			<textarea
				class="sentence-edit-textarea arabic-text"
				rows={3}
				placeholder="أدخل الجملة بالعربية…"
				bind:value={newArabic}
			></textarea>
			<input
				class="sentence-edit-input transliteration"
				type="text"
				placeholder="Transliteration"
				bind:value={newTranslit}
			/>
			<input
				class="sentence-edit-input"
				type="text"
				placeholder="Free translation"
				bind:value={newFreeTranslation}
			/>
			<input
				class="sentence-edit-input"
				type="text"
				placeholder="Notes"
				bind:value={newNotes}
			/>
			{#if newError}
				<p class="sentence-edit-error">{newError}</p>
			{/if}
			<div class="sentence-edit-form-actions">
				<Button
					size="sm"
					disabled={createSentence.isPending}
					onclick={handleAdd}
				>
					{createSentence.isPending ? 'Adding…' : 'Add sentence'}
				</Button>
				<Button size="sm" variant="ghost" onclick={() => (addingNew = false)}>Cancel</Button>
			</div>
		</div>
	{:else}
		<Button variant="outline" onclick={() => (addingNew = true)}>
			<Plus size={14} />
			Add sentence
		</Button>
	{/if}
</div>
