<script lang="ts">
import { untrack } from 'svelte';
import type {
	AiPluralSuggestion,
	AiRelationSuggestion,
	WordEnrichmentSuggestion,
} from '$lib/api/types.gen';
import {
	useAddWordPlural,
	useAddWordRelation,
	useCreateWord,
	useEnrichWord,
	useLookupWordByArabic,
	useUpdateWord,
	useUpsertMorphology,
	useUpsertVerbDetails,
} from '$lib/stores/words';

interface Props {
	wordId: string;
	open: boolean;
	onClose: () => void;
	onAiUnavailable?: () => void;
}

const { wordId, open, onClose, onAiUnavailable }: Props = $props();

const enrichMutation = useEnrichWord();
const upsertMorphology = useUpsertMorphology();
const upsertVerbDetails = useUpsertVerbDetails();
const addPlural = useAddWordPlural();
const updateWord = useUpdateWord();
const addRelation = useAddWordRelation();
const createWord = useCreateWord();
const lookupWord = useLookupWordByArabic();

// suggestions state
let suggestion = $state<WordEnrichmentSuggestion | null>(null);
let errorMessage = $state('');
let isAiNotConfigured = $state(false);

// acceptance state
let acceptNotes = $state(true);
let editedNotes = $state('');
let acceptGender = $state(true);
let acceptVerbDetails = $state(true);
let acceptedPlurals = $state<boolean[]>([]);

let saving = $state(false);
let saveError = $state('');
let saved = $state(false);

// Per-relation state: 'idle' | 'checking' | 'exists' | 'missing' | 'linking' | 'linked' | 'error'
type RelState = {
	status: 'idle' | 'checking' | 'exists' | 'missing' | 'linking' | 'linked' | 'error';
	existingId?: string;
};
let relStates = $state<RelState[]>([]);

function runEnrichment() {
	enrichMutation.mutate(wordId, {
		onSuccess: (data) => {
			suggestion = data;
			editedNotes = data.notes ?? '';
			acceptNotes = !!data.notes;
			acceptGender = !!data.gender;
			acceptVerbDetails = !!data.verbDetails;
			acceptedPlurals = data.plurals.map(() => true);
			relStates = data.relations.map(() => ({ status: 'idle' as const }));
			// Kick off existence checks immediately (fire-and-forget, errors handled inside)
			for (let i = 0; i < data.relations.length; i++) {
				void checkRelation(data.relations[i], i);
			}
		},
		onError: (e) => {
			const errorObj = e as unknown as { status?: number; message?: string };
			if (errorObj.status === 503) {
				isAiNotConfigured = true;
				onAiUnavailable?.();
			} else {
				errorMessage = errorObj.message ?? 'Failed to fetch suggestions.';
			}
		},
	});
}

async function checkRelation(rel: AiRelationSuggestion, i: number) {
	relStates[i] = { status: 'checking' };
	try {
		const existing = await lookupWord.mutateAsync(rel.arabicText);
		relStates[i] = existing ? { status: 'exists', existingId: existing.id } : { status: 'missing' };
	} catch {
		relStates[i] = { status: 'missing' };
	}
}

async function linkRelation(rel: AiRelationSuggestion, i: number) {
	const state = relStates[i];
	relStates[i] = { ...state, status: 'linking' };
	try {
		let targetId: string;
		if (state.status === 'exists' && state.existingId) {
			targetId = state.existingId;
		} else {
			// Create the word first with available metadata
			const created = await createWord.mutateAsync({
				arabicText: rel.arabicText,
				transliteration: rel.transliteration ?? null,
				translation: rel.translation ?? null,
				partOfSpeech: 'UNKNOWN',
				dialect: 'MSA',
				difficulty: 'BEGINNER',
			});
			targetId = created.id;
		}
		await addRelation.mutateAsync({
			id: wordId,
			body: { relatedWordId: targetId, relationType: rel.relationType },
		});
		relStates[i] = { status: 'linked', existingId: targetId };
	} catch {
		relStates[i] = { ...state, status: 'error' };
	}
}

// Fire enrichment automatically when drawer opens.
// untrack() prevents enrichMutation's reactive state changes from re-triggering this effect.
$effect(() => {
	if (open) {
		untrack(() => {
			suggestion = null;
			errorMessage = '';
			isAiNotConfigured = false;
			saving = false;
			saveError = '';
			saved = false;
			relStates = [];
			runEnrichment();
		});
	}
});

async function handleSave() {
	if (!suggestion) return;
	saving = true;
	saveError = '';

	try {
		// Notes
		if (acceptNotes && editedNotes.trim()) {
			await updateWord.mutateAsync({ id: wordId, body: { notes: editedNotes.trim() } });
		}

		// Noun/adjective morphology
		const hasGender = acceptGender && suggestion.gender;
		if (hasGender) {
			await upsertMorphology.mutateAsync({
				id: wordId,
				body: { gender: suggestion.gender },
			});
		}

		if (acceptVerbDetails && suggestion.verbDetails) {
			await upsertVerbDetails.mutateAsync({ id: wordId, body: suggestion.verbDetails });
		}

		// Plurals
		const toAdd: AiPluralSuggestion[] = suggestion.plurals.filter((_, i) => acceptedPlurals[i]);
		for (const p of toAdd) {
			await addPlural.mutateAsync({
				id: wordId,
				body: { pluralForm: p.pluralForm, pluralType: p.pluralType },
			});
		}

		saved = true;
		setTimeout(() => {
			onClose();
		}, 800);
	} catch (e) {
		saveError = e instanceof Error ? e.message : 'Failed to save some suggestions.';
	} finally {
		saving = false;
	}
}

const pluralTypeLabels: Record<AiPluralSuggestion['pluralType'], string> = {
	SOUND_MASC: 'sound m.',
	SOUND_FEM: 'sound f.',
	BROKEN: 'broken',
	PAUCAL: 'paucal',
	COLLECTIVE: 'collective',
	OTHER: 'other',
};
</script>

{#if open}
	<!-- Backdrop acts as click-outside close -->
	<div
		class="modal-backdrop"
		role="button"
		tabindex="-1"
		aria-label="Close"
		onclick={onClose}
		onkeydown={(e) => e.key === 'Escape' && onClose()}
	>
		<!-- Modal panel — stop propagation so clicks inside don't close -->
		<!-- svelte-ignore a11y_click_events_have_key_events a11y_no_noninteractive_element_interactions -->
		<div class="modal" role="dialog" tabindex="-1" aria-label="AI Enrichment suggestions" onclick={(e) => e.stopPropagation()}>
			<div class="modal-header">
				<h2 class="modal-title">✦ AI Enrichment</h2>
				<button class="modal-close" onclick={onClose} aria-label="Close">×</button>
			</div>

			<div class="modal-body">
				{#if enrichMutation.isPending}
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
						<button class="btn btn-sm" onclick={runEnrichment}>Try again</button>
					</div>
				{:else if saved}
					<div class="drawer-success">
						<p>Saved successfully.</p>
					</div>
				{:else if suggestion}
					<div class="modal-body-cols">
						<!-- Left column: morphology + plurals -->
						<div class="enrich-col">
							{#if suggestion.gender}
								<div class="form-field">
									<label class="drawer-field-label">
										<input type="checkbox" bind:checked={acceptGender} />
										Gender: <strong>{suggestion.gender === 'MASCULINE' ? 'Masculine' : 'Feminine'}</strong>
									</label>
								</div>
							{/if}

							{#if suggestion.verbDetails}
								<div class="form-field">
									<label class="drawer-field-label">
										<input type="checkbox" bind:checked={acceptVerbDetails} />
										Verb: <strong>Form {suggestion.verbDetails.verbForm}</strong>
										· {suggestion.verbDetails.weaknessType.toLowerCase().replace('_', ' ')}
									</label>
									{#if suggestion.verbDetails.pastPattern || suggestion.verbDetails.presentPattern}
										<div class="form-hint">{suggestion.verbDetails.pastPattern ?? '—'} · {suggestion.verbDetails.presentPattern ?? '—'}</div>
									{/if}
								</div>
							{/if}

							{#if suggestion.plurals.length > 0}
								<div class="form-field">
									<div class="drawer-field-label drawer-field-title">Plurals</div>
									{#each suggestion.plurals as p, i}
										<label class="drawer-plural-row">
											<input type="checkbox" bind:checked={acceptedPlurals[i]} />
											<span class="arabic-text" dir="rtl">{p.pluralForm}</span>
											<span class="drawer-plural-type">({pluralTypeLabels[p.pluralType]})</span>
										</label>
									{/each}
								</div>
							{/if}

							{#if !suggestion.gender && !suggestion.verbDetails && suggestion.plurals.length === 0}
								<p class="annot-empty">No morphology suggestions.</p>
							{/if}
						</div>

						<!-- Right column: relations + notes -->
						<div class="enrich-col">
							{#if suggestion.relations.length > 0}
								<div class="form-field">
									<div class="drawer-field-label drawer-field-title">Relations</div>
									{#each suggestion.relations as rel, i}
										{@const rs = relStates[i] ?? { status: 'idle' }}
										<div class="drawer-relation-row">
											<span class="arabic-text" dir="rtl">{rel.arabicText}</span>
											{#if rel.transliteration}
												<span class="drawer-relation-tr">{rel.transliteration}</span>
											{/if}
											{#if rel.translation}
												<span class="drawer-relation-gloss">{rel.translation}</span>
											{/if}
											<span class="drawer-relation-type">{rel.relationType.toLowerCase()}</span>
											{#if rs.status === 'idle' || rs.status === 'checking'}
												<span class="drawer-relation-action drawer-relation-checking">…</span>
											{:else if rs.status === 'linked'}
												<span class="drawer-relation-action drawer-relation-linked">✓ linked</span>
											{:else if rs.status === 'error'}
												<span class="drawer-relation-action drawer-relation-err">failed</span>
											{:else if rs.status === 'linking'}
												<span class="drawer-relation-action drawer-relation-checking">linking…</span>
											{:else if rs.status === 'exists'}
												<button
													class="btn btn-xs drawer-relation-action"
													onclick={() => linkRelation(rel, i)}
												>Link ↗</button>
											{:else if rs.status === 'missing'}
												<button
													class="btn btn-xs btn-primary drawer-relation-action"
													onclick={() => linkRelation(rel, i)}
												>Create & link</button>
											{/if}
										</div>
									{/each}
								</div>
							{/if}

							{#if suggestion.notes}
								<div class="form-field">
									<label class="drawer-field-label">
										<input type="checkbox" bind:checked={acceptNotes} />
										Notes
									</label>
									<textarea
										class="drawer-textarea"
										bind:value={editedNotes}
										disabled={!acceptNotes || saving}
										rows="4"
									></textarea>
								</div>
							{/if}
						</div>
					</div>

					{#if saveError}
						<p class="form-error-msg">{saveError}</p>
					{/if}
				{/if}
			</div>

			{#if suggestion && !saved}
				<div class="modal-actions">
					<button class="btn btn-primary" onclick={handleSave} disabled={saving}>
						{saving ? 'Saving…' : 'Save accepted'}
					</button>
					<button class="btn" onclick={onClose} disabled={saving}>Discard</button>
				</div>
			{/if}
		</div>
	</div>
{/if}
