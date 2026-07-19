<script lang="ts">
import { Button } from '$lib/components/ui/button';
import { useListExerciseSessions } from '$lib/stores/exercises';
import { formatAccuracy, formatTrainingDate, formatTrainingMode } from '$lib/training/presentation';

const pageSizes = [5, 10, 15, 25, 50];
let page = $state(1);
let size = $state(10);
const sessions = useListExerciseSessions(
	() => page,
	() => size
);
const pageCount = $derived(Math.max(1, Math.ceil((sessions.data?.total ?? 0) / size)));

function setSize(nextSize: number) {
	size = nextSize;
	page = 1;
}
</script>

<section class="training-history" aria-labelledby="exercise-history-heading">
	<div class="training-history-header"><div><h2 id="exercise-history-heading">Exercise history</h2><p>Resume an active exercise or revisit its outcome.</p></div><label class="training-history-size">Show <select value={size} onchange={(event) => setSize(Number(event.currentTarget.value))}>{#each pageSizes as pageSize}<option value={pageSize}>{pageSize}</option>{/each}</select></label></div>
	{#if sessions.isPending}<p class="training-muted">Loading exercises…</p>
	{:else if sessions.isError}<p class="form-error-msg">Could not load exercise history.</p>
	{:else if (sessions.data?.items ?? []).length === 0}<p class="training-history-empty">Your active and completed exercises will appear here.</p>
	{:else}
		<ul class="training-session-list">{#each sessions.data?.items ?? [] as session (session.id)}<li class="training-session-row"><div class="training-session-date"><span>{session.status === 'ACTIVE' ? 'In progress' : formatTrainingDate(session.completedAt ?? session.createdAt)}</span><small>{formatTrainingMode(session.mode)} · {session.totalItems} questions</small></div>{#if session.status === 'COMPLETED'}<div class="training-session-result"><strong>{formatAccuracy(session.accuracy)}</strong><span>{session.correctCount} correct · {session.incorrectCount} wrong</span></div>{:else}<span class="training-session-active">Active</span>{/if}<Button href={`/training/exercises/${session.id}`} variant={session.status === 'ACTIVE' ? 'default' : 'outline'} size="sm">{session.status === 'ACTIVE' ? 'Resume' : 'Review'}</Button></li>{/each}</ul>
		<div class="training-pagination"><Button variant="outline" size="sm" disabled={page <= 1} onclick={() => (page -= 1)}>Previous</Button><span>Page {page} of {pageCount}</span><Button variant="outline" size="sm" disabled={page >= pageCount} onclick={() => (page += 1)}>Next</Button></div>
	{/if}
</section>
