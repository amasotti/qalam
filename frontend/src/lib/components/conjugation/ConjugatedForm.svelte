<script lang="ts">
import type { SegmentDto } from '$lib/api/types.gen';

const SEGMENT_CLASS: Record<string, string> = {
	PREFIX: 'seg-prefix',
	ROOT: 'seg-root',
	PATTERN_VOWEL: 'seg-vowel',
	SUFFIX: 'seg-suffix',
};

interface Props {
	arabic: string;
	segments: SegmentDto[];
}

const { arabic, segments }: Props = $props();
</script>

<div class="conj-form-cell">
	<span class="conj-form-word">{arabic}</span>
	<span class="conj-form-segments">{#each segments as seg}<span class={SEGMENT_CLASS[seg.type] ?? ''}>{seg.text}</span>{/each}</span>
</div>
