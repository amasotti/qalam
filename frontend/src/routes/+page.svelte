<script lang="ts">
import { createQuery } from '@tanstack/svelte-query';
import { BarChart2, BookOpen, Dumbbell, ScrollText, Sprout } from 'lucide-svelte';

const health = createQuery(() => ({
	queryKey: ['health'],
	queryFn: async () => {
		const res = await fetch('/health');
		if (!res.ok) throw new Error(`${res.status}`);
		return res.json() as Promise<{ status: string }>;
	},
	retry: false,
}));

const verseWords = ['معرفة', 'اللغات', 'مدخل', 'إلى', 'الحكمة'];
// Each word appears 0.25s after the previous; first word starts at 0.5s
const wordDelay = (i: number) => `${0.5 + i * 0.28}s`;

const sections = [
	{
		href: '/roots',
		arabic: 'جذور',
		label: 'Roots',
		icon: Sprout,
	},
	{
		href: '/words',
		arabic: 'كلمات',
		label: 'Words',
		icon: BookOpen,
	},
	{
		href: '/texts',
		arabic: 'نصوص',
		label: 'Texts',
		icon: ScrollText,
	},
	{
		href: '/training',
		arabic: 'تدريب',
		label: 'Training',
		icon: Dumbbell,
	},
	{
		href: '/analytics',
		arabic: 'إحصاءات',
		label: 'Analytics',
		icon: BarChart2,
	},
];
</script>

<div class="home-shell">
	<!-- ── Qalam mark ── -->
	<span class="home-qalam-mark">قلم</span>

	<!-- ── Animated verse ── -->
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

	<!-- ── Section nav ── -->
	<nav class="home-nav-grid stagger-children" aria-label="Sections">
		{#each sections as s}
			<a class="home-nav-card" href={s.href}>
				<div class="home-nav-card-icon">
					<s.icon size={16} />
				</div>
				<span class="home-nav-card-arabic">{s.arabic}</span>
				<span class="home-nav-card-label">{s.label}</span>
			</a>
		{/each}
	</nav>

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
