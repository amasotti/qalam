<script lang="ts">
import { goto } from '$app/navigation';
import { Button } from '$lib/components/ui/button';
import { useCreateSession, useListSessions, useTrainingStats } from '$lib/stores/training';
import { useAllWordLists } from '$lib/stores/wordLists';

const createSession = useCreateSession();
const stats = useTrainingStats();
const wordLists = useAllWordLists();

const modes = ['MIXED', 'NEW', 'LEARNING', 'KNOWN'] as const;
type Mode = (typeof modes)[number];
const sessionSizes = [10, 15, 25, 50];
const historyPageSizes = [10, 25, 50];

let selectedMode = $state<Mode>('MIXED');
let sessionSize = $state(15);
let selectedWordListIds = $state<string[]>([]);
let historyPage = $state(1);
let historySize = $state(10);

const sessions = useListSessions(
	() => historyPage,
	() => historySize
);
const selectedListCount = $derived(selectedWordListIds.length);
const totalVocabulary = $derived(
	Object.values(stats.data?.masteryDistribution ?? {}).reduce((total, count) => total + count, 0)
);
const historyPageCount = $derived(
	Math.max(1, Math.ceil((sessions.data?.total ?? 0) / historySize))
);

const modeCopy: Record<Mode, { label: string; description: string }> = {
	MIXED: { label: 'Mixed review', description: 'A balanced pass across your vocabulary.' },
	NEW: { label: 'New words', description: 'Meet words you have not practised yet.' },
	LEARNING: { label: 'Learning', description: 'Strengthen words still in progress.' },
	KNOWN: { label: 'Known', description: 'Keep familiar vocabulary active.' },
};

function start() {
	createSession.mutate(
		{ mode: selectedMode, size: sessionSize, wordListIds: selectedWordListIds },
		{ onSuccess: (session) => goto(`/training/${session.id}`) }
	);
}

function toggleWordList(id: string) {
	selectedWordListIds = selectedWordListIds.includes(id)
		? selectedWordListIds.filter((selectedId) => selectedId !== id)
		: [...selectedWordListIds, id];
}

function formatMode(mode: string) {
	return mode.charAt(0) + mode.slice(1).toLowerCase();
}

function formatDate(value: string) {
	return new Intl.DateTimeFormat('en-GB', {
		day: 'numeric',
		month: 'short',
		year: 'numeric',
	}).format(new Date(value));
}

function accuracyLabel(accuracy: number) {
	return `${Math.round(accuracy * 100)}%`;
}
</script>

<div class="training-home">
	<header class="training-home-header">
		<p class="training-home-kicker">Practice · Flashcards</p>
		<h1>Review your vocabulary</h1>
		<p>Choose a focused set, then recall before you reveal.</p>
	</header>

	<div class="training-home-layout">
		<section class="training-composer" aria-labelledby="new-session-heading">
			<div class="training-section-heading">
				<div>
					<h2 id="new-session-heading">New session</h2>
					<p>Set a scope and let the cards do the rest.</p>
				</div>
				<span class="training-card-count">{sessionSize} cards</span>
			</div>

			<div class="training-form-section">
				<div class="training-field-heading">
					<span class="training-field-label">What should you review?</span>
					<span>{selectedListCount === 0 ? 'All vocabulary' : `${selectedListCount} list${selectedListCount === 1 ? '' : 's'} selected`}</span>
				</div>
				{#if wordLists.isPending}
					<p class="training-muted">Loading word lists…</p>
				{:else if wordLists.isError}
					<p class="form-error-msg">Could not load word lists.</p>
				{:else if (wordLists.data ?? []).length > 0}
					<div class="training-scope-options">
						<button type="button" class="training-scope-option" class:selected={selectedListCount === 0} onclick={() => (selectedWordListIds = [])}>
							<span>All vocabulary</span><small>{totalVocabulary} words</small>
						</button>
						{#each wordLists.data ?? [] as list}
							<button type="button" class="training-scope-option" class:selected={selectedWordListIds.includes(list.id)} disabled={list.itemCount === 0} onclick={() => toggleWordList(list.id)}>
								<span>{list.title}</span><small>{list.itemCount} words</small>
							</button>
						{/each}
					</div>
				{:else}
					<p class="training-muted">No word lists yet. This session will use all vocabulary.</p>
				{/if}
			</div>

			<div class="training-form-section">
				<div class="training-field-heading"><span class="training-field-label">Choose a focus</span></div>
				<div class="training-mode-options" role="radiogroup" aria-label="Flashcard focus">
					{#each modes as mode}
						<button type="button" class="training-mode-option" class:selected={selectedMode === mode} role="radio" aria-checked={selectedMode === mode} onclick={() => (selectedMode = mode)}>
							<span>{modeCopy[mode].label}</span><small>{modeCopy[mode].description}</small>
						</button>
					{/each}
				</div>
			</div>

			<div class="training-form-section">
				<div class="training-field-heading"><span class="training-field-label">How many cards?</span></div>
				<div class="training-size-options" role="radiogroup" aria-label="Session size">
					{#each sessionSizes as size}
						<button type="button" class:selected={sessionSize === size} class="training-size-option" role="radio" aria-checked={sessionSize === size} onclick={() => (sessionSize = size)}>{size}</button>
					{/each}
				</div>
			</div>

			{#if createSession.error}<p class="form-error-msg">{createSession.error.message}</p>{/if}
			<Button size="lg" onclick={start} disabled={createSession.isPending}>{createSession.isPending ? 'Preparing session…' : 'Start flashcards'}</Button>
		</section>

		<aside class="training-context" aria-label="Vocabulary overview">
			<h2>Your vocabulary</h2>
			{#if stats.isPending}
				<p class="training-muted">Loading progress…</p>
			{:else if stats.data}
				<p class="training-context-total">{totalVocabulary}<span>words</span></p>
				<div class="training-mastery-list">
					{#each Object.entries(stats.data.masteryDistribution) as [level, count]}
						<div class="training-mastery-row"><span>{formatMode(level)}</span><div><i style:width={`${totalVocabulary === 0 ? 0 : (count / totalVocabulary) * 100}%`}></i></div><strong>{count}</strong></div>
					{/each}
				</div>
				<p class="training-context-note">{stats.data.totalSessions} completed session{stats.data.totalSessions === 1 ? '' : 's'}.</p>
			{:else}
				<p class="training-muted">Progress is unavailable.</p>
			{/if}
		</aside>
	</div>

	<section class="training-history" aria-labelledby="session-history-heading">
		<div class="training-history-header">
			<div><h2 id="session-history-heading">Session history</h2><p>Pick up an active session or revisit a completed one.</p></div>
			<label class="training-history-size">Show <select bind:value={historySize} onchange={() => (historyPage = 1)}>{#each historyPageSizes as size}<option value={size}>{size}</option>{/each}</select></label>
		</div>

		{#if sessions.isPending}
			<p class="training-muted">Loading sessions…</p>
		{:else if sessions.isError}
			<p class="form-error-msg">Could not load session history.</p>
		{:else if (sessions.data?.items ?? []).length === 0}
			<p class="training-history-empty">Your completed and active flashcard sessions will appear here.</p>
		{:else}
			<ul class="training-session-list">
				{#each sessions.data?.items ?? [] as session (session.id)}
					<li class="training-session-row">
						<div class="training-session-date"><span>{session.status === 'ACTIVE' ? 'In progress' : formatDate(session.completedAt ?? session.createdAt)}</span><small>{formatMode(session.mode)} · {session.totalWords} cards</small></div>
						{#if session.status === 'COMPLETED'}
							<div class="training-session-result"><strong>{accuracyLabel(session.accuracy)}</strong><span>{session.correctCount} correct · {session.incorrectCount} wrong</span></div>
						{:else}<span class="training-session-active">Active</span>{/if}
						<Button href={`/training/${session.id}`} variant={session.status === 'ACTIVE' ? 'default' : 'outline'} size="sm">{session.status === 'ACTIVE' ? 'Resume' : 'Review'}</Button>
					</li>
				{/each}
			</ul>
			<div class="training-pagination">
				<Button variant="outline" size="sm" disabled={historyPage <= 1} onclick={() => (historyPage -= 1)}>Previous</Button>
				<span>Page {historyPage} of {historyPageCount}</span>
				<Button variant="outline" size="sm" disabled={historyPage >= historyPageCount} onclick={() => (historyPage += 1)}>Next</Button>
			</div>
		{/if}
	</section>
</div>
