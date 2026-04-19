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

<div class="page-create-text page-enter">
	<a class="text-detail-back" href="/texts">
		<ChevronLeft size={14} />
		Texts
	</a>

	<header class="create-text-header">
		<h1 class="create-text-title">New text</h1>
	</header>

	<TextForm
		isPending={createText.isPending}
		onSubmit={(req) => handleSubmit(req as CreateTextRequest)}
		onCancel={() => goto('/texts')}
	/>
</div>

<style>
.page-create-text {
	max-width: 760px;
	margin: 0 auto;
	padding: 2rem 1.5rem;
}

.text-detail-back {
	display: inline-flex;
	align-items: center;
	gap: 0.25rem;
	font-size: 0.8125rem;
	color: hsl(var(--muted-foreground));
	text-decoration: none;
	margin-bottom: 1.25rem;
}

.text-detail-back:hover {
	color: hsl(var(--foreground));
}

.create-text-header {
	margin-bottom: 1.5rem;
}

.create-text-title {
	font-size: 1.375rem;
	font-weight: 600;
	letter-spacing: -0.01em;
}
</style>
