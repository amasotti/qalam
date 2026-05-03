<script lang="ts">
const BADGE_CONFIG: Record<string, { label: string; cls: string }> = {
	VOCABULARY: { label: 'V', cls: 'badge-vocab' },
	GRAMMAR: { label: 'G', cls: 'badge-grammar' },
	CULTURAL: { label: 'C', cls: 'badge-cultural' },
	OTHER: { label: 'O', cls: 'badge-other' },
};

interface Props {
	type: string;
}

let { type }: Props = $props();

const cfg = $derived(BADGE_CONFIG[type] ?? BADGE_CONFIG.OTHER);
</script>

<span class="annotation-badge {cfg.cls}" title={type.toLowerCase()}>{cfg.label}</span>
