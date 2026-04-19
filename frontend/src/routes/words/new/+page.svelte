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

<div class="page-create-word page-enter">
	<a class="word-detail-back" href="/words">
		<ChevronLeft size={14} />
		Words
	</a>

	<header class="create-word-header">
		<h1 class="create-word-title">New word</h1>
		<p class="create-word-subtitle">Add a word to your vocabulary</p>
	</header>

	<WordForm
		isPending={createWord.isPending}
		onSubmit={handleSubmit}
		onCancel={() => goto('/words')}
	/>
</div>
