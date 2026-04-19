<script lang="ts">
	import { useGenerateExamples, useUpdateWord } from '$lib/stores/words';
	import type { ExampleSentenceResponse } from '$lib/api/types.gen';
	import Button from '$lib/components/ui/button/button.svelte';
	import { Sparkles, Check, X, RefreshCw } from 'lucide-svelte';

	interface Props {
		wordId: string;
	}

	const { wordId }: Props = $props();

	// Mutations
	const generateMutation = useGenerateExamples();
	const updateMutation = useUpdateWord();

	// State
	let isExpanded = $state(false);
	let examples = $state<ExampleSentenceResponse[]>([]);
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
					errorMessage =
						errorObj.message || 'Failed to generate examples. Please try again.';
				}
				isExpanded = true;
			},
		});
	}

	function handleUseExample(example: ExampleSentenceResponse) {
		updateMutation.mutate(
			{ id: wordId, body: { exampleSentence: example.arabic } },
			{
				onSuccess: () => {
					isExpanded = false;
					examples = [];
					errorMessage = '';
				},
				onError: (error) => {
					errorMessage =
						error instanceof Error ? error.message : 'Failed to save example';
				},
			}
		);
	}

	function handleDiscard() {
		isExpanded = false;
		examples = [];
		errorMessage = '';
		isAiNotConfigured = false;
	}

	function handleGenerateAgain() {
		handleGenerateClick();
	}

	const isGenerateLoading = $derived(generateMutation.isPending);
	const isUpdateLoading = $derived(updateMutation.isPending);
	const isAnyLoading = $derived(isGenerateLoading || isUpdateLoading);
</script>

<div class="ai-examples">
	{#if !isExpanded}
		<div class="ai-examples-trigger">
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
		<div class="ai-examples-loading">
			<p>Generating examples…</p>
		</div>
	{:else if isAiNotConfigured}
		<div class="ai-examples-notice">
			<p>
				AI not configured — set <code>OPENROUTER_API_KEY</code> to enable this feature.
			</p>
		</div>
		<div class="ai-examples-footer">
			<Button onclick={handleDiscard} size="sm" variant="outline">Close</Button>
		</div>
	{:else if errorMessage}
		<div class="ai-examples-error">
			<p>{errorMessage}</p>
		</div>
		<div class="ai-examples-footer">
			<Button onclick={handleDiscard} size="sm" variant="outline">Close</Button>
			<Button onclick={handleGenerateAgain} size="sm" variant="outline">
				<RefreshCw size={16} />
				Try again
			</Button>
		</div>
	{:else if examples.length > 0}
		<div class="ai-examples-results">
			{#each examples as example (example.arabic)}
				<div class="ai-example-card">
					<div class="ai-example-arabic">{example.arabic}</div>
					<div class="ai-example-transliteration">{example.transliteration}</div>
					<div class="ai-example-translation">{example.translation}</div>
					<div class="ai-example-actions">
						<Button
							onclick={() => handleUseExample(example)}
							disabled={isAnyLoading}
							size="sm"
						>
							<Check size={16} />
							Use this example
						</Button>
						<Button
							onclick={handleDiscard}
							disabled={isAnyLoading}
							size="sm"
							variant="outline"
						>
							<X size={16} />
							Discard
						</Button>
					</div>
				</div>
			{/each}
		</div>
		<div class="ai-examples-footer">
			<Button
				onclick={handleGenerateAgain}
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
