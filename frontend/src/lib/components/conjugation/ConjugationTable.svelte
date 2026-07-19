<script lang="ts">
import type { PersonConjugationDto } from '$lib/api/types.gen';
import ConjugatedForm from './ConjugatedForm.svelte';

type PersonCode = PersonConjugationDto['person'];

interface Props {
	title: string;
	forms: PersonConjugationDto[];
}

const { title, forms }: Props = $props();

/**
 * Person metadata: code → Arabic pronoun + transliteration.
 * Ordered: singular → dual → plural, matching standard grammar tables.
 */
const PERSON_ORDER: { code: PersonCode; ar: string; label: string }[] = [
	{ code: '1S', ar: 'أَنَا', label: 'anā' },
	{ code: '2SM', ar: 'أَنْتَ', label: 'anta' },
	{ code: '2SF', ar: 'أَنْتِ', label: 'anti' },
	{ code: '3SM', ar: 'هُوَ', label: 'huwa' },
	{ code: '3SF', ar: 'هِيَ', label: 'hiya' },
	{ code: '2D', ar: 'أَنْتُمَا', label: 'antumā' },
	{ code: '3DM', ar: 'هُمَا', label: 'humā (m.)' },
	{ code: '3DF', ar: 'هُمَا', label: 'humā (f.)' },
	{ code: '1P', ar: 'نَحْنُ', label: 'naḥnu' },
	{ code: '2PM', ar: 'أَنْتُمْ', label: 'antum' },
	{ code: '2PF', ar: 'أَنْتُنَّ', label: 'antunna' },
	{ code: '3PM', ar: 'هُمْ', label: 'hum' },
	{ code: '3PF', ar: 'هُنَّ', label: 'hunna' },
];

/** Indices where number groups change (dual starts at 5, plural at 8) */
const GROUP_BOUNDARIES = new Set([5, 8]);

const formsByCode = $derived(new Map(forms.map((f) => [f.person, f])));
</script>

<div class="conj-table-card">
	<div class="conj-table-head">{title}</div>
	<table class="conj-table">
		<tbody>
			{#each PERSON_ORDER as person, i}
				{@const form = formsByCode.get(person.code)}
				<tr class:conj-group-sep={GROUP_BOUNDARIES.has(i)}>
					<th>
						<span class="conj-table-person-ar">{person.ar}</span>
					</th>
					<td>
						{#if form}
							<ConjugatedForm arabic={form.arabic} segments={form.segments} />
						{:else}
							—
						{/if}
					</td>
				</tr>
			{/each}
		</tbody>
	</table>
</div>
