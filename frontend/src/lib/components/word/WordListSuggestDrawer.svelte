<script lang="ts">
import { untrack } from 'svelte';
import type { AiListWordSuggestion, Difficulty, PartOfSpeech } from '$lib/api/types.gen';
import { useAddWordToList, useSuggestWordsForList } from '$lib/stores/wordLists';
import { useCreateWord, useLookupWordByArabic } from '$lib/stores/words';

interface Props {
	listId: string;
	open: boolean;
	onClose: () => void;
	onAiUnavailable?: () => void;
}

const { listId, open, onClose, onAiUnavailable }: Props = $props();

const suggestMutation = useSuggestWordsForList();
const createWord = useCreateWord();
const lookupWord = useLookupWordByArabic();
const addWord = useAddWordToList();

let suggestions = $state<AiListWordSuggestion[] | null>(null);
let errorMessage = $state('');
let isAiNotConfigured = $state(false);

// Per-suggestion state machine — mirrors the enrich drawer's relation flow.
type ItemState = {
	status: 'checking' | 'exists' | 'missing' | 'adding' | 'added' | 'error';
	existingId?: string;
};
let itemStates = $state<ItemState[]>([]);

function runSuggest() {
	suggestMutation.mutate(listId, {
		onSuccess: (data: AiListWordSuggestion[]) => {
			suggestions = data;
			itemStates = data.map(() => ({ status: 'checking' as const }));
			for (let i = 0; i < data.length; i++) {
				void checkSuggestion(data[i], i);
			}
		},
		onError: (e: unknown) => {
			const err = e as { status?: number; message?: string };
			if (err.status === 503) {
				isAiNotConfigured = true;
				onAiUnavailable?.();
			} else {
				errorMessage = err.message ?? 'Failed to fetch suggestions.';
			}
		},
	});
}

async function checkSuggestion(s: AiListWordSuggestion, i: number) {
	itemStates[i] = { status: 'checking' };
	try {
		const existing = await lookupWord.mutateAsync(s.arabicText);
		itemStates[i] = existing
			? { status: 'exists', existingId: existing.id }
			: { status: 'missing' };
	} catch {
		itemStates[i] = { status: 'missing' };
	}
}

async function addSuggestion(s: AiListWordSuggestion, i: number) {
	const state = itemStates[i];
	itemStates[i] = { ...state, status: 'adding' };
	try {
		let wordId: string;
		if (state.status === 'exists' && state.existingId) {
			wordId = state.existingId;
		} else {
			const created = await createWord.mutateAsync({
				arabicText: s.arabicText,
				transliteration: s.transliteration ?? null,
				translation: s.translation ?? null,
				partOfSpeech: (s.partOfSpeech ?? 'UNKNOWN') as PartOfSpeech,
				dialect: s.dialect,
				difficulty: (s.difficulty ?? 'INTERMEDIATE') as Difficulty,
			});
			wordId = created.id;
		}
		await addWord.mutateAsync({ listId, wordId });
		itemStates[i] = { status: 'added', existingId: wordId };
	} catch {
		itemStates[i] = { ...state, status: 'error' };
	}
}

// Fire suggestion automatically when the drawer opens.
$effect(() => {
	if (open) {
		untrack(() => {
			suggestions = null;
			errorMessage = '';
			isAiNotConfigured = false;
			itemStates = [];
			runSuggest();
		});
	}
});

function formatEnum(value: string): string {
	return value
		.split('_')
		.map((part) => part.charAt(0) + part.slice(1).toLowerCase())
		.join(' ');
}
</script>

{#if open}
	<div
		class="modal-backdrop"
		role="button"
		tabindex="-1"
		aria-label="Close"
		onclick={onClose}
		onkeydown={(e) => e.key === 'Escape' && onClose()}
	>
		<!-- svelte-ignore a11y_click_events_have_key_events a11y_no_noninteractive_element_interactions -->
		<div
			class="modal"
			role="dialog"
			tabindex="-1"
			aria-label="AI word suggestions"
			onclick={(e) => e.stopPropagation()}
		>
			<div class="modal-header">
				<h2 class="modal-title">✦ AI Suggest words</h2>
				<button class="modal-close" onclick={onClose} aria-label="Close">×</button>
			</div>

			<div class="modal-body">
				{#if suggestMutation.isPending}
					<div class="drawer-loading">
						<div class="spinner"></div>
						<p>Fetching suggestions…</p>
					</div>
				{:else if isAiNotConfigured}
					<div class="drawer-notice">
						<p>AI not configured — set <code>OPENROUTER_API_KEY</code> to enable this feature.</p>
					</div>
				{:else if errorMessage}
					<div class="drawer-error">
						<p>{errorMessage}</p>
						<button class="btn btn-sm" onclick={runSuggest}>Try again</button>
					</div>
				{:else if suggestions}
					{#if suggestions.length === 0}
						<p class="annot-empty">No suggestions returned.</p>
					{:else}
						<div class="form-field">
							{#each suggestions as s, i (s.arabicText)}
								{@const st = itemStates[i] ?? { status: 'checking' }}
								<div class="drawer-relation-row">
									<span class="arabic-text" dir="rtl">{s.arabicText}</span>
									{#if s.transliteration}
										<span class="drawer-relation-tr">{s.transliteration}</span>
									{/if}
									{#if s.translation}
										<span class="drawer-relation-gloss">{s.translation}</span>
									{/if}
									{#if s.partOfSpeech}
										<span class="drawer-relation-type">{formatEnum(s.partOfSpeech)}</span>
									{/if}
									{#if s.dialect}
										<span class="drawer-relation-type">{formatEnum(s.dialect)}</span>
									{/if}
									{#if st.status === 'checking'}
										<span class="drawer-relation-action drawer-relation-checking">…</span>
									{:else if st.status === 'adding'}
										<span class="drawer-relation-action drawer-relation-checking">adding…</span>
									{:else if st.status === 'added'}
										<span class="drawer-relation-action drawer-relation-linked">✓ added</span>
									{:else if st.status === 'error'}
										<span class="drawer-relation-action drawer-relation-err">failed</span>
									{:else if st.status === 'exists'}
										<button
											class="btn btn-xs drawer-relation-action"
											onclick={() => addSuggestion(s, i)}
										>Add to list</button>
									{:else if st.status === 'missing'}
										<button
											class="btn btn-xs btn-primary drawer-relation-action"
											onclick={() => addSuggestion(s, i)}
										>Create & add</button>
									{/if}
								</div>
							{/each}
						</div>
					{/if}
				{/if}
			</div>
			<div class="modal-actions">
				<button class="btn" onclick={onClose}>Done</button>
			</div>
		</div>
	</div>
{/if}
