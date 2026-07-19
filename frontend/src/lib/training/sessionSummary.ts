import type { SessionSummaryResponse, TrainingSessionResponse } from '$lib/api/types.gen';

export function summaryFromCompletedSession(
	session: TrainingSessionResponse
): SessionSummaryResponse {
	const words = session.words ?? [];
	const correct = words.filter((word) => word.result === 'CORRECT').length;
	const incorrect = words.filter((word) => word.result === 'INCORRECT').length;
	const skipped = words.filter((word) => word.result === 'SKIPPED').length;
	const answered = correct + incorrect;

	return {
		sessionId: session.id,
		mode: session.mode,
		totalWords: words.length,
		correct,
		incorrect,
		skipped,
		accuracy: answered === 0 ? 0 : correct / answered,
		promotions: [],
		completedAt: session.completedAt ?? session.createdAt,
	};
}
