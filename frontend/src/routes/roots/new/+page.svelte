<script lang="ts">
import { ChevronLeft } from 'lucide-svelte';
import { goto } from '$app/navigation';
import type { CreateRootRequest, UpdateRootRequest } from '$lib/api/types.gen';
import RootForm from '$lib/components/roots/RootForm.svelte';
import { useCreateRoot } from '$lib/stores/roots';

const createRoot = useCreateRoot();

async function handleSubmit(req: CreateRootRequest | UpdateRootRequest) {
	const created = await createRoot.mutateAsync(req as CreateRootRequest);
	goto(`/roots/${created.id}`);
}
</script>

<div class="page-create-root page-enter">
	<a class="root-detail-back" href="/roots">
		<ChevronLeft size={14} />
		Roots
	</a>

	<header class="create-root-header">
		<h1 class="create-root-title">New root</h1>
		<p class="create-root-subtitle">
			Enter the Arabic consonants — normalization preview updates as you type.
		</p>
	</header>

	<RootForm
		isPending={createRoot.isPending}
		onSubmit={handleSubmit}
		onCancel={() => goto('/roots')}
	/>
</div>
