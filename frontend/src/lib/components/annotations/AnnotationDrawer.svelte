<script lang="ts">
import { fly } from 'svelte/transition';
import type { AnnotationResponse } from '$lib/api/types.gen';
import { useDeleteAnnotation } from '$lib/stores/annotations';
import AnnotationForm from './AnnotationForm.svelte';
import AnnotationItem from './AnnotationItem.svelte';

interface Props {
	open: boolean;
	anchor: string;
	textId: string;
	annotations: AnnotationResponse[];
	onclose: () => void;
}

let { open, anchor, textId, annotations, onclose }: Props = $props();

const anchorAnnotations = $derived(annotations.filter((a) => a.anchor === anchor));
let editingId = $state<string | null>(null);
let showForm = $state(false);

const deleteAnnotation = useDeleteAnnotation();

$effect(() => {
	if (open) {
		editingId = null;
		showForm = anchorAnnotations.length === 0;
	}
});

function handleFormSuccess() {
	editingId = null;
	showForm = false;
}

async function handleDelete(id: string) {
	await deleteAnnotation.mutateAsync({ textId, id });
	if (anchorAnnotations.length <= 1) onclose();
}
</script>

{#if open}
	<div
		class="drawer-backdrop"
		role="button"
		tabindex="-1"
		aria-label="Close"
		onclick={onclose}
		onkeydown={(e) => e.key === 'Escape' && onclose()}
	></div>

	<aside
		class="annotation-drawer"
		transition:fly={{ x: 360, duration: 220, opacity: 1 }}
		aria-label="Annotations"
	>
		<div class="drawer-header">
			<span class="drawer-anchor arabic-text">{anchor}</span>
			<button class="drawer-close" onclick={onclose} aria-label="Close">×</button>
		</div>

		<div class="drawer-body">
			{#each anchorAnnotations as ann (ann.id)}
				{#if editingId === ann.id}
					<AnnotationForm
						{textId}
						{anchor}
						initial={ann}
						onSuccess={handleFormSuccess}
						onCancel={() => (editingId = null)}
					/>
				{:else}
					<AnnotationItem
						annotation={ann}
						{textId}
						onEdit={(a) => { editingId = a.id; showForm = false; }}
						onDelete={handleDelete}
					/>
				{/if}
			{/each}

			{#if showForm && editingId === null}
				<div class="drawer-form-section">
					<AnnotationForm
						{textId}
						{anchor}
						onSuccess={handleFormSuccess}
						onCancel={anchorAnnotations.length > 0 ? () => (showForm = false) : onclose}
					/>
				</div>
			{:else if editingId === null}
				<button class="drawer-add-btn" onclick={() => (showForm = true)}>
					+ Add annotation
				</button>
			{/if}
		</div>
	</aside>
{/if}

<style>
.drawer-backdrop {
	position: fixed;
	inset: 0;
	background: hsl(0 0% 0% / 0.35);
	z-index: 40;
	cursor: default;
}

.annotation-drawer {
	position: fixed;
	top: 0;
	right: 0;
	bottom: 0;
	width: 360px;
	background: hsl(var(--background));
	border-left: 1px solid hsl(var(--border));
	z-index: 50;
	display: flex;
	flex-direction: column;
	overflow: hidden;
}

.drawer-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 1rem 1.25rem 0.875rem;
	border-bottom: 1px solid hsl(var(--border) / 0.6);
	gap: 0.75rem;
	flex-shrink: 0;
}

.drawer-anchor {
	font-size: 1.5rem;
	line-height: 1.6;
	color: hsl(var(--foreground));
}

.drawer-close {
	flex-shrink: 0;
	width: 1.75rem;
	height: 1.75rem;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 0.375rem;
	background: none;
	border: none;
	cursor: pointer;
	font-size: 1.25rem;
	line-height: 1;
	color: hsl(var(--muted-foreground));
}
.drawer-close:hover { background: hsl(var(--muted)); color: hsl(var(--foreground)); }

.drawer-body {
	flex: 1;
	overflow-y: auto;
	padding: 0.875rem 1.25rem;
	display: flex;
	flex-direction: column;
	gap: 0;
}

.drawer-form-section {
	padding-top: 0.5rem;
	border-top: 1px solid hsl(var(--border) / 0.4);
	margin-top: 0.5rem;
}

.drawer-add-btn {
	margin-top: 0.75rem;
	display: inline-flex;
	align-items: center;
	gap: 0.25rem;
	font-size: 0.8125rem;
	color: hsl(var(--primary));
	background: none;
	border: 1px dashed hsl(var(--primary) / 0.4);
	border-radius: 0.375rem;
	padding: 0.375rem 0.75rem;
	cursor: pointer;
	width: 100%;
	justify-content: center;
}
.drawer-add-btn:hover { background: hsl(var(--primary) / 0.06); }
</style>
