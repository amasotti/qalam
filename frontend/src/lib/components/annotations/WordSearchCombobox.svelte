<script lang="ts">
import type { WordAutocompleteResponse } from '$lib/api/types.gen';
import { useWordAutocomplete } from '$lib/stores/words';

interface Props {
	selectedWords: WordAutocompleteResponse[];
	onchange: (words: WordAutocompleteResponse[]) => void;
}

let { selectedWords, onchange }: Props = $props();

let q = $state('');
let open = $state(false);

const autocomplete = useWordAutocomplete(() => q);

function select(word: WordAutocompleteResponse) {
	if (!selectedWords.some((w) => w.id === word.id)) {
		onchange([...selectedWords, word]);
	}
	q = '';
	open = false;
}

function remove(wordId: string) {
	onchange(selectedWords.filter((w) => w.id !== wordId));
}

function handleInput(e: Event) {
	q = (e.target as HTMLInputElement).value;
	open = q.length >= 2;
}
</script>

<div class="word-search">
	{#if selectedWords.length > 0}
		<div class="word-chips">
			{#each selectedWords as word (word.id)}
				<span class="word-chip">
					<span class="word-chip-arabic arabic-text">{word.arabicText}</span>
					{#if word.translation}
						<span class="word-chip-translation">{word.translation}</span>
					{/if}
					<button
						type="button"
						class="word-chip-remove"
						onclick={() => remove(word.id)}
						aria-label="Remove {word.arabicText}"
					>×</button>
				</span>
			{/each}
		</div>
	{/if}

	<div class="word-search-wrap">
		<input
			type="text"
			value={q}
			oninput={handleInput}
			onfocus={() => { if (q.length >= 2) open = true; }}
			onblur={() => setTimeout(() => (open = false), 150)}
			placeholder="Search Arabic or translation…"
			class="word-search-input"
		/>
		{#if open && (autocomplete.data ?? []).length > 0}
			<ul class="word-dropdown" role="listbox">
				{#each autocomplete.data ?? [] as word (word.id)}
					<li role="option" aria-selected="false">
						<button
							type="button"
							class="word-dropdown-option"
							onmousedown={() => select(word)}
						>
							<span class="arabic-text">{word.arabicText}</span>
							{#if word.translation}
								<span class="word-dropdown-translation">{word.translation}</span>
							{/if}
						</button>
					</li>
				{/each}
			</ul>
		{/if}
	</div>
</div>
