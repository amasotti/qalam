<script lang="ts">
import { goto } from '$app/navigation';
import { page } from '$app/state';
import type { UpdateWordRequest } from '$lib/api/types.gen';
import AiInsightPanel from '$lib/components/ai/AiInsightPanel.svelte';
import AnnotationBadge from '$lib/components/annotations/AnnotationBadge.svelte';
import WordEnrichDrawer from '$lib/components/word/WordEnrichDrawer.svelte';
import WordMorphologyStrip from '$lib/components/word/WordMorphologyStrip.svelte';
import WordPluralChips from '$lib/components/word/WordPluralChips.svelte';
import WordRelationsPanel from '$lib/components/word/WordRelationsPanel.svelte';
import AiExamples from '$lib/components/words/AiExamples.svelte';
import DictionaryLinks from '$lib/components/words/DictionaryLinks.svelte';
import WordForm from '$lib/components/words/WordForm.svelte';
import { useRoot } from '$lib/stores/roots';
import {
	useDeleteWord,
	useDeleteWordExample,
	useSaveWordExample,
	useUpdateWord,
	useWord,
	useWordAnnotations,
	useWordExamples,
} from '$lib/stores/words';

const id = $derived(page.params.id ?? '');
const word = useWord(() => id);
const annotations = useWordAnnotations(() => id);
const examples = useWordExamples(() => id);
const updateWord = useUpdateWord();
const deleteWord = useDeleteWord();
const deleteExample = useDeleteWordExample();
const saveExample = useSaveWordExample();
const root = useRoot(() => word.data?.rootId ?? undefined);

let isEditing = $state(false);
let deleteConfirm = $state(false);
let enrichOpen = $state(false);
let aiUnavailable = $state(false);

let addingExample = $state(false);
let newExAr = $state('');
let newExTr = $state('');
let newExEn = $state('');

// Notes inline edit
let editingNotes = $state(false);
let editedNotes = $state('');

function startEditNotes() {
	editedNotes = word.data?.notes ?? '';
	editingNotes = true;
}

async function saveNotes() {
	await updateWord.mutateAsync({ id, body: { notes: editedNotes.trim() || null } });
	editingNotes = false;
}

async function handleUpdate(req: UpdateWordRequest) {
	await updateWord.mutateAsync({ id, body: req });
	isEditing = false;
}

async function handleDelete() {
	if (!deleteConfirm) {
		deleteConfirm = true;
		setTimeout(() => {
			deleteConfirm = false;
		}, 3000);
		return;
	}
	await deleteWord.mutateAsync(id);
	goto('/words');
}

async function handleSaveExample() {
	if (!newExAr.trim()) return;
	await saveExample.mutateAsync({
		id,
		body: {
			arabic: newExAr.trim(),
			transliteration: newExTr.trim() || null,
			translation: newExEn.trim() || null,
		},
	});
	newExAr = '';
	newExTr = '';
	newExEn = '';
	addingExample = false;
}

function cancelAddExample() {
	newExAr = '';
	newExTr = '';
	newExEn = '';
	addingExample = false;
}

function formatEnum(value: string): string {
	return value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();
}

const masterySteps: Record<string, number> = {
	NEW: 1,
	LEARNING: 2,
	FAMILIAR: 3,
	KNOWN: 4,
	MASTERED: 5,
};
</script>

{#if word.isPending}
	<p class="detail-status status-text status-text-muted">Loading…</p>
{:else if word.isError}
	<p class="detail-status status-text status-text-danger">Word not found.</p>
{:else if word.data}
	{#if isEditing}
		<!-- ── Edit mode ── -->
		<div class="detail-edit-content">
			<div class="word-hero detail-edit-hero">
				<div class="word-hero-ar">{word.data.arabicText}</div>
				{#if word.data.transliteration}
					<div class="word-hero-tr">{word.data.transliteration}</div>
				{/if}
			</div>
			<WordForm
				isEdit
				selfId={id}
				initial={{
					arabicText: word.data.arabicText,
					transliteration: word.data.transliteration ?? null,
					translation: word.data.translation ?? null,
					partOfSpeech: word.data.partOfSpeech,
					dialect: word.data.dialect,
					difficulty: word.data.difficulty,
					pronunciationUrl: word.data.pronunciationUrl ?? null,
					rootId: word.data.rootId ?? null,
					derivedFromId: word.data.derivedFromId ?? null,
				}}
				isPending={updateWord.isPending}
				onSubmit={handleUpdate}
				onCancel={() => (isEditing = false)}
			/>
		</div>
	{:else}
		<!-- ── View mode ── -->
		<nav class="breadcrumb">
			<a href="/words">Words</a>
			<span class="bc-sep">/</span>
			<span class="bc-cur">{word.data.arabicText}</span>
		</nav>

		<div class="detail-layout detail-layout-word">
			<div class="detail-content">
				<!-- Hero -->
				<div class="word-hero">
					<div class="word-hero-ghost">{word.data.arabicText[0] ?? ''}</div>
					<div class="word-hero-ar">{word.data.arabicText}</div>
					{#if word.data.transliteration}
						<div class="word-hero-tr">{word.data.transliteration}</div>
					{/if}
					<div class="word-hero-row">
						<div class="chips">
							<span class="chip c-olive">{formatEnum(word.data.partOfSpeech)}</span>
							<span class="chip c-cerulean">{word.data.dialect}</span>
							<span class="chip c-coral">{formatEnum(word.data.difficulty)}</span>
							<span class="chip c-coral">{formatEnum(word.data.masteryLevel)}</span>
						</div>
						<div class="word-actions">
							<button
								class="btn btn-primary"
								onclick={() => { isEditing = true; deleteConfirm = false; }}
							>Edit</button>
							{#if !aiUnavailable}
								<button
									class="btn"
									onclick={() => (enrichOpen = true)}
								>✦ AI Enrich</button>
							{/if}
							<button
								class="btn btn-danger"
								onclick={handleDelete}
								disabled={deleteWord.isPending}
							>{deleteConfirm ? 'Confirm delete' : 'Delete'}</button>
						</div>
					</div>
				</div>

				<!-- Translation + Pronunciation -->
				<div class="section-block">
					<div class="sect-label">Translation</div>
					{#if word.data.translation}
						<p class="word-translation">{word.data.translation}</p>
					{:else}
						<p class="annot-empty">No translation recorded</p>
					{/if}
					{#if word.data.pronunciationUrl}
						<div class="section-block-sm">
							<a
								class="pron-link"
								href={word.data.pronunciationUrl}
								target="_blank"
								rel="noopener noreferrer"
							>♪ Forvo ↗</a>
						</div>
					{/if}
				</div>

				<!-- Morphology + Plurals -->
				<WordMorphologyStrip wordId={id} />
				<WordPluralChips wordId={id} />

				<hr class="sect-divider" />

				<!-- Examples -->
				<div class="sect-label">
					<span>Examples</span>
					{#if !addingExample}
						<button
							class="btn btn-sm"
							onclick={() => (addingExample = true)}
						>+ Add</button>
					{/if}
				</div>
				<div class="word-examples">
					{#if addingExample}
						<div class="example-add-form">
							<textarea
								class="example-input-ar"
								rows="2"
								placeholder="العربية…"
								bind:value={newExAr}
							></textarea>
							<input
								class="example-input-latin"
								type="text"
								placeholder="Transliteration (optional)"
								bind:value={newExTr}
							/>
							<input
								class="example-input-latin"
								type="text"
								placeholder="Translation (optional)"
								bind:value={newExEn}
							/>
							<div class="example-form-actions">
								<button class="btn" onclick={cancelAddExample}>Cancel</button>
								<button
									class="btn btn-primary"
									onclick={handleSaveExample}
									disabled={saveExample.isPending || !newExAr.trim()}
								>Save</button>
							</div>
						</div>
					{/if}

					{#if examples.isPending}
						<p class="status-text status-text-muted">Loading…</p>
					{:else if (examples.data ?? []).length === 0 && !addingExample}
						<p class="annot-empty">No examples saved yet</p>
					{:else}
						{#each examples.data ?? [] as ex (ex.id)}
							<div class="example-card">
								<p class="example-card-ar">{ex.arabic}</p>
								{#if ex.transliteration}<p class="example-card-tr">{ex.transliteration}</p>{/if}
								{#if ex.translation}<p class="example-card-en">{ex.translation}</p>{/if}
								<button
									class="example-delete"
									onclick={() => deleteExample.mutate({ id, exampleId: ex.id })}
									disabled={deleteExample.isPending}
									aria-label="Delete example"
								>×</button>
							</div>
						{/each}
					{/if}

					<!-- AI examples -->
					<AiExamples wordId={id} />
				</div>

				<!-- AI insight -->
				<AiInsightPanel entityType="WORD" entityId={id} />

				<hr class="sect-divider" />

				<!-- Relations -->
				<div class="sect-label">Relations</div>
				<WordRelationsPanel wordId={id} />

				<hr class="sect-divider" />

				<!-- Notes -->
				<div class="sect-label">
					<span>Notes</span>
					{#if !editingNotes}
						<button
							class="btn btn-xs"
							onclick={startEditNotes}
						>✏ Edit</button>
					{/if}
				</div>
				{#if editingNotes}
					<div class="stack-xs section-block">
						<textarea
							class="notes-textarea"
							rows="5"
							bind:value={editedNotes}
							disabled={updateWord.isPending}
						></textarea>
						<div class="row-sm">
							<button
								class="btn btn-primary btn-sm"
								onclick={saveNotes}
								disabled={updateWord.isPending}
							>{updateWord.isPending ? 'Saving…' : 'Save'}</button>
							<button
								class="btn btn-sm"
								onclick={() => (editingNotes = false)}
								disabled={updateWord.isPending}
							>Cancel</button>
						</div>
					</div>
				{:else if word.data.notes}
					<p class="notes-text">{word.data.notes}</p>
				{:else}
					<p class="annot-empty section-block">No notes yet.</p>
				{/if}

				<hr class="sect-divider" />

				<!-- Dictionary sources -->
				<div class="sect-label">Dictionary sources</div>
				<div class="section-block-lg">
					<DictionaryLinks wordId={id} arabicText={word.data.arabicText} />
				</div>

				<!-- Annotations -->
				<div class="sect-label">Annotations</div>
				{#if annotations.isPending}
					<p class="annot-empty">Loading annotations…</p>
				{:else if (annotations.data ?? []).length === 0}
					<p class="annot-empty">No annotations link this word yet.</p>
				{:else}
					<ul class="annotation-list">
						{#each annotations.data ?? [] as annotation (annotation.id)}
							<li class="annotation-list-item">
								<div class="row-sm">
									<AnnotationBadge type={annotation.type} />
									<span class="anchor-ar">{annotation.anchor}</span>
									<a class="annotation-link" href="/texts/{annotation.textId}">View text →</a>
								</div>
								{#if annotation.content}
									<p class="annotation-content">{annotation.content}</p>
								{/if}
							</li>
						{/each}
					</ul>
				{/if}
			</div>

			<aside class="detail-sidebar">
				<!-- Root card -->
				{#if word.data.rootId}
					<div class="meta-card">
						<div class="meta-card-title">Root</div>
						{#if root.data}
							<a href="/roots/{word.data.rootId}" class="root-link-card">
								<span class="root-link-ar">{root.data.normalizedForm}</span>
								<span class="root-link-cta">View family →</span>
							</a>
						{:else}
							<a href="/roots/{word.data.rootId}" class="root-link-card">
								<span class="root-link-cta">View root family →</span>
							</a>
						{/if}
					</div>
				{/if}

				<!-- Mastery gauge -->
				{#if true}
				{@const stepsOn = masterySteps[word.data.masteryLevel] ?? 0}
				<div class="meta-card">
					<div class="meta-card-title">Mastery</div>
					<div class="mastery-steps">
						{#each Array.from({length: 5}, (_, i) => i) as i}
							<div class="mastery-step" class:on={i < stepsOn}></div>
						{/each}
					</div>
					<span class="mastery-label">{formatEnum(word.data.masteryLevel)} — {stepsOn} of 5</span>
				</div>
				{/if}

				<!-- Details card -->
				<div class="meta-card">
					<div class="meta-card-title">Details</div>
					<div class="meta-row">
						<span class="meta-key">Part of speech</span>
						<span class="meta-val">{formatEnum(word.data.partOfSpeech)}</span>
					</div>
					{#if word.data.transliteration}
					<div class="meta-row">
						<span class="meta-key">Transliteration</span>
						<span class="meta-val">{word.data.transliteration}</span>
					</div>
					{/if}
					<div class="meta-row">
						<span class="meta-key">Dialect</span>
						<span class="meta-val">{word.data.dialect}</span>
					</div>
					<div class="meta-row">
						<span class="meta-key">Difficulty</span>
						<span class="meta-val">{formatEnum(word.data.difficulty)}</span>
					</div>
				</div>
			</aside>
		</div>

		<!-- AI Enrich drawer -->
		<WordEnrichDrawer
			wordId={id}
			open={enrichOpen}
			onClose={() => (enrichOpen = false)}
			onAiUnavailable={() => { aiUnavailable = true; enrichOpen = false; }}
		/>
	{/if}
{/if}
