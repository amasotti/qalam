<script lang="ts">
import '../app.css';
import { QueryClient, QueryClientProvider } from '@tanstack/svelte-query';
import { page } from '$app/state';

const { children } = $props();

const queryClient = new QueryClient({
	defaultOptions: { queries: { staleTime: 30_000 } },
});

function isActive(href: string): boolean {
	const pathname = page.url.pathname;
	if (href === '/') return pathname === '/';
	return pathname === href || pathname.startsWith(href);
}
</script>

<QueryClientProvider client={queryClient}>
	<div class="app-shell">
		<aside class="sidebar">
			<div class="sb-head">
				<span class="sb-ar">قلم</span>
				<span class="sb-name">Qalam</span>
			</div>
			<nav class="sb-nav">
				<span class="sb-section">Study</span>
				<a href="/words" class="sb-link" class:active={isActive('/words')}>
					Words <span class="ar">كلمات</span>
				</a>
				<a href="/roots" class="sb-link" class:active={isActive('/roots')}>
					Roots <span class="ar">جذور</span>
				</a>
				<a href="/texts" class="sb-link" class:active={isActive('/texts')}>
					Texts <span class="ar">نصوص</span>
				</a>
				<span class="sb-section">Practice</span>
				<a href="/training" class="sb-link" class:active={isActive('/training')}>
					Training <span class="ar">تدريب</span>
				</a>
				<a href="/analytics" class="sb-link" class:active={isActive('/analytics')}>
					Analytics <span class="ar">إحصاء</span>
				</a>
			</nav>
			<div class="sb-progress">
				<div class="sb-prog-label">Vocabulary</div>
				<div class="sb-prog-track"><div class="sb-prog-fill" style="width: 38%"></div></div>
				<div class="sb-prog-note">Study your words</div>
			</div>
		</aside>

		<main class="sidebar-main">
			{@render children()}
		</main>
	</div>
</QueryClientProvider>
