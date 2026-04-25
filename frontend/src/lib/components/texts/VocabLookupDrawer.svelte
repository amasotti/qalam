<script lang="ts">
import type { AlignmentTokenResponse, WordResponse } from '$lib/api/types.gen';
import { fly } from 'svelte/transition';
import QuickAddWordForm from '$lib/components/words/QuickAddWordForm.svelte';
import { useLookupWordByArabic } from '$lib/stores/words';

interface Props {
	open: boolean;
	token: AlignmentTokenResponse | null;
	onclose: () => void;
	onannotate: (anchor: string) => void;
}

let { open, token, onclose, onannotate }: Props = $props();

const lookup = useLookupWordByArabic();

let found = $state<WordResponse | null>(null);
let notFound = $state(false);
let loading = $state(false);

$effect(() => {
	if (!open || !token) return;
	found = null;
	notFound = false;
	loading = true;
	lookup.mutateAsync(token.arabic).then((word) => {
		found = word;
		notFound = word === null;
		loading = false;
	});
});

function handleAnnotate() {
	if (!token) return;
	onannotate(token.arabic);
	onclose();
}

function handleCreated(_wordId: string) {
	loading = true;
	notFound = false;
	lookup.mutateAsync(token!.arabic).then((word) => {
		found = word;
		loading = false;
	});
}

function handleKeydown(e: KeyboardEvent) {
	if (e.key === 'Escape') onclose();
}
</script>

<svelte:window onkeydown={handleKeydown} />

{#if open}
  <div
    class="vocab-backdrop"
    onclick={onclose}
    role="button"
    aria-label="Close"
    tabindex="-1"
  ></div>

  <aside class="vocab-drawer" transition:fly={{ x: 360, duration: 220, opacity: 1 }}>
    <header class="vocab-header">
      <span class="vocab-header-arabic arabic-text">{token?.arabic ?? ''}</span>
      <button class="vocab-close" onclick={onclose} aria-label="Close">×</button>
    </header>

    <div class="vocab-body">
      {#if loading}
        <p class="vocab-state-msg">Looking up…</p>
      {:else if found}
        <div class="vocab-card">
          <p class="vocab-card-arabic arabic-text">{found.arabicText}</p>
          {#if found.transliteration}
            <p class="vocab-card-translit transliteration">{found.transliteration}</p>
          {/if}
          {#if found.translation}
            <p class="vocab-card-translation">{found.translation}</p>
          {/if}
          <span class="vocab-mastery-badge vocab-mastery-{found.masteryLevel.toLowerCase()}">
            {found.masteryLevel}
          </span>
          <a href="/words/{found.id}" class="vocab-open-link">Open in vocabulary →</a>
        </div>
      {:else if notFound}
        <p class="vocab-state-msg vocab-not-found-msg">Not in vocabulary yet.</p>
        <QuickAddWordForm
          arabicText={token!.arabic}
          onCreated={handleCreated}
          onCancel={onclose}
        />
      {/if}
    </div>

    {#if found}
      <footer class="vocab-footer">
        <button class="vocab-annotate-btn" onclick={handleAnnotate}>
          View / Add Annotations →
        </button>
      </footer>
    {/if}
  </aside>
{/if}

<style>
.vocab-backdrop {
  position: fixed;
  inset: 0;
  background: hsl(var(--foreground) / 0.2);
  z-index: 40;
  cursor: default;
}
.vocab-drawer {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  width: 340px;
  z-index: 50;
  background: hsl(var(--background));
  border-left: 1px solid hsl(var(--border));
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.vocab-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid hsl(var(--border));
  flex-shrink: 0;
}
.vocab-header-arabic { font-size: 1.5rem; line-height: 1.4; }
.vocab-close {
  font-size: 1.25rem;
  line-height: 1;
  background: none;
  border: none;
  cursor: pointer;
  color: hsl(var(--foreground) / 0.6);
  padding: 0.25rem;
}
.vocab-body {
  flex: 1;
  overflow-y: auto;
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.vocab-state-msg {
  font-size: 0.875rem;
  color: hsl(var(--foreground) / 0.6);
  margin: 0;
}
.vocab-not-found-msg { margin-bottom: 0.5rem; }
.vocab-card { display: flex; flex-direction: column; gap: 0.5rem; }
.vocab-card-arabic { font-size: 1.75rem; margin: 0; line-height: 1.6; }
.vocab-card-translit { font-size: 0.875rem; margin: 0; color: hsl(var(--foreground) / 0.7); }
.vocab-card-translation { font-size: 1rem; margin: 0; font-weight: 500; }
.vocab-mastery-badge {
  display: inline-block;
  font-size: 0.6875rem;
  font-weight: 600;
  padding: 0.125rem 0.5rem;
  border-radius: 9999px;
  width: fit-content;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.vocab-mastery-new      { background: hsl(var(--muted));           color: hsl(var(--muted-foreground)); }
.vocab-mastery-learning { background: hsl(40 90% 60% / 0.2);       color: hsl(40 70% 40%); }
.vocab-mastery-known    { background: hsl(210 80% 60% / 0.2);      color: hsl(210 60% 40%); }
.vocab-mastery-mastered { background: hsl(140 60% 40% / 0.15);     color: hsl(140 50% 32%); }
.vocab-open-link {
  font-size: 0.8125rem;
  color: hsl(var(--primary));
  text-decoration: none;
  margin-top: 0.25rem;
}
.vocab-open-link:hover { text-decoration: underline; }
.vocab-footer {
  border-top: 1px solid hsl(var(--border));
  padding: 0.75rem 1.25rem;
  flex-shrink: 0;
}
.vocab-annotate-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 0.8125rem;
  color: hsl(var(--foreground) / 0.7);
  padding: 0;
}
.vocab-annotate-btn:hover { color: hsl(var(--foreground)); text-decoration: underline; }
</style>
