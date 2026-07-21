<script lang="ts">
import { LayoutGrid, Table2 } from '@lucide/svelte';

let {
	view,
	onchange,
}: {
	view: 'grid' | 'table';
	onchange: (v: 'grid' | 'table') => void;
} = $props();
</script>

<div class="view-toggle" role="group" aria-label="View mode">
	<button
		class="view-toggle-btn"
		class:active={view === 'grid'}
		onclick={() => onchange('grid')}
		aria-label="Grid view"
		title="Grid view"
		aria-pressed={view === 'grid'}
	>
		<LayoutGrid size={14} />
	</button>
	<button
		class="view-toggle-btn"
		class:active={view === 'table'}
		onclick={() => onchange('table')}
		aria-label="Table view"
		title="Table view"
		aria-pressed={view === 'table'}
	>
		<Table2 size={14} />
	</button>
</div>
