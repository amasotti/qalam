<script lang="ts">
import { page } from '$app/state';
</script>

<div class="error-page page-enter">
	<div class="error-content">
		<span class="error-status arabic-display">{page.status}</span>
		<h1 class="error-title">{page.error?.message ?? 'Something went wrong'}</h1>
		<a href="/" class="error-home-link">← Back to home</a>
	</div>
</div>

<style>
	.error-page {
		display: flex;
		align-items: center;
		justify-content: center;
		min-height: 60vh;
		padding: 2rem;
	}

	.error-content {
		text-align: center;
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 1rem;
	}

	.error-status {
		font-size: 4rem;
		opacity: 0.2;
		line-height: 1;
	}

	.error-title {
		font-size: 1.125rem;
		font-weight: 500;
		color: var(--color-text-primary, inherit);
		margin: 0;
	}

	.error-home-link {
		font-size: 0.875rem;
		opacity: 0.6;
		text-decoration: none;
		transition: opacity 0.15s;
	}

	.error-home-link:hover {
		opacity: 1;
	}
</style>
