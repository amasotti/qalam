import type {
	ExerciseSessionItemResponse,
	ExerciseSessionResponse,
	ExerciseSessionSummaryResponse,
} from '$lib/api/types.gen';

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

export function exerciseItemWord(item: ExerciseSessionItemResponse) {
	const wordOption = item.options.find((option) => option.wordId === item.wordId);

	return {
		arabicText:
			wordOption?.arabicText ?? (item.prompt.kind === 'ARABIC_WORD' ? item.prompt.text : ''),
		transliteration: wordOption?.transliteration ?? null,
		translation:
			wordOption?.translation ?? (item.prompt.kind === 'TRANSLATION' ? item.prompt.text : null),
	};
}
