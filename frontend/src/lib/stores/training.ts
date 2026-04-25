import { createMutation, createQuery, useQueryClient } from '@tanstack/svelte-query';
import {
	completeTrainingSession,
	createTrainingSession,
	getTrainingSession,
	getTrainingStats,
	listTrainingSessions,
	recordTrainingResult,
} from '$lib/api/sdk.gen';
import type {
	CreateSessionRequest,
	PaginatedSessionsResponse,
	RecordResultRequest,
	SessionSummaryResponse,
	TrainingSessionResponse,
	TrainingStatsResponse,
} from '$lib/api/types.gen';

function requireData<T>(data: T | undefined, label: string): T {
	if (data === undefined) throw new Error(`${label}: empty response`);
	return data;
}

export function useCreateSession() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async (req: CreateSessionRequest) => {
			const { data, error } = await createTrainingSession({ body: req });
			if (error) throw error;
			return requireData(data, 'createTrainingSession') as TrainingSessionResponse;
		},
		onSuccess: () => {
			qc.invalidateQueries({ queryKey: ['training', 'sessions'] });
		},
	}));
}

export function useSession(id: () => string | undefined) {
	return createQuery(() => ({
		queryKey: ['training', 'session', id()],
		queryFn: async () => {
			const resolvedId = id();
			if (!resolvedId) throw new Error('Missing session id');
			const { data, error } = await getTrainingSession({ path: { id: resolvedId } });
			if (error) throw error;
			return requireData(data, 'getTrainingSession') as TrainingSessionResponse;
		},
		enabled: !!id(),
	}));
}

export function useRecordResult() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({ sessionId, body }: { sessionId: string; body: RecordResultRequest }) => {
			const { data, error } = await recordTrainingResult({
				path: { id: sessionId },
				body,
			});
			if (error) throw error;
			return requireData(data, 'recordTrainingResult') as TrainingSessionResponse;
		},
		onSuccess: (_data, variables) => {
			qc.invalidateQueries({ queryKey: ['training', 'session', variables.sessionId] });
		},
	}));
}

export function useCompleteSession() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async (sessionId: string) => {
			const { data, error } = await completeTrainingSession({ path: { id: sessionId } });
			if (error) throw error;
			return requireData(data, 'completeTrainingSession') as SessionSummaryResponse;
		},
		onSuccess: (_data, variables) => {
			qc.invalidateQueries({ queryKey: ['training', 'session', variables] });
			qc.invalidateQueries({ queryKey: ['training', 'sessions'] });
			qc.invalidateQueries({ queryKey: ['training', 'stats'] });
		},
	}));
}

export function useTrainingStats() {
	return createQuery(() => ({
		queryKey: ['training', 'stats'],
		queryFn: async () => {
			const { data, error } = await getTrainingStats();
			if (error) throw error;
			return requireData(data, 'getTrainingStats') as TrainingStatsResponse;
		},
	}));
}

export function useListSessions() {
	return createQuery(() => ({
		queryKey: ['training', 'sessions'],
		queryFn: async () => {
			const { data, error } = await listTrainingSessions();
			if (error) throw error;
			const result = requireData(data, 'listTrainingSessions') as PaginatedSessionsResponse;
			return {
				items: (result.items ?? []) as TrainingSessionResponse[],
				total: result.total,
				page: result.page,
				size: result.size,
			};
		},
	}));
}
