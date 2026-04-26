<script lang="ts">
import { page } from '$app/state';
import type { SessionSummaryResponse, TrainingSessionWordResponse } from '$lib/api/types.gen';
import FlashCard from '$lib/components/training/FlashCard.svelte';
import SessionSummary from '$lib/components/training/SessionSummary.svelte';
import { useCompleteSession, useRecordResult, useSession } from '$lib/stores/training';

const sessionId = $derived(page.params.id);

const session = useSession(() => sessionId);
const record = useRecordResult();
const complete = useCompleteSession();

let summary = $state<SessionSummaryResponse | null>(null);
let currentIndex = $state(0);
let isPending = $state(false);

// Snapshot unanswered words once on load — never re-derive from server state mid-session.
// Re-deriving causes the array to shrink on each background refetch, which breaks currentIndex.
let localWords = $state<TrainingSessionWordResponse[]>([]);

$effect(() => {
	if (session.data && localWords.length === 0) {
		localWords = (session.data.words ?? []).filter(
			(w) => w.result === null || w.result === undefined
		);
	}
});

const currentWord = $derived(localWords[currentIndex] ?? null);
const isFinished = $derived(currentWord === null && localWords.length > 0);

async function handleResult(result: 'CORRECT' | 'INCORRECT' | 'SKIPPED') {
	const sid = sessionId;
	if (!currentWord || !sid) return;
	isPending = true;
	try {
		await record.mutateAsync({
			sessionId: sid,
			body: { wordId: currentWord.wordId, result },
		});
		if (currentIndex + 1 >= localWords.length) {
			summary = await complete.mutateAsync(sid);
		} else {
			currentIndex += 1;
		}
	} finally {
		isPending = false;
	}
}
</script>

{#if session.isLoading}
  <p>Loading…</p>
{:else if session.isError}
  <p>Error loading session.</p>
{:else if summary}
  <SessionSummary {summary} />
{:else if currentWord}
  <FlashCard
    word={currentWord}
    isPending={isPending || record.isPending || complete.isPending}
    currentIndex={currentIndex}
    totalWords={localWords.length}
    mode={session.data?.mode ?? ''}
    onresult={handleResult}
  />
{:else if isFinished}
  <p>All words answered. Completing session…</p>
{/if}

