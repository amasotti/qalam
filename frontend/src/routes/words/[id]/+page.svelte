<script lang="ts">
import { goto } from '$app/navigation';
import { page } from '$app/state';
import type { UpdateWordRequest } from '$lib/api/types.gen';
import AnnotationBadge from '$lib/components/annotations/AnnotationBadge.svelte';
import AiExamples from '$lib/components/words/AiExamples.svelte';
import DictionaryLinks from '$lib/components/words/DictionaryLinks.svelte';
import WordForm from '$lib/components/words/WordForm.svelte';
import { useRoot } from '$lib/stores/roots';
import {
	useDeleteWord,
	useDeleteWordExample,
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
const root = useRoot(() => word.data?.rootId ?? undefined);

let isEditing = $state(false);
let deleteConfirm = $state(false);

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

function formatEnum(value: string): string {
	return value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();
}
</script>

{#if word.isPending}
	<p style="color: var(--ink-ghost); font-size: 0.875rem; padding: 2rem 3rem;">Loading…</p>
{:else if word.isError}
	<p style="color: var(--coral); font-size: 0.875rem; padding: 2rem 3rem;">Word not found.</p>
{:else if word.data}
	{#if isEditing}
		<!-- ── Edit mode ── -->
		<div style="padding: 2rem 3rem;">
			<div class="word-hero" style="margin-bottom: 1.5rem;">
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
							<button
								class="btn btn-danger"
								onclick={handleDelete}
								disabled={deleteWord.isPending}
							>{deleteConfirm ? 'Confirm delete' : 'Delete'}</button>
						</div>
					</div>
				</div>

				<!-- Translation -->
				<div class="sect-label">Translation</div>
				{#if word.data.translation}
					<p class="word-translation">{word.data.translation}</p>
				{:else}
					<p class="annot-empty">No translation recorded</p>
				{/if}

				<!-- Pronunciation -->
				{#if word.data.pronunciationUrl}
					<div style="margin: 0.875rem 0 2.5rem">
						<a
							class="pron-link"
							href={word.data.pronunciationUrl}
							target="_blank"
							rel="noopener noreferrer"
						>♪ Forvo ↗</a>
					</div>
				{/if}

				<!-- Examples -->
				<div class="sect-label">Examples</div>
				<div class="word-examples">
					{#if examples.isPending}
						<p style="color: var(--ink-ghost); font-size: 0.875rem;">Loading…</p>
					{:else if (examples.data ?? []).length === 0}
						<p class="annot-empty">No examples saved yet</p>
					{:else}
						{#each examples.data ?? [] as ex (ex.id)}
							<div class="example-card">
								<p class="example-card-ar">{ex.arabic}</p>
								{#if ex.transliteration}<p class="example-card-tr">{ex.transliteration}</p>{/if}
								{#if ex.translation}<p class="example-card-en">{ex.translation}</p>{/if}
								<button
									style="position:absolute;top:0.375rem;right:0.5rem;background:none;border:none;cursor:pointer;font-size:1rem;color:var(--ink-ghost);"
									onclick={() => deleteExample.mutate({ id, exampleId: ex.id })}
									disabled={deleteExample.isPending}
									aria-label="Delete example"
								>×</button>
							</div>
						{/each}
					{/if}
				</div>

				<!-- Dictionary sources -->
				<div class="sect-label">Dictionary sources</div>
				<DictionaryLinks wordId={id} arabicText={word.data.arabicText} />

				<!-- AI examples -->
				<AiExamples wordId={id} />

				<!-- Annotations -->
				<div class="sect-label">Annotations</div>
				{#if annotations.isPending}
					<p class="annot-empty">Loading annotations…</p>
				{:else if (annotations.data ?? []).length === 0}
					<p class="annot-empty">No annotations link this word yet.</p>
				{:else}
					<ul style="list-style:none;display:flex;flex-direction:column;gap:0.5rem;">
						{#each annotations.data ?? [] as annotation (annotation.id)}
							<li style="display:flex;flex-direction:column;gap:0.25rem;padding:0.5rem 0.75rem;border:1px solid var(--border);border-radius:8px;background:var(--white);">
								<div style="display:flex;align-items:center;gap:0.5rem;">
									<AnnotationBadge type={annotation.type} />
									<span style="font-size:0.875rem;direction:rtl;font-family:'Noto Naskh Arabic',serif;">{annotation.anchor}</span>
									<a style="margin-left:auto;font-size:0.8rem;color:var(--cerulean);text-decoration:none;" href="/texts/{annotation.textId}">View text →</a>
								</div>
								{#if annotation.content}
									<p style="font-size:0.8125rem;color:var(--ink-mid);line-height:1.5;margin:0.125rem 0 0 1.5rem;">{annotation.content}</p>
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
				{@const masterySteps = { NEW: 1, LEARNING: 2, FAMILIAR: 3, KNOWN: 4, MASTERED: 5 }}
				{@const stepsOn = masterySteps[word.data.masteryLevel as keyof typeof masterySteps] ?? 0}
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
	{/if}
{/if}
