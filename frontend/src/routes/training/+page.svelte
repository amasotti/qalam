<script lang="ts">
import { goto } from '$app/navigation';
import { Button } from '$lib/components/ui/button';
import { useCreateSession, useTrainingStats } from '$lib/stores/training';

const createSession = useCreateSession();
const stats = useTrainingStats();

const modes = ['MIXED', 'NEW', 'LEARNING', 'KNOWN'] as const;
type Mode = (typeof modes)[number];

let selectedMode = $state<Mode>('MIXED');
let sessionSize = $state(15);

function start() {
	createSession.mutate(
		{ mode: selectedMode, size: sessionSize },
		{ onSuccess: (session) => goto(`/training/${session.id}`) }
	);
}

const modeLabels: Record<Mode, string> = {
	MIXED: 'Mixed (all levels)',
	NEW: 'New words only',
	LEARNING: 'Learning',
	KNOWN: 'Known',
};
</script>

<div class="training-setup">
  <h1>Start training</h1>

  {#if stats.data}
    <div class="distribution">
      {#each Object.entries(stats.data.masteryDistribution) as [level, count]}
        <span class="dist-badge">{level}: {count}</span>
      {/each}
    </div>
  {/if}

  <section class="setup-form">
    <div class="form-field">
      <span class="form-label">Mode</span>
      <div class="mode-buttons" role="group" aria-label="Training mode">
        {#each modes as mode}
          <Button
            variant={selectedMode === mode ? 'default' : 'outline'}
            onclick={() => (selectedMode = mode)}
          >
            {modeLabels[mode]}
          </Button>
        {/each}
      </div>
    </div>

    <div class="form-field">
      <label class="form-label" for="session-size">Words per session: {sessionSize}</label>
      <input
        id="session-size"
        type="range"
        min="5"
        max="50"
        step="5"
        bind:value={sessionSize}
      />
      <div class="range-hints"><span>5</span><span>50</span></div>
    </div>

    {#if createSession.error}
      <p class="form-error-msg">{createSession.error.message}</p>
    {/if}

    <Button onclick={start} disabled={createSession.isPending}>
      {createSession.isPending ? 'Starting…' : 'Start'}
    </Button>
  </section>
</div>

<style>
  .training-setup {
    max-width: 32rem;
    margin: 2rem auto;
    padding: 0 1rem;
    display: flex;
    flex-direction: column;
    gap: 2rem;
  }

  .distribution {
    display: flex;
    gap: 0.5rem;
    flex-wrap: wrap;
  }

  .dist-badge {
    font-size: 0.75rem;
    padding: 0.2rem 0.6rem;
    border-radius: 9999px;
    background: hsl(var(--muted));
    color: hsl(var(--muted-foreground));
  }

  .setup-form {
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
  }

  .mode-buttons {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
  }

  input[type='range'] { width: 100%; }

  .range-hints {
    display: flex;
    justify-content: space-between;
    font-size: 0.75rem;
    color: hsl(var(--muted-foreground));
  }

</style>
