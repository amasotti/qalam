<script lang="ts">
import { ChevronLeft, ExternalLink, Pencil, Trash2 } from 'lucide-svelte';
import { goto } from '$app/navigation';
import { page } from '$app/state';
import type { UpdateWordRequest } from '$lib/api/types.gen';
import AiExamples from '$lib/components/words/AiExamples.svelte';
import DictionaryLinks from '$lib/components/words/DictionaryLinks.svelte';
import WordForm from '$lib/components/words/WordForm.svelte';
import { Badge } from '$lib/components/ui/badge';
import { Button } from '$lib/components/ui/button';
import { useDeleteWord, useUpdateWord, useWord, useWordAnnotations } from '$lib/stores/words';

const id = $derived(page.params.id ?? '');
const word = useWord(() => id);
const annotations = useWordAnnotations(() => id);
const updateWord = useUpdateWord();
const deleteWord = useDeleteWord();

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

<div class="page-word-detail page-enter">
	<a class="word-detail-back" href="/words">
		<ChevronLeft size={14} />
		Words
	</a>

	{#if word.isPending}
		<p style="color: hsl(var(--muted-foreground)); font-size: 0.875rem;">Loading…</p>
	{:else if word.isError}
		<p style="color: hsl(var(--destructive)); font-size: 0.875rem;">Word not found.</p>
	{:else if word.data}
		{#if isEditing}
			<!-- ── Edit mode ── -->
			<div class="word-hero">
				<div>
					<div class="word-hero-arabic">{word.data.arabicText}</div>
					{#if word.data.transliteration}
						<div class="word-hero-transliteration">{word.data.transliteration}</div>
					{/if}
				</div>
			</div>

			<WordForm
				isEdit
				selfId={id}
				initial={{
					arabicText: word.data.arabicText,
					transliteration: word.data.transliteration ?? null,
					translation: word.data.translation ?? null,
					exampleSentence: word.data.exampleSentence ?? null,
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
		{:else}
			<!-- ── View mode ── -->
			<div class="word-hero">
				<div>
					<div class="word-hero-arabic">{word.data.arabicText}</div>
					{#if word.data.transliteration}
						<div class="word-hero-transliteration">{word.data.transliteration}</div>
					{/if}
					<div class="word-hero-chips">
						<span class="word-hero-chip">{formatEnum(word.data.partOfSpeech)}</span>
						<span class="word-hero-chip">{word.data.dialect}</span>
						<span class="word-hero-chip">{formatEnum(word.data.difficulty)}</span>
						<Badge class="mastery-{word.data.masteryLevel.toLowerCase()}" variant="outline">
							{formatEnum(word.data.masteryLevel)}
						</Badge>
					</div>
				</div>
				<div class="word-hero-actions">
					<Button
						variant="outline"
						size="sm"
						onclick={() => {
							isEditing = true;
							deleteConfirm = false;
						}}
					>
						<Pencil size={13} />
						Edit
					</Button>
					<Button
						variant={deleteConfirm ? 'destructive' : 'ghost'}
						size="sm"
						onclick={handleDelete}
						disabled={deleteWord.isPending}
					>
						<Trash2 size={13} />
						{deleteConfirm ? 'Confirm delete' : 'Delete'}
					</Button>
				</div>
			</div>

			<!-- ── Info sections ── -->
			<div class="word-info">
				<div class="word-info-section">
					<span class="word-info-label">Translation</span>
					{#if word.data.translation}
						<p class="word-info-value">{word.data.translation}</p>
					{:else}
						<p class="word-info-value word-info-empty">No translation recorded</p>
					{/if}
				</div>

				<div class="word-info-section">
					<span class="word-info-label">Example sentence</span>
					{#if word.data.exampleSentence}
						<p class="word-info-value arabic">{word.data.exampleSentence}</p>
					{:else}
						<p class="word-info-value word-info-empty">No example recorded</p>
					{/if}
				</div>

				{#if word.data.pronunciationUrl}
					<div class="word-info-section">
						<span class="word-info-label">Pronunciation</span>
						<a
							class="word-info-link"
							href={word.data.pronunciationUrl}
							target="_blank"
							rel="noopener noreferrer"
						>
							<ExternalLink size={13} />
							{word.data.pronunciationUrl}
						</a>
					</div>
				{/if}
			</div>

			<!-- ── Related ── -->
			{#if word.data.rootId || word.data.derivedFromId}
				<div class="word-related">
					{#if word.data.rootId}
						<div class="word-related-item">
							<span class="word-info-label">Root</span>
							<a href="/roots/{word.data.rootId}" class="word-info-link">
								View root →
							</a>
						</div>
					{/if}
					{#if word.data.derivedFromId}
						<div class="word-related-item">
							<span class="word-info-label">Derived from</span>
							<a href="/words/{word.data.derivedFromId}" class="word-info-link">
								View source word →
							</a>
						</div>
					{/if}
				</div>
			{/if}

			<!-- ── Dictionary links ── -->
			<DictionaryLinks wordId={id} />

			<!-- ── AI examples ── -->
			<AiExamples wordId={id} />

			<!-- ── Annotations ── -->
			<div class="word-annotations">
				<h2 class="word-annotations-title">Annotations</h2>

				{#if annotations.isPending}
					<p style="color: hsl(var(--muted-foreground)); font-size: 0.875rem;">
						Loading annotations…
					</p>
				{:else if annotations.isError}
					<p style="color: hsl(var(--destructive)); font-size: 0.875rem;">
						Could not load annotations.
					</p>
				{:else if (annotations.data ?? []).length === 0}
					<p class="word-info-empty">No annotations link this word yet.</p>
				{:else}
					<ul class="word-annotations-list">
						{#each annotations.data ?? [] as annotation (annotation.id)}
							<li class="word-annotation-item">
								<span class="word-annotation-anchor">{annotation.anchor}</span>
								<span class="word-annotation-type">{annotation.type}</span>
								<a
									class="word-annotation-link"
									href="/texts/{annotation.textId}"
								>
									View text →
								</a>
							</li>
						{/each}
					</ul>
				{/if}
			</div>
		{/if}
	{/if}
</div>
