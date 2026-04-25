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

<style>
.annotation-badge {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	width: 1rem;
	height: 1rem;
	border-radius: 0.25rem;
	font-size: 0.6rem;
	font-weight: 700;
	line-height: 1;
	flex-shrink: 0;
}
.badge-vocab   { background: hsl(150 30% 20%); color: hsl(150 55% 60%); }
.badge-grammar { background: hsl(220 30% 22%); color: hsl(220 55% 65%); }
.badge-cultural{ background: hsl(38 35% 22%);  color: hsl(38  55% 62%); }
.badge-other   { background: hsl(var(--muted)); color: hsl(var(--muted-foreground)); }
</style>
