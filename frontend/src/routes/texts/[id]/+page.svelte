<script lang="ts">
import { ChevronLeft, Pencil, Trash2, X } from 'lucide-svelte';
import { goto } from '$app/navigation';
import { page } from '$app/state';
import type { UpdateTextRequest } from '$lib/api/types.gen';
import { Badge } from '$lib/components/ui/badge';
import { Button } from '$lib/components/ui/button';
import FullTextPanel from '$lib/components/texts/FullTextPanel.svelte';
import InterlinearSentence from '$lib/components/texts/InterlinearSentence.svelte';
import SentenceEditor from '$lib/components/texts/SentenceEditor.svelte';
import TextForm from '$lib/components/texts/TextForm.svelte';
import {
	useAutoTokenize,
	useDeleteText,
	useMarkTokensValid,
	useSentences,
	useText,
	useUpdateText,
} from '$lib/stores/texts';

const id = $derived(page.params.id ?? '');
const text = useText(() => id);
const sentences = useSentences(() => id);
const updateText = useUpdateText();
const deleteText = useDeleteText();
const autoTokenize = useAutoTokenize();
const markValid = useMarkTokensValid();

let isEditing = $state(false);
let deleteConfirm = $state(false);

async function handleUpdate(req: UpdateTextRequest) {
	await updateText.mutateAsync({ id, body: req });
	isEditing = false;
}

async function handleDelete() {
	if (!deleteConfirm) {
		deleteConfirm = true;
		setTimeout(() => (deleteConfirm = false), 3000);
		return;
	}
	await deleteText.mutateAsync(id);
	goto('/texts');
}

function formatEnum(value: string): string {
	return value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();
}
</script>

<div class="page-text-detail page-enter">
	<a class="text-detail-back" href="/texts">
		<ChevronLeft size={14} />
		Texts
	</a>

	{#if text.isPending}
		<p class="text-detail-loading">Loading…</p>
	{:else if text.isError}
		<p class="text-detail-error">Text not found.</p>
	{:else if text.data}
		{#if isEditing}
			<!-- ── Edit text metadata ── -->
			<div class="text-detail-section">
				<div class="text-edit-header">
					<h2 class="text-edit-heading">Edit text</h2>
					<Button variant="ghost" size="sm" onclick={() => (isEditing = false)}>
						<X size={14} />
						Cancel
					</Button>
				</div>
				<TextForm
					isEdit
					isPending={updateText.isPending}
					initial={{
						title: text.data.title,
						body: text.data.body,
						transliteration: text.data.transliteration ?? null,
						translation: text.data.translation ?? null,
						dialect: text.data.dialect,
						difficulty: text.data.difficulty,
						comments: text.data.comments ?? null,
						tags: text.data.tags,
					}}
					onSubmit={(req) => handleUpdate(req as UpdateTextRequest)}
					onCancel={() => (isEditing = false)}
				/>
			</div>
		{:else}
			<!-- ── Display mode ── -->
			<header class="text-detail-header">
				<div class="text-detail-title-row">
					<h1 class="text-detail-title">{text.data.title}</h1>
					<div class="text-detail-actions">
						<Button variant="outline" size="sm" onclick={() => (isEditing = true)}>
							<Pencil size={14} />
							Edit
						</Button>
						<Button
							variant="outline"
							size="sm"
							class="btn-outline-danger"
							disabled={deleteText.isPending}
							onclick={handleDelete}
						>
							<Trash2 size={14} />
							{deleteConfirm ? 'Confirm?' : 'Delete'}
						</Button>
					</div>
				</div>
				<div class="text-detail-badges">
					<Badge class="difficulty-{text.data.difficulty.toLowerCase()}">
						{formatEnum(text.data.difficulty)}
					</Badge>
					<Badge class="dialect-{text.data.dialect.toLowerCase()}">
						{text.data.dialect}
					</Badge>
					{#each text.data.tags as tag}
						<Badge variant="outline">{tag}</Badge>
					{/each}
				</div>
				{#if text.data.comments}
					<p class="text-detail-comments">{text.data.comments}</p>
				{/if}
			</header>
		{/if}

		<!-- ── Sentences section ── -->
		<div class="text-detail-section">
			<div class="text-section-header">
				<h2 class="text-section-heading">Interlinear</h2>
			</div>

			{#if sentences.isPending}
				<p class="text-detail-loading">Loading sentences…</p>
			{:else if sentences.isError}
				<p class="text-detail-error">Could not load sentences.</p>
			{:else if !isEditing}
				<!-- Read-only interlinear view -->
				{#if (sentences.data ?? []).length === 0}
					<p class="text-detail-empty">
						No sentences yet. Switch to edit mode to add sentences.
					</p>
				{:else}
					<div class="interlinear-list">
						{#each sentences.data ?? [] as sentence (sentence.id)}
							<InterlinearSentence
								{sentence}
								onRetokenize={async (s) => { await autoTokenize.mutateAsync({ textId: id, id: s.id }); }}
								onMarkValid={async (s) => { await markValid.mutateAsync({ textId: id, id: s.id, currentTokens: s.tokens }); }}
							/>
						{/each}
					</div>
				{/if}
			{:else}
				<!-- Edit mode — sentence editor -->
				<SentenceEditor sentences={sentences.data ?? []} textId={id} />
			{/if}
		</div>

		<!-- ── Full text panel ── -->
		{#if !isEditing}
			<FullTextPanel text={text.data} />
		{/if}
	{/if}
</div>

<style>
.page-text-detail {
	max-width: 800px;
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

.text-detail-loading {
	font-size: 0.875rem;
	color: hsl(var(--muted-foreground));
}

.text-detail-error {
	font-size: 0.875rem;
	color: hsl(var(--destructive));
}

.text-detail-header {
	margin-bottom: 2rem;
}

.text-detail-title-row {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 1rem;
	margin-bottom: 0.75rem;
}

.text-detail-title {
	font-size: 1.5rem;
	font-weight: 700;
	letter-spacing: -0.02em;
	line-height: 1.3;
}

.text-detail-actions {
	display: flex;
	gap: 0.375rem;
	flex-shrink: 0;
}

.text-detail-badges {
	display: flex;
	flex-wrap: wrap;
	gap: 0.25rem;
	margin-bottom: 0.75rem;
}

.text-detail-comments {
	font-size: 0.875rem;
	color: hsl(var(--muted-foreground));
	line-height: 1.6;
}

.text-detail-section {
	margin-bottom: 2rem;
}

.text-section-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 1rem;
}

.text-section-heading {
	font-size: 0.8125rem;
	font-weight: 600;
	text-transform: uppercase;
	letter-spacing: 0.06em;
	color: hsl(var(--muted-foreground));
}

.interlinear-list {
	display: flex;
	flex-direction: column;
}

.text-detail-empty {
	font-size: 0.875rem;
	color: hsl(var(--muted-foreground));
	padding: 2rem 0;
	text-align: center;
}

.text-edit-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 1rem;
}

.text-edit-heading {
	font-size: 1rem;
	font-weight: 600;
}
</style>
