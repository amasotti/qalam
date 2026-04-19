<script lang="ts">
import { ChevronLeft, Pencil, Trash2 } from 'lucide-svelte';
import { goto } from '$app/navigation';
import { page } from '$app/state';
import type { CreateRootRequest, UpdateRootRequest } from '$lib/api/types.gen';
import Markdown from '$lib/components/Markdown.svelte';
import RootForm from '$lib/components/roots/RootForm.svelte';
import { Badge } from '$lib/components/ui/badge';
import { Button } from '$lib/components/ui/button';
import { useDeleteRoot, useRoot, useUpdateRoot, useWordsForRoot } from '$lib/stores/roots';

const id = $derived(page.params.id ?? '');
const root = useRoot(() => id);
const words = useWordsForRoot(() => id);
const updateRoot = useUpdateRoot();
const deleteRoot = useDeleteRoot();

let isEditing = $state(false);
let deleteConfirm = $state(false);

async function handleUpdate(req: CreateRootRequest | UpdateRootRequest) {
	await updateRoot.mutateAsync({ id, body: req as UpdateRootRequest });
	isEditing = false;
}

async function handleDelete() {
	if (!deleteConfirm) {
		deleteConfirm = true;
		setTimeout(() => {
			deleteConfirm = false;
		}, 3000);
		return;
	}
	await deleteRoot.mutateAsync(id);
	goto('/roots');
}
</script>

<div class="page-root-detail page-enter">
	<a class="root-detail-back" href="/roots">
		<ChevronLeft size={14} />
		Roots
	</a>

	{#if root.isPending}
		<p style="color: hsl(var(--muted-foreground)); font-size: 0.875rem;">Loading…</p>
	{:else if root.isError}
		<p style="color: hsl(var(--destructive)); font-size: 0.875rem;">Root not found.</p>
	{:else if root.data}
		{#if isEditing}
			<!-- ── Edit mode ── -->
			<div class="root-hero">
				<div>
					<div class="root-hero-arabic">{root.data.displayForm}</div>
					<div class="root-hero-normalized">{root.data.normalizedForm}</div>
				</div>
			</div>

			<RootForm
				isEdit
				initial={{
					meaning: root.data.meaning,
					analysis: root.data.analysis,
				}}
				isPending={updateRoot.isPending}
				onSubmit={handleUpdate}
				onCancel={() => (isEditing = false)}
			/>
		{:else}
			<!-- ── View mode ── -->
			<div class="root-hero">
				<div>
					<div class="root-hero-arabic">{root.data.displayForm}</div>
					<div class="root-hero-normalized">{root.data.normalizedForm}</div>
				</div>
				<div class="root-hero-meta">
					<span class="root-hero-badge">{root.data.letterCount} letters</span>
				</div>
				<div class="root-hero-actions">
					<Button
						variant="outline"
						size="sm"
						onclick={() => {
							isEditing = true
							deleteConfirm = false
						}}
					>
						<Pencil size={13} />
						Edit
					</Button>
					<Button
						variant={deleteConfirm ? 'destructive' : 'ghost'}
						size="sm"
						onclick={handleDelete}
						disabled={deleteRoot.isPending}
					>
						<Trash2 size={13} />
						{deleteConfirm ? 'Confirm delete' : 'Delete'}
					</Button>
				</div>
			</div>

			<div class="root-info">
				<div class="root-info-section">
					<span class="root-info-label">Meaning</span>
					{#if root.data.meaning}
						<p class="root-info-value">{root.data.meaning}</p>
					{:else}
						<p class="root-info-value root-info-empty">No meaning recorded</p>
					{/if}
				</div>
				<div class="root-info-section">
					<span class="root-info-label">Analysis</span>
					{#if root.data.analysis}
						<Markdown content={root.data.analysis} />
					{:else}
						<p class="root-info-value root-info-empty">No analysis recorded</p>
					{/if}
				</div>
			</div>

			<!-- ── Word family ── -->
			<div class="root-word-family">
				<div class="root-word-family-header">
					<h2 class="root-word-family-title">Word family</h2>
					{#if words.data !== undefined}
						<span class="root-word-family-count">{words.data.length}</span>
					{/if}
				</div>

				{#if words.isPending}
					<p class="word-family-empty">Loading words…</p>
				{:else if words.isError}
					<p class="word-family-empty">Could not load words.</p>
				{:else if words.data && words.data.length > 0}
					<div class="word-family-grid stagger-children">
						{#each words.data as word (word.id)}
							<a class="word-chip" href="/words/{word.id}">
								<span class="word-chip-arabic">{word.arabicText}</span>
								{#if word.translation}
									<span class="word-chip-translation">{word.translation}</span>
								{/if}
								{#if word.masteryLevel}
									<Badge class="mastery-{word.masteryLevel.toLowerCase()}" variant="outline">
										{word.masteryLevel.toLowerCase()}
									</Badge>
								{/if}
							</a>
						{/each}
					</div>
				{:else}
					<p class="word-family-empty">No words linked to this root yet.</p>
				{/if}
			</div>
		{/if}
	{/if}
</div>
