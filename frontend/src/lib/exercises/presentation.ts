import type { ExerciseSessionResponse, ExerciseSessionSummaryResponse } from '$lib/api/types.gen';

export function summaryFromCompletedExercise(
	session: ExerciseSessionResponse
): ExerciseSessionSummaryResponse {
	const correct = session.items.filter((item) => item.result === 'CORRECT').length;
	const incorrect = session.items.filter((item) => item.result === 'INCORRECT').length;
	const skipped = session.items.filter(
		(item) => item.result !== 'CORRECT' && item.result !== 'INCORRECT'
	).length;
	const answered = correct + incorrect;

	return {
		sessionId: session.id,
		mode: session.mode,
		totalItems: session.items.length,
		correct,
		incorrect,
		skipped,
		accuracy: answered === 0 ? 0 : correct / answered,
		promotions: [],
		completedAt: session.completedAt ?? session.createdAt,
	};
}
