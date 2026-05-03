<script lang="ts">
import { untrack } from 'svelte';
import type { AiPluralSuggestion, WordEnrichmentSuggestion } from '$lib/api/types.gen';
import {
	useAddWordPlural,
	useEnrichWord,
	useUpdateWord,
	useUpsertMorphology,
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
const addPlural = useAddWordPlural();
const updateWord = useUpdateWord();

// suggestions state
let suggestion = $state<WordEnrichmentSuggestion | null>(null);
let errorMessage = $state('');
let isAiNotConfigured = $state(false);

// acceptance state
let acceptNotes = $state(true);
let editedNotes = $state('');
let acceptGender = $state(true);
let acceptVerbPattern = $state(true);
let acceptedPlurals = $state<boolean[]>([]);

let saving = $state(false);
let saveError = $state('');
let saved = $state(false);

function runEnrichment() {
	enrichMutation.mutate(wordId, {
		onSuccess: (data) => {
			suggestion = data;
			editedNotes = data.notes ?? '';
			acceptNotes = !!data.notes;
			acceptGender = !!data.gender;
			acceptVerbPattern = !!data.verbPattern;
			acceptedPlurals = data.plurals.map(() => true);
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

		// Morphology (gender + verbPattern together)
		const hasGender = acceptGender && suggestion.gender;
		const hasPattern = acceptVerbPattern && suggestion.verbPattern;
		if (hasGender || hasPattern) {
			await upsertMorphology.mutateAsync({
				id: wordId,
				body: {
					...(hasGender ? { gender: suggestion.gender } : {}),
					...(hasPattern ? { verbPattern: suggestion.verbPattern } : {}),
				},
			});
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
	<!-- Backdrop -->
	<div
		class="drawer-backdrop"
		role="button"
		tabindex="-1"
		aria-label="Close drawer"
		onclick={onClose}
		onkeydown={(e) => e.key === 'Escape' && onClose()}
	></div>

	<!-- Drawer panel -->
		<div class="drawer drawer-lg" role="dialog" aria-label="AI Enrichment suggestions">
		<div class="drawer-header">
			<h2 class="drawer-title">✦ AI Enrichment</h2>
			<button class="drawer-close" onclick={onClose} aria-label="Close">×</button>
		</div>

		<div class="drawer-body">
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
						<button
							class="btn btn-sm dict-links-add-actions"
							onclick={runEnrichment}
						>Try again</button>
				</div>
			{:else if saved}
				<div class="drawer-success">
					<p>Saved successfully.</p>
				</div>
			{:else if suggestion}
				<!-- Notes -->
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

				<!-- Gender -->
				{#if suggestion.gender}
					<div class="form-field">
						<label class="drawer-field-label">
							<input type="checkbox" bind:checked={acceptGender} />
							Gender: <strong>{suggestion.gender === 'MASCULINE' ? 'Masculine' : 'Feminine'}</strong>
						</label>
					</div>
				{/if}

				<!-- Verb pattern -->
				{#if suggestion.verbPattern}
					<div class="form-field">
						<label class="drawer-field-label">
							<input type="checkbox" bind:checked={acceptVerbPattern} />
							Verb form: <strong>Form {suggestion.verbPattern}</strong>
						</label>
					</div>
				{/if}

				<!-- Plurals -->
					{#if suggestion.plurals.length > 0}
						<div class="form-field">
							<div class="drawer-field-label drawer-field-title">
								Plurals
							</div>
						{#each suggestion.plurals as p, i}
							<label class="drawer-plural-row">
								<input type="checkbox" bind:checked={acceptedPlurals[i]} />
								<span class="arabic-text" dir="rtl">{p.pluralForm}</span>
								<span class="drawer-plural-type">({pluralTypeLabels[p.pluralType]})</span>
							</label>
						{/each}
					</div>
				{/if}

				<!-- Relations — read-only display -->
					{#if suggestion.relations.length > 0}
						<div class="form-field">
							<div class="drawer-field-label drawer-field-title">
								Relations (read-only — link manually via word UUID)
							</div>
						{#each suggestion.relations as rel}
							<div class="drawer-relation-row">
								<span class="arabic-text" dir="rtl">{rel.arabicText}</span>
								<span class="drawer-relation-type">{rel.relationType.toLowerCase()}</span>
							</div>
						{/each}
					</div>
				{/if}

				{#if saveError}
					<p class="form-error-msg">{saveError}</p>
				{/if}

				<div class="drawer-actions">
					<button
						class="btn btn-primary"
						onclick={handleSave}
						disabled={saving}
					>{saving ? 'Saving…' : 'Save accepted'}</button>
					<button class="btn" onclick={onClose} disabled={saving}>Discard</button>
				</div>
			{/if}
		</div>
	</div>
{/if}
