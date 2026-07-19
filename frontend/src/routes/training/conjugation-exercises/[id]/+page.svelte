<script lang="ts">
import { page } from '$app/state';
import type {
	AnswerConjugationExerciseItemResponse,
	ConjugationExerciseMappingResponse,
	ConjugationExerciseSessionSummaryResponse,
} from '$lib/api/types.gen';
import ConjugationMatchBoard from '$lib/components/conjugation/ConjugationMatchBoard.svelte';
import ConjugationWriteFormBoard from '$lib/components/conjugation/ConjugationWriteFormBoard.svelte';
import { Button } from '$lib/components/ui/button';
import {
	useAnswerConjugationExerciseItem,
	useCompleteConjugationExerciseSession,
	useConjugationExerciseSession,
} from '$lib/stores/conjugationExercises';

const sessionId = $derived(page.params.id ?? '');
const session = useConjugationExerciseSession(() => sessionId);
const answerItem = useAnswerConjugationExerciseItem();
const completeSession = useCompleteConjugationExerciseSession();
let currentIndex = $state(0);
let summary = $state<ConjugationExerciseSessionSummaryResponse | null>(null);
let initializedId = $state<string | null>(null);
let writeAnswer = $state<AnswerConjugationExerciseItemResponse | null>(null);
const currentItem = $derived(session.data?.items[currentIndex] ?? null);
const totalItems = $derived(session.data?.items.length ?? 0);
const progress = $derived(totalItems === 0 ? 0 : ((currentIndex + 1) / totalItems) * 100);
$effect(() => {
	if (!session.data || initializedId === session.data.id) return;
	initializedId = session.data.id;
	currentIndex = session.data.items.findIndex((item) => item.result === undefined);
	if (currentIndex < 0) currentIndex = 0;
	if (session.data.status === 'COMPLETED')
		summary = {
			sessionId: session.data.id,
			mode: session.data.mode,
			totalItems,
			correct: session.data.items.filter((item) => item.result === 'CORRECT').length,
			incorrect: session.data.items.filter((item) => item.result === 'INCORRECT').length,
			skipped: session.data.items.filter((item) => item.result === 'SKIPPED').length,
			accuracy: 0,
			completedAt: session.data.completedAt ?? session.data.createdAt,
		};
});
async function submit(mappings: ConjugationExerciseMappingResponse[]) {
	if (!currentItem) return;
	await answerItem.mutateAsync({
		sessionId,
		body: { itemId: currentItem.itemId, mappings: mappings as never },
	});
	await session.refetch();
}
async function submitWrittenForm(submittedText: string) {
	if (!currentItem) return;
	writeAnswer = await answerItem.mutateAsync({
		sessionId,
		body: { itemId: currentItem.itemId, mappings: [] as never, submittedText },
	});
	await session.refetch();
}
async function next() {
	if (currentIndex < totalItems - 1) {
		currentIndex += 1;
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
{#if session.isPending}<div class="exercise-state"><p>Loading conjugation exercise…</p></div>
{:else if session.isError}<div class="exercise-state"><h1>Exercise unavailable</h1><p>{session.error.message}</p><Button href="/training/exercises/conjugation" variant="outline">Back to conjugation exercises</Button></div>
{:else if summary}<main class="exercise-state"><h1>Session complete</h1><p>{summary.correct} boards correct · {summary.incorrect} incorrect · {summary.skipped} skipped</p><Button href="/training/exercises/conjugation">Practise again</Button></main>
{:else if currentItem}<main class="exercise-runner"><header class="exercise-progress-header"><a href="/training/exercises/conjugation">Exit exercise</a><span>Board {currentIndex + 1} of {totalItems}</span></header><div class="exercise-progress-track" aria-hidden="true"><i style:width={`${progress}%`}></i></div>{#if currentItem.exerciseType === 'WRITE_FORM'}<ConjugationWriteFormBoard item={currentItem} answer={writeAnswer?.itemId === currentItem.itemId ? writeAnswer : null} isSubmitting={answerItem.isPending} onsubmit={submitWrittenForm} />{:else}<ConjugationMatchBoard item={currentItem} isSubmitting={answerItem.isPending} onsubmit={submit} />{/if}{#if answerItem.error}<p class="form-error-msg">{answerItem.error.message}</p>{/if}{#if currentItem.result !== undefined}<div class="exercise-runner-actions"><Button onclick={next} disabled={completeSession.isPending}>{currentIndex === totalItems - 1 ? 'Finish session' : 'Next board'}</Button></div>{/if}</main>
{:else}<div class="exercise-state"><h1>No boards remain</h1><p>Finish now; unanswered boards are recorded as skipped.</p><Button onclick={finishEarly}>Finish session</Button></div>{/if}
