import { fireEvent, render, screen } from '@testing-library/svelte';
import { describe, expect, it, vi } from 'vitest';
import type { SessionSummaryResponse, TrainingSessionWordResponse } from '$lib/api/types.gen';
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

function buildWords(): TrainingSessionWordResponse[] {
	return [
		{
			wordId: 'word-correct',
			arabicText: 'كتب',
			dialect: 'MSA',
			transliteration: 'kataba',
			translation: 'write',
			frontSide: 'ARABIC',
			position: 0,
			result: 'CORRECT',
			masteryLevel: 'LEARNING',
		},
		{
			wordId: 'word-incorrect',
			arabicText: 'قرأ',
			dialect: 'MSA',
			transliteration: 'qaraʾa',
			translation: 'read',
			frontSide: 'TRANSLATION',
			position: 1,
			result: 'INCORRECT',
			masteryLevel: 'LEARNING',
		},
		{
			wordId: 'word-skipped',
			arabicText: 'ذهب',
			dialect: 'MSA',
			transliteration: null,
			translation: 'go',
			frontSide: 'ARABIC',
			position: 2,
			result: 'SKIPPED',
			masteryLevel: 'NEW',
		},
	];
}

describe('SessionSummary', () => {
	it('renders stats and promotions from summary', () => {
		render(SessionSummary, {
			props: {
				summary: buildSummary(),
				words: buildWords(),
			},
		});

		expect(screen.getByLabelText('76% accuracy')).toBeInTheDocument();
		expect(screen.getByText('19')).toBeInTheDocument();
		expect(screen.getByText('Flashcards | MIXED')).toBeInTheDocument();
		expect(screen.getByText('kataba')).toBeInTheDocument();
		expect(document.querySelector('.session-word-result-status.correct')).toHaveTextContent(
			'Correct'
		);
		expect(document.querySelector('.session-word-result-status.incorrect')).toHaveTextContent(
			'Wrong'
		);
		expect(document.querySelector('.session-word-result-status.skipped')).toHaveTextContent(
			'Skipped'
		);
		expect(screen.getByText('كتب').closest('a')).toHaveAttribute('href', '/words/word-correct');
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
				words: [],
			},
		});

		await fireEvent.click(screen.getByRole('button', { name: 'New flashcard session' }));

		expect(gotoMock).toHaveBeenCalledWith('/training/flashcards');
	});
});
