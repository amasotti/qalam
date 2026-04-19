<script lang="ts">
import { ChevronLeft, Pencil, Settings, Trash2, X } from 'lucide-svelte';
import { goto } from '$app/navigation';
import { page } from '$app/state';
import type { UpdateTextRequest } from '$lib/api/types.gen';
import FullTextPanel from '$lib/components/texts/FullTextPanel.svelte';
import InterlinearSentence from '$lib/components/texts/InterlinearSentence.svelte';
import SentenceEditor from '$lib/components/texts/SentenceEditor.svelte';
import TextForm from '$lib/components/texts/TextForm.svelte';
import { Badge } from '$lib/components/ui/badge';
import { Button } from '$lib/components/ui/button';
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

let editingSentences = $state(false);
let editingInfo = $state(false);
let deleteConfirm = $state(false);

async function handleUpdate(req: UpdateTextRequest) {
	await updateText.mutateAsync({ id, body: req });
	editingInfo = false;
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
		<p class="text-detail-meta">Loading…</p>
	{:else if text.isError}
		<p class="text-detail-meta" style="color: hsl(var(--destructive));">Text not found.</p>
	{:else if text.data}
		<!-- ── Header — always visible ── -->
		<header class="text-detail-header">
			<div class="text-detail-title-row">
				<h1 class="text-detail-title">{text.data.title}</h1>
				<div class="text-detail-actions">
					<Button
						variant={editingSentences ? 'default' : 'outline'}
						size="sm"
						onclick={() => { editingSentences = !editingSentences; editingInfo = false; }}
					>
						<Pencil size={14} />
						{editingSentences ? 'Done' : 'Edit'}
					</Button>
					<Button
						variant="ghost"
						size="sm"
						onclick={() => { editingInfo = !editingInfo; editingSentences = false; }}
						title="Edit text info"
					>
						<Settings size={14} />
					</Button>
					<Button
						variant="ghost"
						size="sm"
						class="btn-outline-danger"
						disabled={deleteText.isPending}
						onclick={handleDelete}
					>
						<Trash2 size={14} />
						{deleteConfirm ? 'Sure?' : ''}
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

		<!-- ── Info edit panel — collapsible ── -->
		{#if editingInfo}
			<div class="text-info-panel">
				<div class="text-info-panel-header">
					<span class="text-info-panel-title">Text info</span>
					<button class="text-info-close" onclick={() => (editingInfo = false)} aria-label="Close">
						<X size={14} />
					</button>
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
					onCancel={() => (editingInfo = false)}
				/>
			</div>
		{/if}

		<!-- ── Interlinear — always primary ── -->
		<section class="interlinear-section">
			{#if sentences.isPending}
				<p class="text-detail-meta">Loading…</p>
			{:else if sentences.isError}
				<p class="text-detail-meta" style="color: hsl(var(--destructive));">Could not load sentences.</p>
			{:else if editingSentences}
				<SentenceEditor sentences={sentences.data ?? []} textId={id} />
			{:else if (sentences.data ?? []).length === 0}
				<div class="interlinear-empty">
					<p>No sentences yet.</p>
					<Button variant="outline" size="sm" onclick={() => (editingSentences = true)}>
						<Pencil size={14} />
						Add sentences
					</Button>
				</div>
			{:else}
				{#each sentences.data ?? [] as sentence (sentence.id)}
					<InterlinearSentence
						{sentence}
						isPending={autoTokenize.isPending || markValid.isPending}
						onRetokenize={async (s) => { await autoTokenize.mutateAsync({ textId: id, id: s.id }); }}
						onMarkValid={async (s) => { await markValid.mutateAsync({ textId: id, id: s.id, currentTokens: s.tokens }); }}
					/>
				{/each}
			{/if}
		</section>

		<!-- ── Full text body — secondary, bottom ── -->
		<FullTextPanel text={text.data} />
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

.text-detail-meta {
	font-size: 0.875rem;
	color: hsl(var(--muted-foreground));
}

.text-detail-header {
	margin-bottom: 1.5rem;
}

.text-detail-title-row {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 1rem;
	margin-bottom: 0.625rem;
}

.text-detail-title {
	font-size: 1.5rem;
	font-weight: 700;
	letter-spacing: -0.02em;
	line-height: 1.3;
}

.text-detail-actions {
	display: flex;
	gap: 0.25rem;
	flex-shrink: 0;
	align-items: center;
}

.text-detail-badges {
	display: flex;
	flex-wrap: wrap;
	gap: 0.25rem;
	margin-bottom: 0.5rem;
}

.text-detail-comments {
	font-size: 0.875rem;
	color: hsl(var(--muted-foreground));
	line-height: 1.6;
}

/* Collapsible info panel */
.text-info-panel {
	border: 1px solid hsl(var(--border));
	border-radius: 0.5rem;
	padding: 1rem;
	margin-bottom: 1.5rem;
	background: hsl(var(--muted) / 0.2);
}

.text-info-panel-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 1rem;
}

.text-info-panel-title {
	font-size: 0.8125rem;
	font-weight: 600;
	text-transform: uppercase;
	letter-spacing: 0.05em;
	color: hsl(var(--muted-foreground));
}

.text-info-close {
	border: none;
	background: none;
	cursor: pointer;
	color: hsl(var(--muted-foreground));
	padding: 0.125rem;
	border-radius: 0.25rem;
	display: flex;
	align-items: center;
}

.text-info-close:hover {
	color: hsl(var(--foreground));
	background: hsl(var(--muted));
}

/* Interlinear — primary content */
.interlinear-section {
	display: flex;
	flex-direction: column;
	min-height: 4rem;
}

.interlinear-empty {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 0.75rem;
	padding: 3rem 0;
	color: hsl(var(--muted-foreground));
	font-size: 0.875rem;
}
</style>
