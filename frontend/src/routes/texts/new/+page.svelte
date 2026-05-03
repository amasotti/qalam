<script lang="ts">
import { ChevronLeft } from 'lucide-svelte';
import { goto } from '$app/navigation';
import type { CreateTextRequest } from '$lib/api/types.gen';
import TextForm from '$lib/components/texts/TextForm.svelte';
import { useCreateText } from '$lib/stores/texts';

const createText = useCreateText();

async function handleSubmit(req: CreateTextRequest) {
	const created = await createText.mutateAsync(req);
	goto(`/texts/${created.id}`);
}
</script>

<div class="form-page">
	<a href="/texts" class="btn section-block">← Back to Texts</a>

	<header class="form-page-header">
		<h1 class="form-page-title">New text</h1>
	</header>

	<TextForm
		isPending={createText.isPending}
		onSubmit={(req) => handleSubmit(req as CreateTextRequest)}
		onCancel={() => goto('/texts')}
	/>
</div>
