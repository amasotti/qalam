<script lang="ts">
import type {
	AnswerConjugationExerciseItemResponse,
	ConjugationExerciseItemResponse,
} from '$lib/api/types.gen';
import { Button } from '$lib/components/ui/button';

interface Props {
	item: ConjugationExerciseItemResponse;
	answer: AnswerConjugationExerciseItemResponse | null;
	isSubmitting: boolean;
	onsubmit: (submittedText: string) => void;
}

const diacritics = [
	{ mark: 'َ', name: 'Fatha' },
	{ mark: 'ُ', name: 'Damma' },
	{ mark: 'ِ', name: 'Kasra' },
	{ mark: 'ْ', name: 'Sukun' },
	{ mark: 'ّ', name: 'Shadda' },
	{ mark: 'ً', name: 'Fathatan' },
	{ mark: 'ٌ', name: 'Dammatan' },
	{ mark: 'ٍ', name: 'Kasratan' },
];

let { item, answer, isSubmitting, onsubmit }: Props = $props();
let input = $state<HTMLInputElement>();
let submittedText = $state('');
let initializedItemId = $state<string | null>(null);
const isAnswered = $derived(item.result !== undefined);
const prompt = $derived(item.labels[0]?.label ?? 'Selected form');

$effect(() => {
	if (initializedItemId === item.itemId) return;
	initializedItemId = item.itemId;
	submittedText = '';
});

function insertDiacritic(mark: string) {
	if (!input || isAnswered || isSubmitting) return;
	const start = input.selectionStart ?? submittedText.length;
	const end = input.selectionEnd ?? start;
	submittedText = `${submittedText.slice(0, start)}${mark}${submittedText.slice(end)}`;
	requestAnimationFrame(() => {
		input?.focus();
		input?.setSelectionRange(start + mark.length, start + mark.length);
	});
}

function submit() {
	if (!submittedText.trim() || isAnswered) return;
	onsubmit(submittedText);
}
</script>

<section class="conj-exercise-board" aria-labelledby="conj-exercise-lemma">
	<header class="conj-exercise-board-header">
		<p>Write the fully vocalised form</p>
		<h1 id="conj-exercise-lemma" class="arabic" lang="ar" dir="rtl">{item.lemma}</h1>
		<span>{item.translation ?? 'No translation'} · Form {item.verbForm} · {item.tense.toLowerCase()} active</span>
	</header>

	<div class="conj-write-prompt"><span>Write:</span><strong>{prompt}</strong></div>
	<label class="conj-write-answer" for="conj-write-answer">Your answer</label>
	<input bind:this={input} id="conj-write-answer" class="conj-write-input arabic" lang="ar" dir="rtl" autocomplete="off" autocapitalize="off" spellcheck={false} disabled={isAnswered || isSubmitting} bind:value={submittedText} onkeydown={(event) => { if (event.key === 'Enter') submit(); }} />
	<div class="conj-diacritic-toolbar" role="toolbar" aria-label="Arabic diacritics">
		{#each diacritics as diacritic}
			<button type="button" aria-label={diacritic.name} disabled={isAnswered || isSubmitting} onclick={() => insertDiacritic(diacritic.mark)}>{diacritic.mark}</button>
		{/each}
	</div>

	{#if isAnswered}
		<div class:correct={item.result === 'CORRECT'} class:incorrect={item.result === 'INCORRECT'} class="conj-write-feedback">
			<p>{item.result === 'CORRECT' ? 'Correct.' : 'Not quite.'}</p>
			{#if answer}
				<dl><div><dt>Your answer</dt><dd class="arabic" lang="ar" dir="rtl">{answer.submittedText ?? '—'}</dd></div><div><dt>Expected form</dt><dd class="arabic" lang="ar" dir="rtl">{answer.expectedArabic ?? '—'}</dd></div></dl>
			{/if}
		</div>
	{:else}
		<div class="conj-exercise-actions"><Button onclick={submit} disabled={!submittedText.trim() || isSubmitting}>{isSubmitting ? 'Checking…' : 'Check answer'}</Button></div>
	{/if}
</section>
