<script lang="ts">
import { createQuery } from '@tanstack/svelte-query';
import {
	ArrowRight,
	BarChart2,
	BookOpen,
	Dumbbell,
	ExternalLink,
	ScrollText,
	Sprout,
} from 'lucide-svelte';

const health = createQuery(() => ({
	queryKey: ['health'],
	queryFn: async () => {
		const res = await fetch('/health');
		if (!res.ok) throw new Error(`${res.status}`);
		return res.json() as Promise<{ status: string }>;
	},
	retry: false,
}));

// Verse: words stagger quickly (0.08s apart), whole sequence done in ~0.5s
const verseWords = ['معرفة', 'اللغات', 'مدخل', 'إلى', 'الحكمة'];
const wordDelay = (i: number) => `${0.05 + i * 0.08}s`;

const sections = [
	{
		id: 'roots',
		href: '/roots',
		arabic: 'جذور',
		label: 'Roots',
		desc: 'Three consonants that hold an entire world of meaning.',
		icon: Sprout,
	},
	{
		id: 'words',
		href: '/words',
		arabic: 'كلمات',
		label: 'Words',
		desc: 'Your personal lexicon — vocabulary with mastery tracking.',
		icon: BookOpen,
	},
	{
		id: 'texts',
		href: '/texts',
		arabic: 'نصوص',
		label: 'Texts',
		desc: 'Passages to read, annotate, and make your own.',
		icon: ScrollText,
	},
	{
		id: 'training',
		href: '/training',
		arabic: 'تدريب',
		label: 'Training',
		desc: 'Spaced repetition — surface what you need to review.',
		icon: Dumbbell,
	},
	{
		id: 'analytics',
		href: '/analytics',
		arabic: 'إحصاءات',
		label: 'Analytics',
		desc: 'See how your Arabic grows over time.',
		icon: BarChart2,
	},
] as const;

// Dictionaries: matching the DictionarySource enum in the backend
const dictionaries = [
	{ label: 'Almany', href: 'https://www.almany.org' },
	{ label: 'Living Arabic Project', href: 'https://livingarabic.com' },
	{ label: 'Derja Ninja', href: 'https://derja.ninja' },
	{ label: 'Reverso Context', href: 'https://context.reverso.net/translation/arabic-english/' },
	{ label: 'Wiktionary', href: 'https://en.wiktionary.org/wiki/Wiktionary:Main_Page' },
	{ label: 'Arabic Student Dictionary', href: 'https://arabic.desert-sky.net' },
	{ label: 'Langenscheidt', href: 'https://www.langenscheidt.com/arabisch-deutsch/' },
];

// TODO: replace placeholders with real URLs
const personalLinks = [
	{ label: 'qalam on GitHub', href: 'https://github.com/antonio-masotti/qalam' },
	{ label: 'Blog', href: 'https://blog.toni-hacks.com/' },
];

const learningLinks = [
	{ label: 'Dreaming Arabic', href: 'https://www.youtube.com/@DreamingArabic' },
	{ label: 'Learn Arabic with Maha', href: 'https://www.youtube.com/@LearnArabicwithMaha' },
	{ label: 'Arabic with Sam', href: 'https://www.youtube.com/@ArabicwithSam' },
];
</script>

<div class="home-shell">
	<!-- ── Qalam mark ── -->
	<span class="home-qalam-mark" aria-label="Qalam">قلم</span>

	<!-- ── Verse ── -->
	<div class="home-verse-block">
		<div class="home-verse-words" aria-label="معرفة اللغات مدخل إلى الحكمة">
			{#each verseWords as word, i}
				<span class="home-verse-word" style="animation-delay: {wordDelay(i)}">
					{word}
				</span>
			{/each}
		</div>
		<p class="home-verse-transliteration">
			maʿrifat al-lughāt madkhal ilā al-ḥikma
		</p>
		<p class="home-verse-translation">
			Knowledge of languages is the doorway to wisdom.
		</p>
	</div>

	<!-- ── Section cards — appear immediately alongside verse ── -->
	<nav class="home-nav-grid" aria-label="Sections">
		{#each sections as s, i}
			<a
				class="home-nav-card"
				href={s.href}
				data-section={s.id}
				style="animation: slide-up 280ms cubic-bezier(0.16,1,0.3,1) both; animation-delay: {0.05 + i * 0.06}s;"
			>
				<div class="home-nav-card-header">
					<div class="home-nav-card-icon">
						<s.icon size={18} />
					</div>
					<div class="home-nav-card-titles">
						<span class="home-nav-card-arabic">{s.arabic}</span>
						<span class="home-nav-card-label">{s.label}</span>
					</div>
				</div>
				<p class="home-nav-card-desc">{s.desc}</p>
				<span class="home-nav-card-cta">
					Explore <ArrowRight size={12} />
				</span>
			</a>
		{/each}
	</nav>

	<!-- ── Resources ── -->
	<section class="home-resources" aria-label="Resources">
		<div class="home-resources-group">
			<span class="home-resources-label">Dictionaries</span>
			<div class="home-resources-links">
				{#each dictionaries as d}
					<a class="home-resource-link" href={d.href} target="_blank" rel="noopener noreferrer">
						{d.label}
						<ExternalLink size={10} />
					</a>
				{/each}
			</div>
		</div>

		<div class="home-resources-group">
			<span class="home-resources-label">Learning</span>
			<div class="home-resources-links">
				{#each learningLinks as l}
					<a class="home-resource-link" href={l.href} target="_blank" rel="noopener noreferrer">
						{l.label}
						<ExternalLink size={10} />
					</a>
				{/each}
			</div>
		</div>

		<div class="home-resources-group">
			<span class="home-resources-label">This project</span>
			<div class="home-resources-links">
				{#each personalLinks as l}
					<a class="home-resource-link" href={l.href} target="_blank" rel="noopener noreferrer">
						{l.label}
						<ExternalLink size={10} />
					</a>
				{/each}
			</div>
		</div>
	</section>

	<!-- ── Backend status ── -->
	<div class="home-status">
		<span
			class="home-status-dot"
			class:ok={health.isSuccess}
			class:error={health.isError}
		></span>
		{#if health.isPending}
			Backend connecting…
		{:else if health.isError}
			Backend unreachable
		{:else}
			Backend · {health.data?.status}
		{/if}
	</div>
</div>
