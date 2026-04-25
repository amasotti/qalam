<script lang="ts">
import { goto } from '$app/navigation';
import { page } from '$app/state';
import type { AlignmentTokenResponse, UpdateTextRequest } from '$lib/api/types.gen';
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
let editingInfo = $state(false);
let deleteConfirm = $state(false);

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

function formatEnum(value: string): string {
	return value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();
}
</script>

{#if text.isPending}
	<p style="color:var(--ink-ghost);">Loading…</p>
{:else if text.isError}
	<p style="color:var(--coral);">Text not found.</p>
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
							class="btn btn-primary"
							onclick={() => { editingSentences = !editingSentences; editingInfo = false; }}
						>
							{editingSentences ? 'Done' : 'Edit'}
						</button>
						<button
							class="btn"
							onclick={() => { editingInfo = !editingInfo; editingSentences = false; }}
						>Settings</button>
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
			</div>

			<!-- Info edit panel (collapsible) -->
			{#if editingInfo}
				<div style="border:1px solid var(--border);border-radius:8px;padding:1rem;margin-bottom:1.5rem;background:var(--bg-dark);">
					<div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:1rem;">
						<span style="font-size:0.8rem;font-weight:600;text-transform:uppercase;letter-spacing:0.05em;color:var(--ink-ghost);">Text info</span>
						<button onclick={() => (editingInfo = false)} style="background:none;border:none;cursor:pointer;color:var(--ink-ghost);">×</button>
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
				<p style="color:var(--ink-ghost);">Loading…</p>
			{:else if sentences.isError}
				<p style="color:var(--coral);">Could not load sentences.</p>
			{:else if editingSentences}
				<SentenceEditor sentences={sentences.data ?? []} textId={id} />
			{:else if (sentences.data ?? []).length === 0}
				<div style="display:flex;flex-direction:column;align-items:center;gap:0.75rem;padding:3rem 0;color:var(--ink-ghost);">
					<p>No sentences yet.</p>
					<button class="btn" onclick={() => (editingSentences = true)}>Add sentences</button>
				</div>
			{:else}
				{#each sentences.data ?? [] as sentence (sentence.id)}
					<InterlinearSentence
						{sentence}
						annotations={annotations.data ?? []}
						onTokenClick={openVocabLookup}
						isPending={autoTokenize.isPending || markValid.isPending}
						onRetokenize={async (s) => { await autoTokenize.mutateAsync({ textId: id, id: s.id }); }}
						onMarkValid={async (s) => { await markValid.mutateAsync({ textId: id, id: s.id, currentTokens: s.tokens }); }}
					/>
				{/each}
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
					<div style="display:flex;flex-wrap:wrap;gap:0.375rem;">
						{#each text.data.tags as tag}
							<span class="chip c-muted">{tag}</span>
						{/each}
					</div>
				</div>
			{/if}

			<!-- Actions card -->
			<div class="meta-card">
				<div class="meta-card-title">Actions</div>
				<div style="display:flex;flex-direction:column;gap:0.5rem;">
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
