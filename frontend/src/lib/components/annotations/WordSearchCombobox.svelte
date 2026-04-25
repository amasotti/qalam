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

<style>
.word-search { display: flex; flex-direction: column; gap: 0.5rem; }

.word-chips { display: flex; flex-wrap: wrap; gap: 0.375rem; }

.word-chip {
	display: inline-flex;
	align-items: center;
	gap: 0.25rem;
	padding: 0.125rem 0.5rem;
	border-radius: 0.375rem;
	background: hsl(var(--muted));
	font-size: 0.8125rem;
}

.word-chip-arabic { font-size: 0.9rem; }
.word-chip-translation { font-size: 0.7rem; color: hsl(var(--muted-foreground)); }

.word-chip-remove {
	background: none;
	border: none;
	cursor: pointer;
	color: hsl(var(--muted-foreground));
	padding: 0 0.125rem;
	line-height: 1;
	font-size: 1rem;
}
.word-chip-remove:hover { color: hsl(var(--destructive)); }

.word-search-wrap { position: relative; }

.word-search-input {
	width: 100%;
	padding: 0.375rem 0.625rem;
	background: hsl(var(--muted) / 0.5);
	border: 1px solid hsl(var(--border));
	border-radius: 0.375rem;
	font-size: 0.8125rem;
	color: hsl(var(--foreground));
	outline: none;
	box-sizing: border-box;
}
.word-search-input:focus { border-color: hsl(var(--primary) / 0.6); }

.word-dropdown {
	position: absolute;
	top: calc(100% + 2px);
	left: 0;
	right: 0;
	z-index: 50;
	background: hsl(var(--background));
	border: 1px solid hsl(var(--border));
	border-radius: 0.375rem;
	padding: 0.25rem 0;
	margin: 0;
	list-style: none;
	box-shadow: 0 8px 24px hsl(0 0% 0% / 0.3);
	max-height: 200px;
	overflow-y: auto;
}

.word-dropdown-option {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 0.5rem;
	width: 100%;
	padding: 0.375rem 0.75rem;
	background: none;
	border: none;
	cursor: pointer;
	text-align: right;
	font-size: 0.8125rem;
	color: hsl(var(--foreground));
}
.word-dropdown-option:hover { background: hsl(var(--muted)); }

.word-dropdown-translation { font-size: 0.7rem; color: hsl(var(--muted-foreground)); }
</style>
