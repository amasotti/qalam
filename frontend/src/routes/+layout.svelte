<script lang="ts">
import '../app.css';
import { QueryClient, QueryClientProvider } from '@tanstack/svelte-query';
import { BarChart2, BookOpen, Dumbbell, House, ScrollText, Sprout } from 'lucide-svelte';
import { page } from '$app/state';

const { children } = $props();

const queryClient = new QueryClient({
	defaultOptions: { queries: { staleTime: 30_000 } },
});

const navLinks = [
	{ href: '/', label: 'Home', icon: House },
	{ href: '/words', label: 'Words', icon: BookOpen },
	{ href: '/roots', label: 'Roots', icon: Sprout },
	{ href: '/texts', label: 'Texts', icon: ScrollText },
	{ href: '/training', label: 'Training', icon: Dumbbell },
	{ href: '/analytics', label: 'Analytics', icon: BarChart2 },
];
</script>

<QueryClientProvider client={queryClient}>
	<div class="app-shell">
		<aside class="sidebar">
			<div class="sidebar-header">
				<span class="arabic-display" style="font-size: 1.75rem;">قلم</span>
				<p style="font-size: 0.7rem; opacity: 0.5; margin-top: 0.125rem; letter-spacing: 0.08em; text-transform: uppercase;">Qalam</p>
			</div>

			<nav class="sidebar-nav">
				{#each navLinks as link}
					<a href={link.href} class="sidebar-nav-link" class:active={page.url.pathname === link.href || (link.href !== '/' && page.url.pathname.startsWith(link.href))}>
						<link.icon size={16} />
						{link.label}
					</a>
				{/each}
			</nav>
		</aside>

		<main class="sidebar-main">
			{@render children()}
		</main>
	</div>
</QueryClientProvider>
