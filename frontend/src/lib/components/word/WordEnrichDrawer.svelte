<script lang="ts">
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
}

const { wordId, open, onClose }: Props = $props();

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

// Fire enrichment automatically when drawer opens
$effect(() => {
	if (open) {
		suggestion = null;
		errorMessage = '';
		isAiNotConfigured = false;
		saving = false;
		saveError = '';
		saved = false;

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
				} else {
					errorMessage = errorObj.message ?? 'Failed to fetch suggestions.';
				}
			},
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
	<div class="drawer-panel" role="dialog" aria-label="AI Enrichment suggestions">
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
						class="btn"
						style="margin-top:0.75rem;font-size:0.8rem;"
						onclick={() => enrichMutation.mutate(wordId)}
					>Try again</button>
				</div>
			{:else if saved}
				<div class="drawer-success">
					<p>Saved successfully.</p>
				</div>
			{:else if suggestion}
				<!-- Notes -->
				{#if suggestion.notes}
					<div class="drawer-field">
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
					<div class="drawer-field">
						<label class="drawer-field-label">
							<input type="checkbox" bind:checked={acceptGender} />
							Gender: <strong>{suggestion.gender === 'MASCULINE' ? 'Masculine' : 'Feminine'}</strong>
						</label>
					</div>
				{/if}

				<!-- Verb pattern -->
				{#if suggestion.verbPattern}
					<div class="drawer-field">
						<label class="drawer-field-label">
							<input type="checkbox" bind:checked={acceptVerbPattern} />
							Verb form: <strong>Form {suggestion.verbPattern}</strong>
						</label>
					</div>
				{/if}

				<!-- Plurals -->
				{#if suggestion.plurals.length > 0}
					<div class="drawer-field">
						<div class="drawer-field-label" style="font-weight:600;margin-bottom:0.5rem;">
							Plurals
						</div>
						{#each suggestion.plurals as p, i}
							<label class="drawer-plural-row">
								<input type="checkbox" bind:checked={acceptedPlurals[i]} />
								<span class="drawer-plural-ar" dir="rtl">{p.pluralForm}</span>
								<span class="drawer-plural-type">({pluralTypeLabels[p.pluralType]})</span>
							</label>
						{/each}
					</div>
				{/if}

				<!-- Relations — read-only display -->
				{#if suggestion.relations.length > 0}
					<div class="drawer-field">
						<div class="drawer-field-label" style="font-weight:600;margin-bottom:0.5rem;">
							Relations (read-only — link manually via word UUID)
						</div>
						{#each suggestion.relations as rel}
							<div class="drawer-relation-row">
								<span class="drawer-relation-ar" dir="rtl">{rel.arabicText}</span>
								<span class="drawer-relation-type">{rel.relationType.toLowerCase()}</span>
							</div>
						{/each}
					</div>
				{/if}

				{#if saveError}
					<p style="color:var(--coral);font-size:0.8rem;margin-top:0.5rem;">{saveError}</p>
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

<style>
.drawer-backdrop {
	position: fixed;
	inset: 0;
	background: rgba(0, 0, 0, 0.35);
	z-index: 40;
}

.drawer-panel {
	position: fixed;
	top: 0;
	right: 0;
	bottom: 0;
	width: 26rem;
	max-width: 95vw;
	background: var(--white, #fff);
	border-left: 1px solid var(--border, #e2e8f0);
	z-index: 50;
	display: flex;
	flex-direction: column;
	box-shadow: -4px 0 24px rgba(0, 0, 0, 0.1);
}

.drawer-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 1rem 1.25rem;
	border-bottom: 1px solid var(--border, #e2e8f0);
}

.drawer-title {
	font-size: 1rem;
	font-weight: 700;
	margin: 0;
}

.drawer-close {
	background: none;
	border: none;
	font-size: 1.5rem;
	line-height: 1;
	cursor: pointer;
	color: var(--ink-ghost, #a0aec0);
	padding: 0 0.25rem;
}

.drawer-close:hover {
	color: var(--ink, #1a1a1a);
}

.drawer-body {
	flex: 1;
	overflow-y: auto;
	padding: 1.25rem;
	display: flex;
	flex-direction: column;
	gap: 1rem;
}

.drawer-loading {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 0.75rem;
	padding: 2rem 0;
	color: var(--ink-ghost, #a0aec0);
}

.spinner {
	width: 1.75rem;
	height: 1.75rem;
	border: 3px solid var(--border, #e2e8f0);
	border-top-color: var(--olive, #5a7a3a);
	border-radius: 50%;
	animation: spin 0.7s linear infinite;
}

@keyframes spin {
	to { transform: rotate(360deg); }
}

.drawer-notice,
.drawer-error,
.drawer-success {
	font-size: 0.875rem;
	padding: 0.75rem;
	border-radius: 8px;
}

.drawer-notice {
	background: var(--cerulean-pale, #ebf3ff);
}

.drawer-error {
	background: #fff5f5;
	color: var(--coral, #e53e3e);
}

.drawer-success {
	background: #f0fff4;
	color: #276749;
}

.drawer-field {
	display: flex;
	flex-direction: column;
	gap: 0.375rem;
}

.drawer-field-label {
	display: flex;
	align-items: center;
	gap: 0.5rem;
	font-size: 0.875rem;
	color: var(--ink-mid, #4a5568);
	cursor: pointer;
}

.drawer-textarea {
	font-size: 0.875rem;
	padding: 0.5rem;
	border: 1px solid var(--border, #e2e8f0);
	border-radius: 6px;
	resize: vertical;
	background: var(--white, #fff);
	line-height: 1.5;
}

.drawer-textarea:disabled {
	background: var(--bg-subtle, #f7fafc);
	color: var(--ink-ghost, #a0aec0);
}

.drawer-plural-row {
	display: flex;
	align-items: center;
	gap: 0.5rem;
	font-size: 0.875rem;
	padding: 0.25rem 0;
	cursor: pointer;
}

.drawer-plural-ar {
	font-family: 'Noto Naskh Arabic', serif;
	font-size: 1rem;
}

.drawer-plural-type {
	color: var(--ink-ghost, #a0aec0);
	font-size: 0.75rem;
}

.drawer-relation-row {
	display: flex;
	align-items: center;
	gap: 0.5rem;
	padding: 0.25rem 0;
	font-size: 0.875rem;
}

.drawer-relation-ar {
	font-family: 'Noto Naskh Arabic', serif;
	font-size: 1rem;
}

.drawer-relation-type {
	color: var(--ink-ghost, #a0aec0);
	font-size: 0.75rem;
	background: var(--bg-subtle, #f7fafc);
	padding: 0.1rem 0.35rem;
	border-radius: 4px;
}

.drawer-actions {
	display: flex;
	gap: 0.5rem;
	margin-top: 0.5rem;
	padding-top: 0.75rem;
	border-top: 1px solid var(--border, #e2e8f0);
}
</style>
