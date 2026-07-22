<script lang="ts">
import { RefreshCw, Sparkles } from '@lucide/svelte';
import type {
	ProductionPracticePromptResponse,
	ProductionPracticeReviewResponse,
	ReviewProductionPracticeRequest,
} from '$lib/api/types.gen';
import Markdown from '$lib/components/Markdown.svelte';
import { Button } from '$lib/components/ui/button';
import {
	useProductionPracticePrompt,
	useReviewProductionPractice,
} from '$lib/stores/productionPractice';

const prompt = useProductionPracticePrompt();
const review = useReviewProductionPractice();

let sentence = $state('');
let usedWordIds = $state<string[]>([]);
let feedback = $state<ProductionPracticeReviewResponse | null>(null);

const selectedWordCount = $derived(usedWordIds.length);
const canSubmit = $derived(
	sentence.trim().length > 0 && selectedWordCount >= 2 && !review.isPending
);
const reviewError = $derived(review.error as { status?: number; message?: string } | null);
const isAiNotConfigured = $derived(reviewError?.status === 503);
const reviewErrorMessage = $derived(
	reviewError?.message ?? 'Could not review your sentence. Please try again.'
);

function toggleUsedWord(wordId: string) {
	usedWordIds = usedWordIds.includes(wordId)
		? usedWordIds.filter((id) => id !== wordId)
		: [...usedWordIds, wordId];
}

function submitReview() {
	if (!canSubmit || !prompt.data) return;
	feedback = null;
	review.mutate(
		{
			sentence,
			targetWordIds: targetWordIds(prompt.data.words),
			usedWordIds,
		},
		{ onSuccess: (result) => (feedback = result) }
	);
}

function targetWordIds(
	words: ProductionPracticePromptResponse['words']
): ReviewProductionPracticeRequest['targetWordIds'] {
	return [
		words[0].id,
		words[1].id,
		words[2].id,
		words[3].id,
		words[4].id,
		words[5].id,
		words[6].id,
	];
}

function startFreshPrompt() {
	sentence = '';
	usedWordIds = [];
	feedback = null;
	review.reset();
	prompt.refetch();
}
</script>

<div class="training-home production-practice-page">
	<header class="training-home-header">
		<p class="training-home-kicker">Practice · Production</p>
		<h1>Write with your vocabulary</h1>
		<p>Use at least two target words in one Arabic sentence. Feedback is private to this moment and never changes your progress.</p>
	</header>

	{#if prompt.isPending}
		<p class="training-muted">Preparing your words…</p>
	{:else if prompt.isError}
		<section class="production-practice-notice" aria-live="polite">
			<p>Could not prepare a writing prompt.</p>
			<Button variant="outline" onclick={() => prompt.refetch()}><RefreshCw size={16} />Try again</Button>
		</section>
	{:else if prompt.data}
		<section class="production-practice-composer" aria-labelledby="production-practice-words-heading">
			<div class="training-section-heading">
				<div>
					<h2 id="production-practice-words-heading">Your seven words</h2>
					<p>Two nouns and two verbs are included; choose any two or more to use.</p>
				</div>
				<span class="training-card-count">{selectedWordCount} selected</span>
			</div>

			<div class="production-practice-words">
				{#each prompt.data.words as word (word.id)}
					<label class="production-practice-word" class:selected={usedWordIds.includes(word.id)}>
						<input type="checkbox" checked={usedWordIds.includes(word.id)} onchange={() => toggleUsedWord(word.id)} />
						<span class="production-practice-word-arabic" lang="ar" dir="rtl">{word.arabicText}</span>
						<span class="production-practice-word-detail">
							{#if word.transliteration}<small>{word.transliteration}</small>{/if}
							{#if word.translation}<span>{word.translation}</span>{/if}
							<em>{word.partOfSpeech.toLowerCase()}</em>
						</span>
					</label>
				{/each}
			</div>

			<label class="production-practice-sentence">
				<span class="training-field-label">Your sentence</span>
				<textarea class="form-textarea" bind:value={sentence} lang="ar" dir="rtl" rows="4" maxlength="1000" placeholder="اكتب جملة هنا…"></textarea>
				<span class="production-practice-hint">{selectedWordCount < 2 ? `Select ${2 - selectedWordCount} more word${selectedWordCount === 0 ? 's' : ''} before reviewing.` : 'Ready to review.'}</span>
			</label>

			<div class="production-practice-actions">
				<Button size="lg" onclick={submitReview} disabled={!canSubmit}>
					<Sparkles size={17} />{review.isPending ? 'Reviewing…' : 'Review my sentence'}
				</Button>
				<Button variant="outline" onclick={startFreshPrompt} disabled={prompt.isFetching || review.isPending}>
					<RefreshCw size={16} />New words
				</Button>
			</div>
		</section>

		{#if isAiNotConfigured}
			<section class="production-practice-notice" aria-live="polite"><p>AI not configured — set <code>OPENROUTER_API_KEY</code> to review sentences.</p></section>
		{:else if review.isError}
			<section class="production-practice-notice production-practice-notice-error" aria-live="polite"><p>{reviewErrorMessage}</p></section>
		{:else if feedback}
			<section class="production-practice-feedback" aria-live="polite" aria-labelledby="production-practice-feedback-heading">
				<h2 id="production-practice-feedback-heading">Your review</h2>
				<Markdown content={feedback.reviewMarkdown} />
			</section>
		{/if}
	{/if}
</div>
