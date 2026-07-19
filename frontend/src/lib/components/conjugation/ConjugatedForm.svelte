<script lang="ts">
import type { SegmentDto } from '$lib/api/types.gen';

const SEGMENT_CLASS: Record<string, string> = {
	PREFIX: 'seg-prefix',
	ROOT: 'seg-root',
	PATTERN_VOWEL: 'seg-vowel',
	SUFFIX: 'seg-suffix',
};

interface Props {
	segments: SegmentDto[];
}

const { segments }: Props = $props();
</script>

<span class="conj-form" dir="rtl">{#each segments as seg}<span class={SEGMENT_CLASS[seg.type] ?? ''}>{seg.text}</span>{/each}</span>
