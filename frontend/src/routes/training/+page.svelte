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
  <h1 class="panel-page-title">Start training</h1>

  {#if stats.data}
    <div class="distribution-chips">
      {#each Object.entries(stats.data.masteryDistribution) as [level, count]}
        <span class="distribution-badge">{level}: {count}</span>
      {/each}
    </div>
  {/if}

  <section class="training-form">
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
        class="range-input"
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
