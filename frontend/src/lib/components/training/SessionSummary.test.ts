import { fireEvent, render, screen } from '@testing-library/svelte';
import { describe, expect, it, vi } from 'vitest';
import type { SessionSummaryResponse } from '$lib/api/types.gen';
import SessionSummary from './SessionSummary.svelte';

const { gotoMock } = vi.hoisted(() => ({
	gotoMock: vi.fn(),
}));

vi.mock('$app/navigation', () => ({
	goto: gotoMock,
}));

function buildSummary(overrides: Partial<SessionSummaryResponse> = {}): SessionSummaryResponse {
	return {
		sessionId: 'session-1',
		mode: 'MIXED',
		totalWords: 25,
		correct: 19,
		incorrect: 5,
		skipped: 1,
		accuracy: 0.76,
		promotions: [{ wordId: 'w1', from: 'LEARNING', to: 'KNOWN' }],
		completedAt: '2026-05-03T09:00:00Z',
		...overrides,
	};
}

describe('SessionSummary', () => {
	it('renders stats and promotions from summary', () => {
		render(SessionSummary, {
			props: {
				summary: buildSummary(),
			},
		});

		expect(screen.getByText('76%')).toBeInTheDocument();
		expect(screen.getByText('19')).toBeInTheDocument();
		expect(screen.getByText('MIXED')).toBeInTheDocument();
		expect(screen.getByText('LEARNING → KNOWN')).toBeInTheDocument();
	});

	it('starts a new session when button clicked', async () => {
		render(SessionSummary, {
			props: {
				summary: buildSummary({
					accuracy: 1,
					correct: 1,
					incorrect: 0,
					skipped: 0,
					mode: 'REVIEW',
					totalWords: 1,
					promotions: [],
				}),
			},
		});

		await fireEvent.click(screen.getByRole('button', { name: 'New session' }));

		expect(gotoMock).toHaveBeenCalledWith('/training');
	});
});
