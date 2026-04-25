<script lang="ts">
import { ChevronLeft } from 'lucide-svelte';
import { goto } from '$app/navigation';
import type { CreateWordRequest, UpdateWordRequest } from '$lib/api/types.gen';
import WordForm from '$lib/components/words/WordForm.svelte';
import { useCreateWord } from '$lib/stores/words';

const createWord = useCreateWord();

async function handleSubmit(req: CreateWordRequest | UpdateWordRequest) {
	const created = await createWord.mutateAsync(req as CreateWordRequest);
	goto(`/words/${created.id}`);
}
</script>

<div class="form-page">
	<a href="/words" class="btn" style="margin-bottom:1.5rem;">← Back to Words</a>

	<header class="form-page-header">
		<h1 class="form-page-title">New word</h1>
		<p class="form-page-subtitle">Add a word to your vocabulary</p>
	</header>

	<WordForm
		isPending={createWord.isPending}
		onSubmit={handleSubmit}
		onCancel={() => goto('/words')}
	/>
</div>
