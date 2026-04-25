<script lang="ts">
  import { page } from '$app/state';
  import { onMount, onDestroy } from 'svelte';
  import FlashCard from '$lib/components/training/FlashCard.svelte';
  import SessionSummary from '$lib/components/training/SessionSummary.svelte';
  import { useSession, useRecordResult, useCompleteSession } from '$lib/stores/training';
  import type { SessionSummaryResponse, TrainingSessionWordResponse } from '$lib/api/types.gen';

  const sessionId = $derived(page.params.id);

  const session  = useSession(() => sessionId);
  const record   = useRecordResult();
  const complete = useCompleteSession();

  let summary      = $state<SessionSummaryResponse | null>(null);
  let currentIndex = $state(0);
  let isPending    = $state(false);

  const words = $derived<TrainingSessionWordResponse[]>(
    (session.data?.words ?? []).filter(w => w.result === null || w.result === undefined)
  );

  const currentWord = $derived(words[currentIndex] ?? null);
  const isFinished  = $derived(currentWord === null && session.data !== undefined);

  async function handleResult(result: 'CORRECT' | 'INCORRECT' | 'SKIPPED') {
    const sid = sessionId;
    if (!currentWord || !sid) return;
    isPending = true;
    try {
      await record.mutateAsync({
        sessionId: sid,
        body: { wordId: currentWord.wordId, result },
      });
      if (currentIndex + 1 >= words.length) {
        summary = await complete.mutateAsync(sid);
      } else {
        currentIndex += 1;
      }
    } finally {
      isPending = false;
    }
  }

  function onWindowResult(e: Event) {
    const detail = (e as CustomEvent<{ result: 'CORRECT' | 'INCORRECT' | 'SKIPPED'; wordId: string }>).detail;
    handleResult(detail.result);
  }

  onMount(() => {
    window.addEventListener('result', onWindowResult);
  });

  onDestroy(() => {
    window.removeEventListener('result', onWindowResult);
  });
</script>

{#if session.isLoading}
  <p>Loading…</p>
{:else if session.isError}
  <p>Error loading session.</p>
{:else if summary}
  <SessionSummary {summary} />
{:else if currentWord}
  <div class="session-header">
    <span class="progress">
      {currentIndex + 1} / {session.data?.words.length ?? '?'}
    </span>
    <span class="mode">{session.data?.mode}</span>
  </div>
  <FlashCard
    word={currentWord}
    isPending={isPending || record.isPending || complete.isPending}
  />
{:else if isFinished}
  <p>All words answered. Completing session…</p>
{/if}

<style>
  .session-header {
    display: flex;
    justify-content: space-between;
    padding: 1rem 2rem;
    font-size: 0.875rem;
    color: hsl(var(--muted-foreground));
    max-width: 32rem;
    margin: 0 auto;
  }
</style>
