<script lang="ts">
import type { TextResponse } from '$lib/api/types.gen';

interface Props {
	text: TextResponse;
}

let { text }: Props = $props();
</script>

{#if text.body || text.transliteration}
	<div class="full-text-panel">
		<h2 class="full-text-heading">Full text</h2>

		{#if text.body}
			<div class="full-text-ar arabic-text">{text.body}</div>
		{/if}

		{#if text.transliteration}
			<div class="full-text-tr transliteration">{text.transliteration}</div>
		{/if}

		{#if text.translation}
			<div class="full-text-en">{text.translation}</div>
		{/if}
	</div>
{/if}
