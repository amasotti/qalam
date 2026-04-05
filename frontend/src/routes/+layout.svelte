<script lang="ts">
	import '../app.css';
	import { House, BookOpen, Sprout, ScrollText, Dumbbell, BarChart2 } from 'lucide-svelte';

	let { children } = $props();

	const navLinks = [
		{ href: '/',          label: 'Home',      icon: House      },
		{ href: '/words',     label: 'Words',     icon: BookOpen   },
		{ href: '/roots',     label: 'Roots',     icon: Sprout     },
		{ href: '/texts',     label: 'Texts',     icon: ScrollText },
		{ href: '/training',  label: 'Training',  icon: Dumbbell   },
		{ href: '/analytics', label: 'Analytics', icon: BarChart2  },
	];
</script>

<div class="app-shell">
	<aside class="sidebar">
		<div class="sidebar-header">
			<span class="arabic-display" style="font-size: 1.75rem;">قلم</span>
			<p style="font-size: 0.7rem; opacity: 0.5; margin-top: 0.125rem; letter-spacing: 0.08em; text-transform: uppercase;">Qalam</p>
		</div>

		<nav class="sidebar-nav">
			{#each navLinks as link}
				<a href={link.href} class="sidebar-nav-link">
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
