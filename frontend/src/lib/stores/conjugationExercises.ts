import { createMutation, createQuery, useQueryClient } from '@tanstack/svelte-query';
import {
	answerConjugationExerciseItem,
	completeConjugationExerciseSession,
	createConjugationExerciseSession,
	getConjugationExerciseEligibility,
	getConjugationExerciseSession,
	listConjugationExerciseSessions,
} from '$lib/api/sdk.gen';
import type {
	AnswerConjugationExerciseItemRequest,
	AnswerConjugationExerciseItemResponse,
	ConjugationExerciseEligibilityResponse,
	ConjugationExerciseSessionResponse,
	ConjugationExerciseSessionSummaryResponse,
	CreateConjugationExerciseSessionRequest,
	PaginatedConjugationExerciseSessionsResponse,
} from '$lib/api/types.gen';

function requireData<T>(data: T | undefined, label: string): T {
	if (data === undefined) throw new Error(`${label}: empty response`);
	return data;
}

export function useCreateConjugationExerciseSession() {
	const queryClient = useQueryClient();
	return createMutation(() => ({
		mutationFn: async (request: CreateConjugationExerciseSessionRequest) => {
			const { data, error } = await createConjugationExerciseSession({ body: request });
			if (error) throw error;
			return requireData(
				data,
				'createConjugationExerciseSession'
			) as ConjugationExerciseSessionResponse;
		},
		onSuccess: () =>
			queryClient.invalidateQueries({ queryKey: ['conjugation-exercises', 'sessions'] }),
	}));
}

export function useConjugationExerciseEligibility(
	mode: () => 'NEW' | 'LEARNING' | 'KNOWN' | 'MIXED',
	wordListIds: () => string[]
) {
	return createQuery(() => ({
		queryKey: ['conjugation-exercises', 'eligibility', mode(), wordListIds()],
		queryFn: async () => {
			const { data, error } = await getConjugationExerciseEligibility({
				query: { mode: mode(), wordListId: wordListIds() },
			});
			if (error) throw error;
			return requireData(
				data,
				'getConjugationExerciseEligibility'
			) as ConjugationExerciseEligibilityResponse;
		},
	}));
}

export function useConjugationExerciseSession(id: () => string | undefined) {
	return createQuery(() => ({
		queryKey: ['conjugation-exercises', 'session', id()],
		queryFn: async () => {
			const sessionId = id();
			if (!sessionId) throw new Error('Missing conjugation exercise session id');
			const { data, error } = await getConjugationExerciseSession({ path: { id: sessionId } });
			if (error) throw error;
			return requireData(
				data,
				'getConjugationExerciseSession'
			) as ConjugationExerciseSessionResponse;
		},
		enabled: !!id(),
	}));
}

export function useAnswerConjugationExerciseItem() {
	const queryClient = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({
			sessionId,
			body,
		}: {
			sessionId: string;
			body: AnswerConjugationExerciseItemRequest;
		}) => {
			const { data, error } = await answerConjugationExerciseItem({
				path: { id: sessionId },
				body,
			});
			if (error) throw error;
			return requireData(
				data,
				'answerConjugationExerciseItem'
			) as AnswerConjugationExerciseItemResponse;
		},
		onSuccess: (_data, variables) =>
			queryClient.invalidateQueries({
				queryKey: ['conjugation-exercises', 'session', variables.sessionId],
			}),
	}));
}

export function useCompleteConjugationExerciseSession() {
	const queryClient = useQueryClient();
	return createMutation(() => ({
		mutationFn: async (sessionId: string) => {
			const { data, error } = await completeConjugationExerciseSession({ path: { id: sessionId } });
			if (error) throw error;
			return requireData(
				data,
				'completeConjugationExerciseSession'
			) as ConjugationExerciseSessionSummaryResponse;
		},
		onSuccess: (_data, sessionId) => {
			queryClient.invalidateQueries({ queryKey: ['conjugation-exercises', 'session', sessionId] });
			queryClient.invalidateQueries({ queryKey: ['conjugation-exercises', 'sessions'] });
		},
	}));
}

export function useListConjugationExerciseSessions(page: () => number, size: () => number) {
	return createQuery(() => ({
		queryKey: ['conjugation-exercises', 'sessions', page(), size()],
		queryFn: async () => {
			const { data, error } = await listConjugationExerciseSessions({
				query: { page: page(), size: size() },
			});
			if (error) throw error;
			return requireData(
				data,
				'listConjugationExerciseSessions'
			) as PaginatedConjugationExerciseSessionsResponse;
		},
	}));
}
