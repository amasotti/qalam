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

<div class="banner-warning" role="alert">
	<AlertTriangle size={14} />
	<span class="banner-text">
		Arabic text changed — existing tokens may be stale.
	</span>
	<div class="banner-actions">
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
