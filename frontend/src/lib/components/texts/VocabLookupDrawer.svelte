<script lang="ts">
import { untrack } from 'svelte';
import { fly } from 'svelte/transition';
import type { AlignmentTokenResponse, WordResponse } from '$lib/api/types.gen';
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

let searchText = $state('');
let found = $state<WordResponse | null>(null);
let notFound = $state(false);
let loading = $state(false);

function doLookup(arabic: string) {
	found = null;
	notFound = false;
	loading = true;
	lookup.mutateAsync(arabic).then((word) => {
		found = word;
		notFound = word === null;
		loading = false;
	});
}

$effect(() => {
	if (!open || !token) return;
	const arabic = token.arabic;
	untrack(() => {
		searchText = arabic;
		doLookup(arabic);
	});
});

function handleBlur() {
	const q = searchText.trim();
	if (q) doLookup(q);
}

function handleAnnotate() {
	if (!token) return;
	onannotate(token.arabic);
	onclose();
}

function handleCreated(_wordId: string) {
	loading = true;
	notFound = false;
	lookup.mutateAsync(searchText).then((word) => {
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
    class="drawer-backdrop"
    onclick={onclose}
    onkeydown={(e) => e.key === 'Enter' && onclose()}
    role="button"
    aria-label="Close"
    tabindex="-1"
  ></div>

  <aside class="drawer" style="width:340px" transition:fly={{ x: 360, duration: 220, opacity: 1 }}>
    <header class="drawer-header">
      <input
        class="vocab-header-input arabic-text"
        bind:value={searchText}
        onblur={handleBlur}
        aria-label="Lookup word"
        dir="rtl"
      />
      <button class="drawer-close" onclick={onclose} aria-label="Close">×</button>
    </header>

    <div class="drawer-body">
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
          <span class="vocab-mastery-badge mastery-{found.masteryLevel.toLowerCase()}">
            {found.masteryLevel}
          </span>
          <a href="/words/{found.id}" class="vocab-open-link">Open in vocabulary →</a>
        </div>
      {:else if notFound}
        <p class="vocab-state-msg vocab-not-found-msg">Not in vocabulary yet.</p>
        <QuickAddWordForm
          arabicText={searchText}
          onCreated={handleCreated}
          onCancel={onclose}
        />
      {/if}
    </div>

    <footer class="drawer-footer">
      <button class="vocab-annotate-btn" onclick={handleAnnotate}>
        {found ? 'View / Add Annotations →' : 'Annotate this token →'}
      </button>
    </footer>
  </aside>
{/if}

<style>
.vocab-header-input {
  font-size: 1.5rem;
  line-height: 1.4;
  flex: 1;
  min-width: 0;
  background: transparent;
  border: none;
  border-bottom: 1px dashed transparent;
  outline: none;
  color: hsl(var(--foreground));
  font-family: inherit;
  padding: 0;
}
.vocab-header-input:focus {
  border-bottom-color: hsl(var(--primary) / 0.5);
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
.vocab-open-link {
  font-size: 0.8125rem;
  color: hsl(var(--primary));
  text-decoration: none;
  margin-top: 0.25rem;
}
.vocab-open-link:hover { text-decoration: underline; }
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
