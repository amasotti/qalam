<script lang="ts">
import { useVerbDetails } from '$lib/stores/words';

interface Props {
	wordId: string;
}

const { wordId }: Props = $props();
const verbDetails = useVerbDetails(() => wordId);
</script>

{#if verbDetails.data}
	<div class="morph-strip">
		<span class="morph-tag">Verb</span>
		<span class="chip c-olive">Form {verbDetails.data.verbForm}</span>
		<span class="chip c-muted">{verbDetails.data.weaknessType.toLowerCase().replace('_', ' ')}</span>
		{#if verbDetails.data.pastPattern}<span class="chip c-muted">{verbDetails.data.pastPattern}</span>{/if}
		{#if verbDetails.data.presentPattern}<span class="chip c-muted">{verbDetails.data.presentPattern}</span>{/if}
		<a class="btn btn-xs" href={`/verbs/conjugation?wordId=${wordId}`}>View conjugation</a>
	</div>
{/if}
