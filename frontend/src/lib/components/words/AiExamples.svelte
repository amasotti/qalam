<script lang="ts">
import { Check, RefreshCw, Sparkles, X } from 'lucide-svelte';
import type { AiExampleSentence } from '$lib/api/types.gen';
import Button from '$lib/components/ui/button/button.svelte';
import { useGenerateExamples, useSaveWordExample } from '$lib/stores/words';

interface Props {
	wordId: string;
}

const { wordId }: Props = $props();

const generateMutation = useGenerateExamples();
const saveMutation = useSaveWordExample();

let isExpanded = $state(false);
let examples = $state<AiExampleSentence[]>([]);
let errorMessage = $state('');
let isAiNotConfigured = $state(false);

function handleGenerateClick() {
	examples = [];
	errorMessage = '';
	isAiNotConfigured = false;

	generateMutation.mutate(wordId, {
		onSuccess: (data) => {
			examples = data.examples;
			isExpanded = true;
		},
		onError: (error) => {
			const errorObj = error as unknown as { status?: number; message?: string };
			if (errorObj.status === 503) {
				isAiNotConfigured = true;
			} else {
				errorMessage = errorObj.message || 'Failed to generate examples. Please try again.';
			}
			isExpanded = true;
		},
	});
}

function handleUseExample(example: AiExampleSentence) {
	saveMutation.mutate(
		{
			id: wordId,
			body: {
				arabic: example.arabic,
				transliteration: example.transliteration,
				translation: example.translation,
			},
		},
		{
			onSuccess: () => {
				examples = examples.filter((e) => e !== example);
				if (examples.length === 0) {
					isExpanded = false;
					errorMessage = '';
				}
			},
			onError: (error) => {
				errorMessage = error instanceof Error ? error.message : 'Failed to save example';
			},
		}
	);
}

function handleDiscardExample(example: AiExampleSentence) {
	examples = examples.filter((e) => e !== example);
	if (examples.length === 0) {
		isExpanded = false;
		errorMessage = '';
		isAiNotConfigured = false;
	}
}

function handleDismissAll() {
	isExpanded = false;
	examples = [];
	errorMessage = '';
	isAiNotConfigured = false;
}

const isGenerateLoading = $derived(generateMutation.isPending);
const isSaveLoading = $derived(saveMutation.isPending);
const isAnyLoading = $derived(isGenerateLoading || isSaveLoading);
</script>

<div class="ai-examples">
	{#if !isExpanded}
		<div class="ai-trigger">
			<Button
				onclick={handleGenerateClick}
				disabled={isGenerateLoading}
				size="sm"
				variant="outline"
			>
				<Sparkles size={16} />
				Generate examples
			</Button>
		</div>
	{:else if isGenerateLoading}
		<div class="ai-loading">
			<p>Generating examples…</p>
		</div>
	{:else if isAiNotConfigured}
		<div class="ai-notice">
			<p>AI not configured — set <code>OPENROUTER_API_KEY</code> to enable this feature.</p>
		</div>
		<div class="ai-footer">
			<Button onclick={handleDismissAll} size="sm" variant="outline">Close</Button>
		</div>
	{:else if errorMessage}
		<div class="ai-error">
			<p>{errorMessage}</p>
		</div>
		<div class="ai-footer">
			<Button onclick={handleDismissAll} size="sm" variant="outline">Close</Button>
			<Button onclick={handleGenerateClick} size="sm" variant="outline">
				<RefreshCw size={16} />
				Try again
			</Button>
		</div>
	{:else if examples.length > 0}
		<div class="ai-results">
			{#each examples as example (example.arabic)}
				<div class="ai-card">
					<div class="ai-card-body">
						<p class="example-card-ar">{example.arabic}</p>
						{#if example.transliteration}
							<p class="example-card-tr">{example.transliteration}</p>
						{/if}
						{#if example.translation}
							<p class="example-card-en">{example.translation}</p>
						{/if}
					</div>
					<div class="ai-card-actions">
						<Button
							onclick={() => handleUseExample(example)}
							disabled={isAnyLoading}
							size="sm"
						>
							<Check size={14} />
							Use
						</Button>
						<Button
							onclick={() => handleDiscardExample(example)}
							disabled={isAnyLoading}
							size="sm"
							variant="outline"
						>
							<X size={14} />
							Discard
						</Button>
					</div>
				</div>
			{/each}
		</div>
		<div class="ai-footer">
			<Button
				onclick={handleGenerateClick}
				disabled={isGenerateLoading}
				size="sm"
				variant="outline"
			>
				<RefreshCw size={16} />
				Generate again
			</Button>
		</div>
	{/if}
</div>

<style>
.ai-examples {
	margin-top: 0.5rem;
}

.ai-trigger {
	display: flex;
	justify-content: flex-start;
}

.ai-loading,
.ai-notice,
.ai-error {
	padding: 0.875rem 1.125rem;
	border-radius: 0 8px 8px 0;
	border-left: 3px solid var(--cerulean);
	background: var(--cerulean-pale);
	font-size: 0.875rem;
	color: var(--cerulean);
}

.ai-error {
	border-left-color: var(--coral);
	background: var(--coral-pale);
	color: var(--coral);
}

.ai-loading {
	color: var(--ink-ghost);
	border-left-color: var(--border);
	background: var(--bg-dark);
}

.ai-results {
	display: flex;
	flex-direction: column;
	gap: 0.75rem;
}

/* AI example card — same shape as example-card but cerulean accent */
.ai-card {
	position: relative;
	padding: 1rem 1.125rem;
	border-left: 3px solid var(--cerulean);
	background: var(--cerulean-pale);
	border-radius: 0 8px 8px 0;
	display: flex;
	gap: 1rem;
	align-items: flex-start;
}

.ai-card-body {
	flex: 1;
	min-width: 0;
}

.ai-card-actions {
	display: flex;
	flex-direction: column;
	gap: 0.375rem;
	flex-shrink: 0;
}

.ai-footer {
	display: flex;
	gap: 0.5rem;
	margin-top: 0.625rem;
}
</style>
