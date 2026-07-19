<script lang="ts">
    import {goto} from '$app/navigation';
    import type {SessionSummaryResponse, TrainingSessionWordResponse} from '$lib/api/types.gen';
    import {Button} from '$lib/components/ui/button';

    interface Props {
        summary: SessionSummaryResponse;
        words: TrainingSessionWordResponse[];
    }

    const STATUS_MAP = {
        CORRECT: 'Correct',
        INCORRECT: 'Wrong',
        SKIPPED: 'Skipped'
    } as const;
    let {summary, words}: Props = $props();
    let accuracyPercent = $derived(Math.round(summary.accuracy * 100));

</script>

<div class="session-summary">
    <h1 class="session-summary-heading">Session complete</h1>

    <div class="session-summary-stats">
        <div class="session-stat">
            <div class="session-stat-value accuracy">{accuracyPercent}%</div>
            <div class="session-stat-label">Accuracy</div>
        </div>
        <div class="session-stat">
            <div class="session-stat-value correct">{summary.correct}</div>
            <div class="session-stat-label">Correct</div>
        </div>
        <div class="session-stat">
            <div class="session-stat-value incorrect">{summary.incorrect}</div>
            <div class="session-stat-label">Wrong</div>
        </div>
        <div class="session-stat">
            <div class="session-stat-value skipped">{summary.skipped}</div>
            <div class="session-stat-label">Skipped</div>
        </div>
        <div class="session-stat">
            <div class="session-stat-value mode">{summary.mode}</div>
            <div class="session-stat-label">Mode</div>
        </div>
    </div>

    {#if summary.promotions.length > 0}
        <div class="promotions-section">
            <h2 class="promotions-heading">Mastery promotions</h2>
            <ul class="promotions-list">
                {#each summary.promotions as promotion (promotion.wordId)}
                    <li class="promotion-item">
                        {promotion.from} → {promotion.to}
                    </li>
                {/each}
            </ul>
        </div>
    {/if}

    <section class="session-word-results" aria-labelledby="session-word-results-heading">
        <h2 id="session-word-results-heading" class="session-word-results-heading">Trained Words</h2>

        <ul class="session-word-results-list">
            {#each words as word (word.wordId)}
                <li class="session-word-result">
                    <a class="session-word-link" href={`/words/${word.wordId}`}>
                        <span class="session-word-arabic" lang="ar" dir="rtl">{word.arabicText}</span>
                        <span>{word.translation || 'no translation'}</span>
                        <span class="session-word-transliteration">{word.transliteration || 'no transl' }</span>
                    </a>
                    <span
                            class="session-word-result-status"
                            class:correct={word.result === 'CORRECT'}
                            class:incorrect={word.result === 'INCORRECT'}
                            class:skipped={word.result === 'SKIPPED'}
                    >
       					{STATUS_MAP[word.result!]}
    				</span>
                </li>
            {/each}
        </ul>
    </section>

    <div class="session-summary-actions">
        <Button onclick={() => goto('/training')}>New session</Button>
    </div>
</div>
