<script lang="ts">
import { useAnalytics } from '$lib/stores/analytics';

const analytics = useAnalytics();
</script>

{#if analytics.isPending}
  <p class="loading">Loading…</p>
{:else if analytics.isError}
  <p class="error">Failed to load analytics.</p>
{:else if analytics.data}
  {@const d = analytics.data}

  <div class="analytics-page">
    <h1>Analytics</h1>

    <div class="stat-cards">
      <div class="stat-card">
        <span class="stat-value">{d.words.total}</span>
        <span class="stat-label">Words</span>
      </div>
      <div class="stat-card">
        <span class="stat-value">{d.texts.total}</span>
        <span class="stat-label">Texts</span>
      </div>
      <div class="stat-card">
        <span class="stat-value">{d.roots.total}</span>
        <span class="stat-label">Roots</span>
      </div>
      <div class="stat-card">
        <span class="stat-value">{d.training.completedSessions}</span>
        <span class="stat-label">Sessions</span>
      </div>
    </div>

    <section class="analytics-section">
      <h2>Words</h2>
      <div class="distributions">
        {#each [
          { label: 'Mastery', dist: d.words.byMastery, total: d.words.total },
          { label: 'Dialect', dist: d.words.byDialect, total: d.words.total },
          { label: 'Difficulty', dist: d.words.byDifficulty, total: d.words.total },
          { label: 'Part of Speech', dist: d.words.byPartOfSpeech, total: d.words.total },
        ] as group}
          <div class="dist-group">
            <h3>{group.label}</h3>
            {#each Object.entries(group.dist).sort((a, b) => b[1] - a[1]) as [key, count]}
              <div class="bar-row">
                <span class="bar-label">{key}</span>
                <div class="bar-track">
                  <div
                    class="bar-fill"
                    style="width: {group.total > 0 ? ((count / group.total) * 100).toFixed(1) : 0}%"
                  ></div>
                </div>
                <span class="bar-count">{count}</span>
              </div>
            {/each}
          </div>
        {/each}
      </div>
    </section>

    <section class="analytics-section">
      <h2>Texts</h2>
      <div class="distributions">
        {#each [
          { label: 'Dialect', dist: d.texts.byDialect, total: d.texts.total },
          { label: 'Difficulty', dist: d.texts.byDifficulty, total: d.texts.total },
        ] as group}
          <div class="dist-group">
            <h3>{group.label}</h3>
            {#each Object.entries(group.dist).sort((a, b) => b[1] - a[1]) as [key, count]}
              <div class="bar-row">
                <span class="bar-label">{key}</span>
                <div class="bar-track">
                  <div
                    class="bar-fill"
                    style="width: {group.total > 0 ? ((count / group.total) * 100).toFixed(1) : 0}%"
                  ></div>
                </div>
                <span class="bar-count">{count}</span>
              </div>
            {/each}
          </div>
        {/each}
      </div>
    </section>

    <section class="analytics-section">
      <h2>Training</h2>
      <div class="stat-cards">
        <div class="stat-card">
          <span class="stat-value">{Math.round(d.training.averageAccuracy * 100)}%</span>
          <span class="stat-label">Avg Accuracy</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{d.training.totalPromotions}</span>
          <span class="stat-label">Promotions</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{d.training.totalSessions}</span>
          <span class="stat-label">Total Sessions</span>
        </div>
      </div>

      {#if d.training.recentSessions.length > 0}
        <div class="dist-group" style="margin-top: 1.5rem;">
          <h3>Recent sessions (last {d.training.recentSessions.length})</h3>
          {#each d.training.recentSessions as s}
            <div class="bar-row">
              <span class="bar-label">
                {s.mode} · {new Date(s.date).toLocaleDateString()}
              </span>
              <div class="bar-track">
                <div
                  class="bar-fill"
                  style="width: {(s.accuracy * 100).toFixed(1)}%"
                ></div>
              </div>
              <span class="bar-count">{(s.accuracy * 100).toFixed(0)}%</span>
            </div>
          {/each}
        </div>
      {/if}
    </section>
  </div>
{/if}

<style>
  .analytics-page {
    max-width: 52rem;
    margin: 2rem auto;
    padding: 0 1rem;
    display: flex;
    flex-direction: column;
    gap: 2.5rem;
  }

  h1 { font-size: 1.5rem; font-weight: 700; margin: 0; }
  h2 { font-size: 1.1rem; font-weight: 600; margin: 0 0 1rem; }
  h3 { font-size: 0.8rem; font-weight: 600; text-transform: uppercase;
       letter-spacing: 0.05em; color: hsl(var(--muted-foreground)); margin: 0 0 0.5rem; }

  .stat-cards {
    display: flex;
    gap: 1rem;
    flex-wrap: wrap;
  }

  .stat-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.25rem;
    padding: 1rem 1.5rem;
    border: 1px solid hsl(var(--border));
    border-radius: 0.5rem;
    min-width: 6rem;
  }

  .stat-value { font-size: 1.75rem; font-weight: 700; line-height: 1; }
  .stat-label { font-size: 0.75rem; color: hsl(var(--muted-foreground)); text-transform: uppercase;
                letter-spacing: 0.04em; }

  .analytics-section {
    display: flex;
    flex-direction: column;
    gap: 0;
  }

  .distributions {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(20rem, 1fr));
    gap: 1.5rem;
  }

  .dist-group {
    display: flex;
    flex-direction: column;
    gap: 0.4rem;
  }

  .bar-row {
    display: grid;
    grid-template-columns: 10rem 1fr 3rem;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.8rem;
  }

  .bar-label {
    color: hsl(var(--foreground));
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .bar-track {
    height: 0.5rem;
    background: hsl(var(--muted));
    border-radius: 9999px;
    overflow: hidden;
  }

  .bar-fill {
    height: 100%;
    background: hsl(var(--primary));
    border-radius: 9999px;
    transition: width 0.3s ease;
    min-width: 2px;
  }

  .bar-count {
    text-align: right;
    color: hsl(var(--muted-foreground));
    font-variant-numeric: tabular-nums;
  }

  .loading, .error {
    max-width: 52rem;
    margin: 2rem auto;
    padding: 0 1rem;
  }

  .error { color: hsl(var(--destructive)); }
</style>
