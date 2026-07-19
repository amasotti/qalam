<script lang="ts">
import type { WordAutocompleteResponse } from '$lib/api/types.gen';
import { useWordAutocomplete } from '$lib/stores/words';

interface Props {
	onselect: (word: WordAutocompleteResponse) => void;
}

const { onselect }: Props = $props();

let query = $state('');
let open = $state(false);
const autocomplete = useWordAutocomplete(
	() => query,
	() => 'VERB'
);

function handleInput(event: Event) {
	query = (event.currentTarget as HTMLInputElement).value;
	open = query.trim().length >= 2;
}

function select(word: WordAutocompleteResponse) {
	query = '';
	open = false;
	onselect(word);
}
</script>

<div class="conj-picker">
	<label class="conj-adhoc-label" for="conj-verb-search">Your verbs</label>
	<div class="conj-picker-wrap">
		<input
			id="conj-verb-search"
			class="conj-adhoc-input arabic"
			dir="rtl"
			placeholder="ابحث عن فعل…"
			value={query}
			oninput={handleInput}
			onfocus={() => (open = query.trim().length >= 2)}
			onblur={() => setTimeout(() => (open = false), 150)}
			autocomplete="off"
		/>
		{#if open && (autocomplete.data ?? []).length > 0}
			<ul class="conj-picker-results" role="listbox">
				{#each autocomplete.data ?? [] as word (word.id)}
					<li role="option" aria-selected="false">
						<button type="button" class="conj-picker-option" onmousedown={() => select(word)}>
							<span class="arabic" dir="rtl">{word.arabicText}</span>
							{#if word.translation}<span>{word.translation}</span>{/if}
						</button>
					</li>
				{/each}
			</ul>
		{:else if open && !autocomplete.isPending}
			<p class="conj-picker-empty">No saved verbs found.</p>
		{/if}
	</div>
	<p class="conj-adhoc-hint">Search Arabic, translation, or transliteration.</p>
</div>
