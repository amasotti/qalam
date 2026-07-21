<script lang="ts">
import { untrack } from 'svelte';
import type { AiRootWordSuggestion, WordResponse } from '$lib/api/types.gen';
import { useSuggestWordsForRoot } from '$lib/stores/roots';
import { useCreateWord, useLookupWordByArabic, useUpdateWord } from '$lib/stores/words';

type Match = 'checking' | 'missing' | 'current-family' | 'unlinked' | 'other-root';
type Phase = 'idle' | 'creating' | 'linking' | 'done' | 'failed';
type Row = {
	suggestion: AiRootWordSuggestion;
	match: Match;
	phase: Phase;
	word?: WordResponse;
	message?: string;
};
type RequestState = 'idle' | 'loading' | 'meaning-required' | 'failed' | 'unavailable';

interface Props {
	rootId: string;
	open: boolean;
	onClose: () => void;
	onAiUnavailable: () => void;
}

const { rootId, open, onClose, onAiUnavailable }: Props = $props();
const suggestWords = useSuggestWordsForRoot();
const lookupWord = useLookupWordByArabic();
const createWord = useCreateWord();
const updateWord = useUpdateWord();

let dialog = $state<HTMLDialogElement>();
let rows = $state<Row[]>([]);
let requestState = $state<RequestState>('idle');
let requestMessage = $state('');
let requestVersion = 0;

$effect(() => {
	if (!dialog) return;

	if (open && !dialog.open) {
		dialog.showModal();
		untrack(() => void resetAndSuggest());
	} else if (!open && dialog.open) {
		requestVersion += 1;
		dialog.close();
	}
});

function close() {
	requestVersion += 1;
	onClose();
}

async function resetAndSuggest() {
	const version = ++requestVersion;
	rows = [];
	requestState = 'loading';
	requestMessage = '';

	try {
		const suggestions: AiRootWordSuggestion[] = await suggestWords.mutateAsync(rootId);
		if (version !== requestVersion) return;

		rows = suggestions.map((suggestion) => ({ suggestion, match: 'checking', phase: 'idle' }));
		requestState = 'idle';
		await Promise.all(suggestions.map((_, index) => resolveRow(index, version)));
	} catch (error) {
		if (version !== requestVersion) return;

		const status = errorStatus(error);
		if (status === 503) {
			requestState = 'unavailable';
			onAiUnavailable();
		} else if (status === 400) {
			requestState = 'meaning-required';
		} else {
			requestState = 'failed';
			requestMessage = errorMessage(error, 'Could not fetch suggestions.');
		}
	}
}

async function resolveRow(index: number, version = requestVersion) {
	const row = rows[index];
	if (!row) return;

	rows[index] = { ...row, match: 'checking', phase: 'idle', message: undefined };
	try {
		const word = await lookupWord.mutateAsync(row.suggestion.arabicText);
		if (version !== requestVersion || !rows[index]) return;

		rows[index] = { ...rows[index], match: classify(word), word: word ?? undefined };
	} catch (error) {
		if (version !== requestVersion || !rows[index]) return;

		rows[index] = {
			...rows[index],
			phase: 'failed',
			message: errorMessage(error, 'Could not check existing vocabulary.'),
		};
	}
}

async function acceptRow(index: number) {
	const row = rows[index];
	if (!row) return;

	if (row.match === 'missing') {
		rows[index] = { ...row, phase: 'creating', message: undefined };
		try {
			const word = await createWord.mutateAsync({
				arabicText: row.suggestion.arabicText,
				transliteration: row.suggestion.transliteration,
				translation: row.suggestion.translation,
				partOfSpeech: row.suggestion.partOfSpeech,
				dialect: row.suggestion.dialect,
				difficulty: row.suggestion.difficulty ?? 'INTERMEDIATE',
				rootId,
			});
			rows[index] = { ...row, match: 'current-family', phase: 'done', word };
		} catch (error) {
			rows[index] = {
				...row,
				phase: 'failed',
				message: errorMessage(error, 'Could not create word.'),
			};
		}
		return;
	}

	if (row.match !== 'unlinked' || !row.word) return;

	rows[index] = { ...row, phase: 'linking', message: undefined };
	try {
		const word = await updateWord.mutateAsync({ id: row.word.id, body: { rootId } });
		rows[index] = { ...row, match: 'current-family', phase: 'done', word };
	} catch (error) {
		rows[index] = { ...row, phase: 'failed', message: errorMessage(error, 'Could not link word.') };
	}
}

function classify(word: WordResponse | null): Match {
	if (!word) return 'missing';
	if (word.rootId === rootId) return 'current-family';
	return word.rootId ? 'other-root' : 'unlinked';
}

function errorStatus(error: unknown): number | undefined {
	return typeof error === 'object' &&
		error !== null &&
		'status' in error &&
		typeof error.status === 'number'
		? error.status
		: undefined;
}

function errorMessage(error: unknown, fallback: string): string {
	return error instanceof Error && error.message ? error.message : fallback;
}

function formatEnum(value: string): string {
	return value
		.split('_')
		.map((part) => part.charAt(0) + part.slice(1).toLowerCase())
		.join(' ');
}
</script>

<dialog bind:this={dialog} class="root-family-dialog" aria-labelledby="root-family-dialog-title" oncancel={(event) => { event.preventDefault(); close(); }}>
	<header class="root-family-dialog-header">
		<div>
			<p class="root-family-dialog-kicker">Root family</p>
			<h2 id="root-family-dialog-title">✦ AI Suggest family words</h2>
		</div>
		<button class="modal-close" onclick={close} aria-label="Close suggestions">×</button>
	</header>

	<section class="root-family-dialog-body" aria-live="polite">
		{#if requestState === 'loading'}
			<p class="status-text status-text-muted">Finding curated family candidates…</p>
		{:else if requestState === 'unavailable'}
			<p class="root-family-notice">AI is not configured for this app.</p>
		{:else if requestState === 'meaning-required'}
			<p class="root-family-notice">Add a core meaning to this root before requesting family suggestions.</p>
		{:else if requestState === 'failed'}
			<div class="root-family-error">
				<p>{requestMessage}</p>
				<button class="btn btn-sm" onclick={() => void resetAndSuggest()}>Try again</button>
			</div>
		{:else if rows.length === 0}
			<p class="annot-empty">No suggestions returned.</p>
		{:else}
			<div class="root-family-suggestions">
				{#each rows as row, index (row.suggestion.arabicText)}
					<article class="root-family-suggestion">
						<div class="root-family-suggestion-main">
							<span class="root-family-suggestion-ar" dir="rtl">{row.suggestion.arabicText}</span>
							<span class="root-family-suggestion-tr">{row.suggestion.transliteration}</span>
							<span class="root-family-suggestion-translation">{row.suggestion.translation}</span>
							<div class="root-family-suggestion-meta">
								<span class="chip chip-sm c-muted">{formatEnum(row.suggestion.partOfSpeech)}</span>
								<span class="chip chip-sm c-muted">{formatEnum(row.suggestion.dialect)}</span>
								<span class="chip chip-sm c-muted">{formatEnum(row.suggestion.difficulty ?? 'INTERMEDIATE')}</span>
							</div>
						</div>

						<div class="root-family-suggestion-action">
							{#if row.match === 'checking'}
								<span class="status-text status-text-muted">Checking…</span>
							{:else if row.phase === 'creating'}
								<span class="status-text status-text-muted">Creating…</span>
							{:else if row.phase === 'linking'}
								<span class="status-text status-text-muted">Linking…</span>
							{:else if row.phase === 'done'}
								<span class="root-family-success">✓ Added to family</span>
							{:else if row.phase === 'failed'}
								<p class="root-family-row-error">
									{row.message}
									<button class="btn btn-xs" onclick={() => row.match === 'checking' ? void resolveRow(index) : void acceptRow(index)}>Retry</button>
								</p>
							{:else if row.match === 'current-family'}
								<span class="status-text status-text-muted">Already in family</span>
							{:else if row.match === 'unlinked'}
								<button class="btn btn-sm" onclick={() => void acceptRow(index)}>Link to family</button>
							{:else if row.match === 'other-root' && row.word}
								<a class="btn btn-sm" href="/words/{row.word.id}">View word</a>
							{:else}
								<button class="btn btn-sm btn-primary" onclick={() => void acceptRow(index)}>Create word</button>
							{/if}
						</div>
					</article>
				{/each}
			</div>
		{/if}
	</section>

	<footer class="root-family-dialog-footer">
		<button class="btn" onclick={close}>Done</button>
	</footer>
</dialog>
