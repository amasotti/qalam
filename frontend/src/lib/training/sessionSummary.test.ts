import { describe, expect, it } from 'vitest';
import type { TrainingSessionResponse } from '$lib/api/types.gen';
import { summaryFromCompletedSession } from './sessionSummary';

const session: TrainingSessionResponse = {
	id: 'session-1',
	mode: 'MIXED',
	status: 'COMPLETED',
	createdAt: '2026-07-18T10:00:00Z',
	completedAt: '2026-07-18T10:15:00Z',
	words: [
		{
			wordId: 'word-1',
			arabicText: 'كتب',
			dialect: 'MSA',
			translation: 'write',
			frontSide: 'ARABIC',
			position: 0,
			masteryLevel: 'LEARNING',
			result: 'CORRECT',
		},
		{
			wordId: 'word-2',
			arabicText: 'قرأ',
			dialect: 'MSA',
			translation: 'read',
			frontSide: 'ARABIC',
			position: 1,
			masteryLevel: 'LEARNING',
			result: 'INCORRECT',
		},
		{
			wordId: 'word-3',
			arabicText: 'ذهب',
			dialect: 'MSA',
			translation: 'go',
			frontSide: 'ARABIC',
			position: 2,
			masteryLevel: 'NEW',
			result: 'SKIPPED',
		},
	],
};

describe('summaryFromCompletedSession', () => {
	it('derives durable review statistics from recorded word results', () => {
		expect(summaryFromCompletedSession(session)).toMatchObject({
			sessionId: 'session-1',
			correct: 1,
			incorrect: 1,
			skipped: 1,
			accuracy: 0.5,
			promotions: [],
			completedAt: '2026-07-18T10:15:00Z',
		});
	});

	it('uses zero accuracy when no answers were given', () => {
		const skippedSession = {
			...session,
			words: session.words.map((word) => ({ ...word, result: 'SKIPPED' as const })),
		};

		expect(summaryFromCompletedSession(skippedSession).accuracy).toBe(0);
	});
});
