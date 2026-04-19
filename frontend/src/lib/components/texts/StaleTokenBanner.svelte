<script lang="ts">
import { AlertTriangle } from 'lucide-svelte';
import { Button } from '$lib/components/ui/button';

interface Props {
	onRetokenize: () => Promise<void>;
	onMarkValid: () => Promise<void>;
	isPending?: boolean;
}

let { onRetokenize, onMarkValid, isPending = false }: Props = $props();

let retokenizePending = $state(false);
let markValidPending = $state(false);

async function handleRetokenize() {
	retokenizePending = true;
	try {
		await onRetokenize();
	} finally {
		retokenizePending = false;
	}
}

async function handleMarkValid() {
	markValidPending = true;
	try {
		await onMarkValid();
	} finally {
		markValidPending = false;
	}
}
</script>

<div class="stale-banner" role="alert">
	<AlertTriangle size={14} class="stale-banner-icon" />
	<span class="stale-banner-text">
		Arabic text changed — existing tokens may be stale.
	</span>
	<div class="stale-banner-actions">
		<Button
			size="sm"
			variant="outline"
			disabled={isPending || retokenizePending || markValidPending}
			onclick={handleRetokenize}
		>
			{retokenizePending ? 'Tokenizing…' : 'Re-tokenize'}
		</Button>
		<Button
			size="sm"
			variant="ghost"
			disabled={isPending || retokenizePending || markValidPending}
			onclick={handleMarkValid}
		>
			{markValidPending ? 'Marking…' : 'Mark as valid'}
		</Button>
	</div>
</div>

<style>
.stale-banner {
	display: flex;
	align-items: center;
	gap: 0.5rem;
	padding: 0.5rem 0.75rem;
	background: hsl(38 92% 95%);
	border: 1px solid hsl(38 80% 80%);
	border-radius: 0.375rem;
	font-size: 0.8125rem;
	color: hsl(38 80% 28%);
	flex-wrap: wrap;
}

:global(.dark) .stale-banner {
	background: hsl(38 60% 12%);
	border-color: hsl(38 60% 28%);
	color: hsl(38 80% 70%);
}

.stale-banner-text {
	flex: 1;
	min-width: 0;
}

.stale-banner-actions {
	display: flex;
	gap: 0.25rem;
}
</style>
