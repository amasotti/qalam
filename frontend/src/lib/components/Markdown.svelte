<script lang="ts">
import { marked } from 'marked';

interface Props {
	content: string;
	class?: string;
}

let { content, class: className = '' }: Props = $props();

const html = $derived(marked.parse(content) as string);
</script>

<div class="prose {className}">
	{@html html}
</div>
