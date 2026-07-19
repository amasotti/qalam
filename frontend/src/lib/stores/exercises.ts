import { createMutation, createQuery, useQueryClient } from '@tanstack/svelte-query';
import {
	answerExerciseItem,
	completeExerciseSession,
	createExerciseSession,
	getExerciseSession,
	listExerciseSessions,
} from '$lib/api/sdk.gen';
import type {
	AnswerExerciseItemRequest,
	AnswerExerciseItemResponse,
	CreateExerciseSessionRequest,
	ExerciseSessionResponse,
	ExerciseSessionSummaryResponse,
	PaginatedExerciseSessionsResponse,
} from '$lib/api/types.gen';

function requireData<T>(data: T | undefined, label: string): T {
	if (data === undefined) throw new Error(`${label}: empty response`);
	return data;
}

export function useCreateExerciseSession() {
	const queryClient = useQueryClient();
	return createMutation(() => ({
		mutationFn: async (request: CreateExerciseSessionRequest) => {
			const { data, error } = await createExerciseSession({ body: request });
			if (error) throw error;
			return requireData(data, 'createExerciseSession') as ExerciseSessionResponse;
		},
		onSuccess: () => queryClient.invalidateQueries({ queryKey: ['exercises', 'sessions'] }),
	}));
}

export function useExerciseSession(id: () => string | undefined) {
	return createQuery(() => ({
		queryKey: ['exercises', 'session', id()],
		queryFn: async () => {
			const sessionId = id();
			if (!sessionId) throw new Error('Missing exercise session id');
			const { data, error } = await getExerciseSession({ path: { id: sessionId } });
			if (error) throw error;
			return requireData(data, 'getExerciseSession') as ExerciseSessionResponse;
		},
		enabled: !!id(),
	}));
}

export function useAnswerExerciseItem() {
	const queryClient = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({
			sessionId,
			body,
		}: {
			sessionId: string;
			body: AnswerExerciseItemRequest;
		}) => {
			const { data, error } = await answerExerciseItem({ path: { id: sessionId }, body });
			if (error) throw error;
			return requireData(data, 'answerExerciseItem') as AnswerExerciseItemResponse;
		},
		onSuccess: (_data, variables) =>
			queryClient.invalidateQueries({ queryKey: ['exercises', 'session', variables.sessionId] }),
	}));
}

export function useCompleteExerciseSession() {
	const queryClient = useQueryClient();
	return createMutation(() => ({
		mutationFn: async (sessionId: string) => {
			const { data, error } = await completeExerciseSession({ path: { id: sessionId } });
			if (error) throw error;
			return requireData(data, 'completeExerciseSession') as ExerciseSessionSummaryResponse;
		},
		onSuccess: (_data, sessionId) => {
			queryClient.invalidateQueries({ queryKey: ['exercises', 'session', sessionId] });
			queryClient.invalidateQueries({ queryKey: ['exercises', 'sessions'] });
		},
	}));
}

export function useListExerciseSessions(page: () => number, size: () => number) {
	return createQuery(() => ({
		queryKey: ['exercises', 'sessions', page(), size()],
		queryFn: async () => {
			const { data, error } = await listExerciseSessions({ query: { page: page(), size: size() } });
			if (error) throw error;
			return requireData(data, 'listExerciseSessions') as PaginatedExerciseSessionsResponse;
		},
	}));
}
