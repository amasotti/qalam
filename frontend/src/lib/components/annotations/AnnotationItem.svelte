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
