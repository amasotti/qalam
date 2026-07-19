<script lang="ts">
import { page } from '$app/state';
import type {
	AnswerExerciseItemResponse,
	ExerciseSessionSummaryResponse,
} from '$lib/api/types.gen';
import ExerciseSessionSummary from '$lib/components/training/ExerciseSessionSummary.svelte';
import { Button } from '$lib/components/ui/button';
import { summaryFromCompletedExercise } from '$lib/exercises/presentation';
import {
	useAnswerExerciseItem,
	useCompleteExerciseSession,
	useExerciseSession,
} from '$lib/stores/exercises';

const sessionId = $derived(page.params.id ?? '');
const session = useExerciseSession(() => sessionId);
const answerItem = useAnswerExerciseItem();
const completeSession = useCompleteExerciseSession();

let currentIndex = $state(0);
let selectedOptionId = $state<string | null>(null);
let feedback = $state<AnswerExerciseItemResponse | null>(null);
let summary = $state<ExerciseSessionSummaryResponse | null>(null);
let initializedSessionId = $state<string | null>(null);

const currentItem = $derived(session.data?.items[currentIndex] ?? null);
const totalItems = $derived(session.data?.items.length ?? 0);
const progress = $derived(totalItems === 0 ? 0 : ((currentIndex + 1) / totalItems) * 100);

$effect(() => {
	if (!session.data || initializedSessionId === session.data.id) return;

	initializedSessionId = session.data.id;
	currentIndex = session.data.items.findIndex((item) => item.result == null);
	selectedOptionId = null;
	feedback = null;
	summary = session.data.status === 'COMPLETED' ? summaryFromCompletedExercise(session.data) : null;
});

async function selectOption(optionId: string) {
	if (!currentItem || feedback || selectedOptionId || answerItem.isPending) return;

	selectedOptionId = optionId;
	try {
		feedback = await answerItem.mutateAsync({
			sessionId,
			body: { itemId: currentItem.itemId, selectedOptionId: optionId },
		});
	} catch {
		selectedOptionId = null;
	}
}

async function next() {
	if (!session.data || !feedback) return;

	if (currentIndex < totalItems - 1) {
		currentIndex += 1;
		selectedOptionId = null;
		feedback = null;
		return;
	}

	summary = await completeSession.mutateAsync(sessionId);
	await session.refetch();
}

async function finishEarly() {
	summary = await completeSession.mutateAsync(sessionId);
	await session.refetch();
}
</script>

{#if session.isPending}
	<div class="exercise-state"><p>Loading exercise…</p></div>
{:else if session.isError}
	<div class="exercise-state"><h1>Exercise unavailable</h1><p>{session.error.message}</p><Button href="/training/exercises/multiple-choice" variant="outline">Back to exercises</Button></div>
{:else if summary}
	<ExerciseSessionSummary {summary} items={session.data?.items ?? []} />
{:else if currentItem}
	<main class="exercise-runner">
		<header class="exercise-progress-header">
			<a href="/training/exercises/multiple-choice">Exit exercise</a>
			<span>Question {currentIndex + 1} of {totalItems}</span>
		</header>
		<div class="exercise-progress-track" aria-hidden="true"><i style:width={`${progress}%`}></i></div>

		<section class="exercise-card" aria-labelledby="exercise-prompt">
			<p class="exercise-direction">{currentItem.prompt.kind === 'ARABIC_WORD' ? 'Choose the meaning' : 'Choose the Arabic word'}</p>
			<h1 id="exercise-prompt" class:arabic={currentItem.prompt.kind === 'ARABIC_WORD'} class:exercise-prompt-arabic={currentItem.prompt.kind === 'ARABIC_WORD'} class="exercise-prompt" lang={currentItem.prompt.kind === 'ARABIC_WORD' ? 'ar' : undefined}>{currentItem.prompt.text}</h1>

			<div class="exercise-options" role="group" aria-label="Answer options">
				{#each currentItem.options as option (option.optionId)}
					<button
						type="button"
						class="exercise-option"
						class:correct={feedback?.correctOptionId === option.optionId}
						class:incorrect={feedback !== null && selectedOptionId === option.optionId && feedback.correctOptionId !== option.optionId}
						disabled={feedback !== null || answerItem.isPending}
						onclick={() => selectOption(option.optionId)}
					>
						{#if currentItem.prompt.kind === 'ARABIC_WORD'}
							<span>{option.translation ?? 'No translation'}</span>
						{:else}
							<span class="exercise-option-arabic arabic" lang="ar">{option.arabicText}</span>
							{#if option.transliteration}<small>{option.transliteration}</small>{/if}
						{/if}
					</button>
				{/each}
			</div>

			{#if feedback}
				<p class:correct={feedback.result === 'CORRECT'} class:incorrect={feedback.result === 'INCORRECT'} class="exercise-feedback">
					{feedback.result === 'CORRECT' ? 'Correct.' : 'Not this one — the highlighted answer is correct.'}
				</p>
				<div class="exercise-runner-actions"><Button onclick={next} disabled={completeSession.isPending}>{currentIndex === totalItems - 1 ? 'Finish exercise' : 'Next question'}</Button></div>
			{:else if answerItem.error}
				<p class="form-error-msg">{answerItem.error.message}</p>
			{/if}
		</section>
	</main>
{:else}
	<div class="exercise-state"><h1>No questions remain</h1><p>You can finish this exercise now; unanswered questions will be recorded as skipped.</p><Button onclick={finishEarly} disabled={completeSession.isPending}>Finish exercise</Button></div>
{/if}
