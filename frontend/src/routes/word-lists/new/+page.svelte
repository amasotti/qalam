<script lang="ts">
import { goto } from '$app/navigation';
import { useCreateWordList } from '$lib/stores/wordLists';

const createList = useCreateWordList();

let title = $state('');
let description = $state('');

async function handleSubmit(e: SubmitEvent) {
	e.preventDefault();
	if (!title.trim()) return;
	const created = await createList.mutateAsync({
		title: title.trim(),
		description: description.trim() || null,
	});
	goto(`/word-lists/${created.id}`);
}
</script>

<div class="form-page">
	<a href="/word-lists" class="btn section-block">← Back to Lists</a>

	<header class="form-page-header">
		<h1 class="form-page-title">New list</h1>
		<p class="form-page-subtitle">
			Group words by theme — colors, family, kitchen. Add words after creating it.
		</p>
	</header>

	<form class="form-shell" onsubmit={handleSubmit}>
		<div class="form-field">
			<label class="form-label" for="wl-title">Title</label>
			<input
				id="wl-title"
				class="form-input"
				type="text"
				placeholder="e.g. Colors"
				bind:value={title}
				required
			/>
		</div>

		<div class="form-field">
			<label class="form-label" for="wl-desc">Description</label>
			<p class="form-hint">Optional — what ties these words together.</p>
			<textarea
				id="wl-desc"
				class="form-input"
				rows="3"
				placeholder="What ties these words together?"
				bind:value={description}
			></textarea>
		</div>

		<div class="form-actions">
			<button type="button" class="btn" onclick={() => goto('/word-lists')}>Cancel</button>
			<button
				type="submit"
				class="btn btn-primary"
				disabled={createList.isPending || !title.trim()}
			>
				{createList.isPending ? 'Creating…' : 'Create list'}
			</button>
		</div>
	</form>
</div>
