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

<div class="form-page">
	<a href="/roots" class="btn" style="margin-bottom:1.5rem;">← Back to Roots</a>

	<header class="form-page-header">
		<h1 class="form-page-title">New root</h1>
		<p class="form-page-subtitle">
			Enter the Arabic consonants — normalization preview updates as you type.
		</p>
	</header>

	<RootForm
		isPending={createRoot.isPending}
		onSubmit={handleSubmit}
		onCancel={() => goto('/roots')}
	/>
</div>
