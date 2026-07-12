<script lang="ts">
import { goto } from '$app/navigation';
import { page } from '$app/state';
import type {
	AlignmentTokenResponse,
	SentenceResponse,
	UpdateTextRequest,
} from '$lib/api/types.gen';
import AnnotationDrawer from '$lib/components/annotations/AnnotationDrawer.svelte';
import FullTextPanel from '$lib/components/texts/FullTextPanel.svelte';
import InterlinearSentence from '$lib/components/texts/InterlinearSentence.svelte';
import SentenceEditor from '$lib/components/texts/SentenceEditor.svelte';
import TextForm from '$lib/components/texts/TextForm.svelte';
import VocabLookupDrawer from '$lib/components/texts/VocabLookupDrawer.svelte';
import { useTextAnnotations } from '$lib/stores/annotations';
import {
	useAutoTokenize,
	useDeleteText,
	useMarkTokensValid,
	useSentences,
	useText,
	useUpdateText,
} from '$lib/stores/texts';

const id = $derived(page.params.id ?? '');
const text = useText(() => id);
const sentences = useSentences(() => id);
const annotations = useTextAnnotations(() => id);

let drawerOpen = $state(false);
let drawerAnchor = $state('');

let vocabOpen = $state(false);
let vocabToken = $state<AlignmentTokenResponse | null>(null);

function openVocabLookup(token: AlignmentTokenResponse) {
	drawerOpen = false;
	vocabToken = token;
	vocabOpen = true;
}

function openAnnotationDrawer(anchor: string) {
	vocabOpen = false;
	drawerAnchor = anchor;
	drawerOpen = true;
}
const updateText = useUpdateText();
const deleteText = useDeleteText();
const autoTokenize = useAutoTokenize();
const markValid = useMarkTokensValid();

let editingSentences = $state(false);
let editingSentenceId = $state<string | null>(null);
let editingInfo = $state(false);
let deleteConfirm = $state(false);
let bulkTokenizing = $state(false);
let bulkTokenizeIndex = $state(0);
let bulkTokenizeTotal = $state(0);
let bulkTokenizeError = $state<string | null>(null);

async function handleUpdate(req: UpdateTextRequest) {
	await updateText.mutateAsync({ id, body: req });
	editingInfo = false;
}

async function handleDelete() {
	if (!deleteConfirm) {
		deleteConfirm = true;
		setTimeout(() => (deleteConfirm = false), 3000);
		return;
	}
	await deleteText.mutateAsync(id);
	goto('/texts');
}

async function handleAutoTokenizeAll() {
	const loadedSentences = sentences.data ?? [];
	if (loadedSentences.length === 0) return;

	bulkTokenizing = true;
	bulkTokenizeError = null;
	bulkTokenizeTotal = loadedSentences.length;
	bulkTokenizeIndex = 0;

	try {
		for (const sentence of loadedSentences) {
			bulkTokenizeIndex += 1;
			await autoTokenize.mutateAsync({ textId: id, id: sentence.id });
		}
	} catch (err) {
		bulkTokenizeError = err instanceof Error ? err.message : 'Auto-tokenize failed';
	} finally {
		bulkTokenizing = false;
	}
}

function startSentenceEdit(sentence: SentenceResponse) {
	editingSentences = false;
	editingInfo = false;
	editingSentenceId = sentence.id;
}

function formatEnum(value: string): string {
	return value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();
}
</script>

{#if text.isPending}
	<p class="status-text status-text-muted">Loading…</p>
{:else if text.isError}
	<p class="status-text status-text-danger">Text not found.</p>
{:else if text.data}
	<!-- Breadcrumb -->
	<nav class="breadcrumb">
		<a href="/texts">Texts</a>
		<span class="bc-sep">/</span>
		{text.data.title}
	</nav>

	<div class="detail-layout detail-layout-text">
		<div class="detail-content">
			<!-- Text header -->
			<div class="text-header">
				<div class="text-title-row">
					<h1 class="text-title">{text.data.title}</h1>
					<div class="text-actions">
						<button
							class="btn"
							onclick={() => { editingInfo = !editingInfo; editingSentences = false; editingSentenceId = null; }}
						>Edit</button>
						<button
							class="btn"
							disabled={bulkTokenizing || autoTokenize.isPending || (sentences.data ?? []).length === 0}
							onclick={handleAutoTokenizeAll}
						>
							{#if bulkTokenizing}
								Tokenizing {bulkTokenizeIndex}/{bulkTokenizeTotal}
							{:else}
								Auto-tokenize all
							{/if}
						</button>
						<a
							class="btn"
							href="/api/v1/texts/{id}/print"
							target="_blank"
							rel="noopener"
						>Print</a>
						<button
							class="btn btn-danger"
							onclick={handleDelete}
							disabled={deleteText.isPending}
						>
							{deleteConfirm ? 'Sure?' : 'Delete'}
						</button>
					</div>
				</div>
				<div class="text-chips">
					<span class="chip c-coral">{formatEnum(text.data.difficulty)}</span>
					<span class="chip c-cerulean">{text.data.dialect}</span>
					{#each text.data.tags as tag}
						<span class="chip c-muted">{tag}</span>
					{/each}
				</div>
				{#if text.data.comments}
					<p class="text-desc">{text.data.comments}</p>
				{/if}
				{#if bulkTokenizeError}
					<p class="form-error">{bulkTokenizeError}</p>
				{/if}
			</div>

			<!-- Info edit panel (collapsible) -->
			{#if editingInfo}
				<div class="surface-card-muted surface-card-pad-md section-block">
					<div class="row-between section-block">
						<span class="helper-copy-strong">Text info</span>
						<button onclick={() => (editingInfo = false)} class="plain-icon-btn">×</button>
					</div>
					<TextForm
						isEdit
						isPending={updateText.isPending}
						initial={{
							title: text.data.title,
							body: text.data.body,
							transliteration: text.data.transliteration ?? null,
							translation: text.data.translation ?? null,
							dialect: text.data.dialect,
							difficulty: text.data.difficulty,
							comments: text.data.comments ?? null,
							tags: text.data.tags,
						}}
						onSubmit={(req) => handleUpdate(req as UpdateTextRequest)}
						onCancel={() => (editingInfo = false)}
					/>
				</div>
			{/if}

			<!-- Interlinear section -->
			<div class="sect-label">Interlinear analysis</div>
			{#if sentences.isPending}
				<p class="status-text status-text-muted">Loading…</p>
			{:else if sentences.isError}
				<p class="status-text status-text-danger">Could not load sentences.</p>
			{:else if editingSentences}
				<SentenceEditor sentences={sentences.data ?? []} textId={id} onDone={() => (editingSentences = false)} />
			{:else if (sentences.data ?? []).length === 0}
				<div class="empty-state-inline">
					<p>No sentences yet.</p>
					<button class="btn" onclick={() => (editingSentences = true)}>Add sentences</button>
				</div>
			{:else}
				{#each sentences.data ?? [] as sentence (sentence.id)}
					{#if editingSentenceId === sentence.id}
						<SentenceEditor
							sentences={[sentence]}
							textId={id}
							hideAdd
							hideOrder
							onDone={() => (editingSentenceId = null)}
						/>
					{:else}
						<InterlinearSentence
							{sentence}
							annotations={annotations.data ?? []}
							onTokenClick={openVocabLookup}
							onEdit={startSentenceEdit}
							isPending={autoTokenize.isPending || markValid.isPending || bulkTokenizing}
							onRetokenize={async (s) => { await autoTokenize.mutateAsync({ textId: id, id: s.id }); }}
							onMarkValid={async (s) => { await markValid.mutateAsync({ textId: id, id: s.id, currentTokens: s.tokens }); }}
						/>
					{/if}
				{/each}
				<div class="sentence-list-footer">
					<button class="btn btn-sm" onclick={() => (editingSentences = true)}>+ Add sentence</button>
				</div>
			{/if}

			<!-- Full text panel -->
			<FullTextPanel text={text.data} />
		</div>

		<aside class="detail-sidebar">
			<!-- Text details card -->
			<div class="meta-card">
				<div class="meta-card-title">Text details</div>
				<div class="meta-row">
					<span class="meta-key">Dialect</span>
					<span class="meta-val">{text.data.dialect}</span>
				</div>
				<div class="meta-row">
					<span class="meta-key">Difficulty</span>
					<span class="meta-val">{formatEnum(text.data.difficulty)}</span>
				</div>
				{#if sentences.data}
					<div class="meta-row">
						<span class="meta-key">Sentences</span>
						<span class="meta-val">{sentences.data.length}</span>
					</div>
				{/if}
			</div>

			<!-- Tags card -->
			{#if text.data.tags.length > 0}
				<div class="meta-card">
					<div class="meta-card-title">Tags</div>
					<div class="word-card-badges">
						{#each text.data.tags as tag}
							<span class="chip c-muted">{tag}</span>
						{/each}
					</div>
				</div>
			{/if}

			<!-- Actions card -->
			<div class="meta-card">
				<div class="meta-card-title">Actions</div>
				<div class="meta-actions">
					<button class="btn btn-full" onclick={() => (editingSentences = true)}>+ Add sentence</button>
					<a href="/training" class="ext-link">Practice words →</a>
				</div>
			</div>
		</aside>
	</div>
{/if}

<AnnotationDrawer
	open={drawerOpen}
	anchor={drawerAnchor}
	textId={id}
	annotations={annotations.data ?? []}
	onclose={() => (drawerOpen = false)}
/>

<VocabLookupDrawer
	open={vocabOpen}
	token={vocabToken}
	onclose={() => (vocabOpen = false)}
	onannotate={openAnnotationDrawer}
/>
