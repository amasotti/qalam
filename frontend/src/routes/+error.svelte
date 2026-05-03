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
