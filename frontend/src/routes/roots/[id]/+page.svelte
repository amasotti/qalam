<script lang="ts">
import { goto } from '$app/navigation';
import { page } from '$app/state';
import type { CreateRootRequest, UpdateRootRequest } from '$lib/api/types.gen';
import Markdown from '$lib/components/Markdown.svelte';
import RootForm from '$lib/components/roots/RootForm.svelte';
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

function formatEnum(value: string): string {
	return value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();
}
</script>

{#if root.isPending}
	<p class="detail-status status-text status-text-muted">Loading…</p>
{:else if root.isError}
	<p class="detail-status status-text status-text-danger">Root not found.</p>
{:else if root.data}
	{#if isEditing}
		<!-- ── Edit mode ── -->
		<div class="detail-edit-content">
			<div class="section-block">
				<div class="root-letter-ar arabic-display">{root.data.displayForm}</div>
				<div class="status-text status-text-muted">{root.data.normalizedForm}</div>
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
		</div>
	{:else}
		<!-- ── View mode ── -->
		<nav class="breadcrumb">
			<a href="/roots">Roots</a>
			<span class="bc-sep">/</span>
			<span class="bc-cur">{root.data.displayForm}</span>
		</nav>

		<div class="detail-layout detail-layout-root">
			<div class="detail-content">
				<!-- Hero -->
				<div class="root-hero">
					<div class="root-hero-ghost">{root.data.displayForm}</div>
					<div>
						<div class="row-rtl-end section-block-sm">
							<span class="root-letter-ar">{root.data.displayForm}</span>
						</div>
						<div class="root-hero-meta">
							<span class="root-normalized">{root.data.normalizedForm}</span>
							<span class="root-badge">{root.data.letterCount} letters</span>
						</div>
					</div>
					<div class="root-hero-actions">
						<button class="btn btn-primary" onclick={() => { isEditing = true; deleteConfirm = false; }}>Edit</button>
						<button class="btn btn-danger" onclick={handleDelete} disabled={deleteRoot.isPending}>
							{deleteConfirm ? 'Confirm delete' : 'Delete'}
						</button>
					</div>
				</div>

				<!-- Core meaning -->
				<div class="sect-label">Core meaning</div>
				{#if root.data.meaning}
					<p class="root-meaning">{root.data.meaning}</p>
				{:else}
					<p class="annot-empty section-block-lg">No meaning recorded</p>
				{/if}

				<!-- Analysis -->
				<div class="sect-label">Analysis</div>
				<div class="root-analysis">
					{#if root.data.analysis}
						<Markdown content={root.data.analysis} />
					{:else}
						<p class="annot-empty">No analysis recorded</p>
					{/if}
				</div>

				<!-- Word family -->
				<div class="sect-label">
					Word family
					{#if words.data !== undefined}
						<span class="meta-inline-count">{words.data.length} words</span>
					{/if}
				</div>
				{#if words.isPending}
					<p class="status-text status-text-muted">Loading words…</p>
				{:else if words.isError}
					<p class="status-text status-text-danger">Could not load words.</p>
				{:else if words.data && words.data.length > 0}
					<div class="word-family-grid">
						{#each words.data as word (word.id)}
							{@const masteryClass = 'wc-' + (word.masteryLevel?.toLowerCase() ?? 'new')}
							<a class="root-word-chip {masteryClass}" href="/words/{word.id}">
								<span class="wc-ar">{word.arabicText}</span>
								{#if word.translation}
									<span class="wc-en">{word.translation}</span>
								{/if}
								{#if word.partOfSpeech}
									<span class="wc-pos pos-{word.partOfSpeech.toLowerCase() === 'verb' ? 'verb' : word.partOfSpeech.toLowerCase() === 'noun' ? 'noun' : word.partOfSpeech.toLowerCase() === 'adjective' ? 'adj' : 'other'}">{formatEnum(word.partOfSpeech)}</span>
								{/if}
								{#if word.masteryLevel}
									<span class="wc-mastery wc-mastery-{word.masteryLevel.toLowerCase()}">{formatEnum(word.masteryLevel)}</span>
								{/if}
							</a>
						{/each}
					</div>
				{:else}
					<p class="annot-empty">No words linked to this root yet.</p>
				{/if}
			</div>

			<!-- Sidebar -->
			<aside class="detail-sidebar">
				<!-- Root details card -->
				<div class="meta-card">
					<div class="meta-card-title">Root details</div>
					<div class="meta-row">
						<span class="meta-key">Display form</span>
						<span class="meta-val ar">{root.data.displayForm}</span>
					</div>
					<div class="meta-row">
						<span class="meta-key">Normalized</span>
						<span class="meta-val">{root.data.normalizedForm}</span>
					</div>
					<div class="meta-row">
						<span class="meta-key">Letter count</span>
						<span class="meta-val">{root.data.letterCount} ({root.data.letterCount === 3 ? 'trilateral' : root.data.letterCount === 4 ? 'quadrilateral' : 'other'})</span>
					</div>
					{#if words.data !== undefined}
						<div class="meta-row">
							<span class="meta-key">Word family</span>
							<span class="meta-val">{words.data.length} words</span>
						</div>
					{/if}
				</div>

				<!-- Family mastery card -->
				{#if words.data && words.data.length > 0}
					{@const total = words.data.length}
					{@const masteryCount = Object.fromEntries(
						['MASTERED', 'KNOWN', 'FAMILIAR', 'LEARNING', 'NEW'].map(m => [
							m, (words.data ?? []).filter(w => w.masteryLevel === m).length
						])
					)}
					<div class="meta-card">
						<div class="meta-card-title">Family mastery</div>
						<div class="fam-mastery">
							{#each [['MASTERED','mastered'],['KNOWN','known'],['FAMILIAR','familiar'],['LEARNING','learning'],['NEW','new']] as [level, cls]}
								<div class="fam-row">
									<span class="fam-dot fam-dot-{cls}"></span>
									<span class="fam-label">{formatEnum(level)}</span>
									<div class="fam-bar-outer">
										<div class="fam-bar-inner fam-bar-inner-{cls}" style="width:{total > 0 ? Math.round(masteryCount[level] / total * 100) : 0}%;"></div>
									</div>
									<span class="fam-count">{masteryCount[level]}</span>
								</div>
							{/each}
						</div>
					</div>

					<!-- Key words card -->
					<div class="meta-card">
						<div class="meta-card-title">Key words</div>
						<div class="quick-links">
							{#each words.data.slice(0, 3) as word (word.id)}
								<a href="/words/{word.id}" class="ql">
									<span class="ql-ar">{word.arabicText}</span>
									<span class="ql-en">{word.translation ?? ''} →</span>
								</a>
							{/each}
						</div>
					</div>
				{/if}

				<!-- Actions card -->
				<div class="meta-card">
					<div class="meta-card-title">Actions</div>
					<div class="meta-actions">
						<a href="/words/new" class="btn btn-full">+ Add word to family</a>
						<a href="/training" class="btn btn-full">Practice this root</a>
					</div>
				</div>
			</aside>
		</div>
	{/if}
{/if}
