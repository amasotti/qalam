import {
	createRoot,
	deleteRoot,
	getRootById,
	listRoots,
	listWords,
	normalizeRoot,
	updateRoot,
} from '$lib/api/sdk.gen';
import type {
	CreateRootRequest,
	RootResponse,
	UpdateRootRequest,
	WordResponse,
} from '$lib/api/types.gen';
import { createMutation, createQuery, useQueryClient } from '@tanstack/svelte-query';

function requireData<T>(data: T | undefined, label: string): T {
	if (data === undefined) throw new Error(`${label}: empty response`);
	return data;
}

/** Fetch all roots in one shot — personal tool, ≤ a few hundred. */
export function useAllRoots() {
	return createQuery(() => ({
		queryKey: ['roots', 'all'],
		queryFn: async () => {
			const { data, error } = await listRoots({ query: { size: 500 } });
			if (error) throw error;
			return (requireData(data, 'listRoots').items ?? []) as RootResponse[];
		},
	}));
}

export function useRoot(id: () => string | undefined) {
	return createQuery(() => ({
		queryKey: ['roots', id()],
		queryFn: async () => {
			const resolvedId = id();
			if (!resolvedId) throw new Error('Missing root id');
			const { data, error } = await getRootById({ path: { id: resolvedId } });
			if (error) throw error;
			return requireData(data, 'getRootById');
		},
		enabled: !!id(),
	}));
}

/**
 * Fetch words belonging to a root.
 * Backend has no rootId filter — load all and filter client-side.
 */
export function useWordsForRoot(rootId: () => string | undefined) {
	return createQuery(() => ({
		queryKey: ['words', 'forRoot', rootId()],
		queryFn: async () => {
			const resolved = rootId();
			const { data, error } = await listWords({ query: { size: 500 } });
			if (error) throw error;
			const all = (requireData(data, 'listWords').items ?? []) as WordResponse[];
			return all.filter((w) => w.rootId === resolved);
		},
		enabled: !!rootId(),
	}));
}

export function useCreateRoot() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async (req: CreateRootRequest) => {
			const { data, error } = await createRoot({ body: req });
			if (error) throw error;
			return requireData(data, 'createRoot');
		},
		onSuccess: () => {
			qc.invalidateQueries({ queryKey: ['roots'] });
		},
	}));
}

export function useUpdateRoot() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({ id, body }: { id: string; body: UpdateRootRequest }) => {
			const { data, error } = await updateRoot({ path: { id }, body });
			if (error) throw error;
			return requireData(data, 'updateRoot');
		},
		onSuccess: () => {
			qc.invalidateQueries({ queryKey: ['roots'] });
		},
	}));
}

export function useDeleteRoot() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async (id: string) => {
			const { error } = await deleteRoot({ path: { id } });
			if (error) throw error;
		},
		onSuccess: () => {
			qc.invalidateQueries({ queryKey: ['roots'] });
		},
	}));
}

export function useNormalizeRoot() {
	return createMutation(() => ({
		mutationFn: async (input: string) => {
			const { data, error } = await normalizeRoot({ body: { input } });
			if (error) throw error;
			return requireData(data, 'normalizeRoot');
		},
	}));
}
