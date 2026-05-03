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

	<aside class="drawer drawer-sm" transition:fly={{ x: 360, duration: 220, opacity: 1 }}>
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
