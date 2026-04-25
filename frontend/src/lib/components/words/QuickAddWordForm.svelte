<script lang="ts">
import type { PartOfSpeech } from '$lib/api/types.gen';
import { useAnalyzeWord, useCreateWord } from '$lib/stores/words';
import { Button } from '$lib/components/ui/button';
import { Input } from '$lib/components/ui/input';

interface Props {
	arabicText: string;
	onCreated: (wordId: string) => void;
	onCancel: () => void;
}

let { arabicText, onCreated, onCancel }: Props = $props();

const analyzeWord = useAnalyzeWord();
const createWord = useCreateWord();

let transliteration = $state('');
let translation = $state('');
let partOfSpeech = $state<PartOfSpeech>('UNKNOWN');

const posOptions: PartOfSpeech[] = [
	'UNKNOWN',
	'NOUN',
	'VERB',
	'ADJECTIVE',
	'ADVERB',
	'PREPOSITION',
	'PARTICLE',
	'INTERJECTION',
	'CONJUNCTION',
	'PRONOUN',
];

async function handleAnalyze() {
	const result = await analyzeWord.mutateAsync(arabicText);
	transliteration = result.transliteration ?? '';
	translation = result.translation ?? '';
	partOfSpeech = (result.partOfSpeech as PartOfSpeech | null) ?? 'UNKNOWN';
}

async function handleSubmit(e: SubmitEvent) {
	e.preventDefault();
	const result = await createWord.mutateAsync({
		arabicText,
		transliteration: transliteration.trim() || null,
		translation: translation.trim() || null,
		partOfSpeech,
	});
	onCreated(result.id);
}
</script>

<form class="quick-add-form" onsubmit={handleSubmit}>
  <div class="quick-add-arabic arabic-text">{arabicText}</div>

  <div class="quick-add-ai">
    <Button
      type="button"
      variant="outline"
      size="sm"
      disabled={analyzeWord.isPending}
      onclick={handleAnalyze}
    >
      {analyzeWord.isPending ? 'Analyzing…' : 'Analyze with AI'}
    </Button>
    {#if analyzeWord.isError}
      <span class="quick-add-error">AI unavailable</span>
    {/if}
  </div>

  <div class="quick-add-field">
    <label class="quick-add-label" for="qa-translit">Transliteration</label>
    <Input id="qa-translit" bind:value={transliteration} placeholder="e.g. kataba" />
  </div>

  <div class="quick-add-field">
    <label class="quick-add-label" for="qa-translation">Translation</label>
    <Input id="qa-translation" bind:value={translation} placeholder="English meaning" />
  </div>

  <div class="quick-add-field">
    <label class="quick-add-label" for="qa-pos">Part of speech</label>
    <select id="qa-pos" class="quick-add-select" bind:value={partOfSpeech}>
      {#each posOptions as pos}
        <option value={pos}>{pos.charAt(0) + pos.slice(1).toLowerCase()}</option>
      {/each}
    </select>
  </div>

  <div class="quick-add-actions">
    <Button type="submit" size="sm" disabled={createWord.isPending}>
      {createWord.isPending ? 'Saving…' : 'Add to vocabulary'}
    </Button>
    <Button type="button" variant="ghost" size="sm" onclick={onCancel}>Cancel</Button>
  </div>
</form>

<style>
.quick-add-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.quick-add-arabic {
  font-size: 1.5rem;
  text-align: center;
  padding: 0.5rem 0;
  color: hsl(var(--foreground));
}
.quick-add-ai {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.quick-add-error {
  font-size: 0.75rem;
  color: hsl(var(--destructive));
}
.quick-add-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: hsl(var(--foreground));
}
.quick-add-field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}
.quick-add-select {
  width: 100%;
  padding: 0.375rem 0.75rem;
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
  background: hsl(var(--background));
  color: hsl(var(--foreground));
  font-size: 0.875rem;
}
.quick-add-actions {
  display: flex;
  gap: 0.5rem;
  padding-top: 0.5rem;
}
</style>
