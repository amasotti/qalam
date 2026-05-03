<script lang="ts">
import { useAnalytics } from '$lib/stores/analytics';

const analytics = useAnalytics();
</script>

{#if analytics.isPending}
  <p class="panel-page status-text status-text-muted">Loading…</p>
{:else if analytics.isError}
  <p class="panel-page status-text status-text-danger">Failed to load analytics.</p>
{:else if analytics.data}
  {@const d = analytics.data}

  <div class="panel-page">
    <h1 class="panel-page-title">Analytics</h1>

    <div class="panel-grid">
      <div class="panel-card">
        <span class="panel-value">{d.words.total}</span>
        <span class="panel-label">Words</span>
      </div>
      <div class="panel-card">
        <span class="panel-value">{d.texts.total}</span>
        <span class="panel-label">Texts</span>
      </div>
      <div class="panel-card">
        <span class="panel-value">{d.roots.total}</span>
        <span class="panel-label">Roots</span>
      </div>
      <div class="panel-card">
        <span class="panel-value">{d.training.completedSessions}</span>
        <span class="panel-label">Sessions</span>
      </div>
    </div>

    <section class="panel-section">
      <h2 class="panel-section-title">Words</h2>
      <div class="distribution-grid">
        {#each [
          { label: 'Mastery', dist: d.words.byMastery, total: d.words.total },
          { label: 'Dialect', dist: d.words.byDialect, total: d.words.total },
          { label: 'Difficulty', dist: d.words.byDifficulty, total: d.words.total },
          { label: 'Part of Speech', dist: d.words.byPartOfSpeech, total: d.words.total },
        ] as group}
          <div class="distribution-group">
            <h3 class="panel-subtitle">{group.label}</h3>
            {#each Object.entries(group.dist).sort((a, b) => b[1] - a[1]) as [key, count]}
              <div class="distribution-row">
                <span class="distribution-label">{key}</span>
                <div class="distribution-track">
                  <div
                    class="distribution-fill"
                    style="width: {group.total > 0 ? ((count / group.total) * 100).toFixed(1) : 0}%"
                  ></div>
                </div>
                <span class="distribution-count">{count}</span>
              </div>
            {/each}
          </div>
        {/each}
      </div>
    </section>

    <section class="panel-section">
      <h2 class="panel-section-title">Texts</h2>
      <div class="distribution-grid">
        {#each [
          { label: 'Dialect', dist: d.texts.byDialect, total: d.texts.total },
          { label: 'Difficulty', dist: d.texts.byDifficulty, total: d.texts.total },
        ] as group}
          <div class="distribution-group">
            <h3 class="panel-subtitle">{group.label}</h3>
            {#each Object.entries(group.dist).sort((a, b) => b[1] - a[1]) as [key, count]}
              <div class="distribution-row">
                <span class="distribution-label">{key}</span>
                <div class="distribution-track">
                  <div
                    class="distribution-fill"
                    style="width: {group.total > 0 ? ((count / group.total) * 100).toFixed(1) : 0}%"
                  ></div>
                </div>
                <span class="distribution-count">{count}</span>
              </div>
            {/each}
          </div>
        {/each}
      </div>
    </section>

    <section class="panel-section">
      <h2 class="panel-section-title">Training</h2>
      <div class="panel-grid">
        <div class="panel-card">
          <span class="panel-value">{Math.round(d.training.averageAccuracy * 100)}%</span>
          <span class="panel-label">Avg Accuracy</span>
        </div>
        <div class="panel-card">
          <span class="panel-value">{d.training.totalPromotions}</span>
          <span class="panel-label">Promotions</span>
        </div>
        <div class="panel-card">
          <span class="panel-value">{d.training.totalSessions}</span>
          <span class="panel-label">Total Sessions</span>
        </div>
      </div>

      {#if d.training.recentSessions.length > 0}
        <div class="distribution-group section-block">
          <h3 class="panel-subtitle">Recent sessions (last {d.training.recentSessions.length})</h3>
          {#each d.training.recentSessions as s}
            <div class="distribution-row">
              <span class="distribution-label">
                {s.mode} · {new Date(s.date).toLocaleDateString()}
              </span>
              <div class="distribution-track">
                <div
                  class="distribution-fill"
                  style="width: {(s.accuracy * 100).toFixed(1)}%"
                ></div>
              </div>
              <span class="distribution-count">{(s.accuracy * 100).toFixed(0)}%</span>
            </div>
          {/each}
        </div>
      {/if}
    </section>
  </div>
{/if}
