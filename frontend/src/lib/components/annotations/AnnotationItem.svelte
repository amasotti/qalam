<script lang="ts">
import { Pencil, Trash2 } from 'lucide-svelte';
import type { AnnotationResponse } from '$lib/api/types.gen';
import AnnotationBadge from './AnnotationBadge.svelte';

interface Props {
	annotation: AnnotationResponse;
	textId: string;
	onEdit: (annotation: AnnotationResponse) => void;
	onDelete: (id: string) => void;
}

let { annotation, textId, onEdit, onDelete }: Props = $props();

let deleteConfirm = $state(false);

function handleDelete() {
	if (!deleteConfirm) {
		deleteConfirm = true;
		setTimeout(() => (deleteConfirm = false), 3000);
		return;
	}
	onDelete(annotation.id);
}
</script>

<div class="annotation-item">
	<div class="item-header">
		<AnnotationBadge type={annotation.type} />
		<span class="item-type">{annotation.type.charAt(0) + annotation.type.slice(1).toLowerCase()}</span>
		<div class="item-actions">
			<button class="item-btn" onclick={() => onEdit(annotation)} aria-label="Edit">
				<Pencil size={11} />
			</button>
			<button
				class="item-btn"
				class:item-btn-danger={deleteConfirm}
				onclick={handleDelete}
				aria-label="Delete"
			>
				<Trash2 size={11} />
			</button>
		</div>
	</div>

	{#if annotation.content}
		<p class="item-content">{annotation.content}</p>
	{/if}

	{#if annotation.linkedWordIds.length > 0}
		<div class="item-words">
			{#each annotation.linkedWordIds as wordId (wordId)}
				<a href="/words/{wordId}" class="item-word-link">→ word</a>
			{/each}
		</div>
	{/if}
</div>

<style>
.annotation-item {
	padding: 0.625rem 0;
	border-bottom: 1px solid hsl(var(--border) / 0.4);
	display: flex;
	flex-direction: column;
	gap: 0.375rem;
}
.annotation-item:last-of-type { border-bottom: none; }

.item-header {
	display: flex;
	align-items: center;
	gap: 0.375rem;
}

.item-type {
	font-size: 0.7rem;
	font-weight: 600;
	text-transform: uppercase;
	letter-spacing: 0.04em;
	color: hsl(var(--muted-foreground));
	flex: 1;
}

.item-actions { display: flex; gap: 0.125rem; margin-left: auto; }

.item-btn {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	width: 1.375rem;
	height: 1.375rem;
	border-radius: 0.25rem;
	background: none;
	border: none;
	cursor: pointer;
	color: hsl(var(--muted-foreground));
}
.item-btn:hover { background: hsl(var(--muted)); color: hsl(var(--foreground)); }
.item-btn-danger { color: hsl(var(--destructive)); }

.item-content {
	font-size: 0.8125rem;
	color: hsl(var(--foreground) / 0.9);
	line-height: 1.5;
	margin: 0;
}

.item-words { display: flex; flex-wrap: wrap; gap: 0.25rem; }

.item-word-link {
	font-size: 0.7rem;
	color: hsl(var(--primary));
	text-decoration: none;
	padding: 0.125rem 0.375rem;
	border-radius: 0.25rem;
	background: hsl(var(--primary) / 0.08);
}
.item-word-link:hover { background: hsl(var(--primary) / 0.15); }
</style>
