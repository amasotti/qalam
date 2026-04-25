import { createMutation, createQuery, useQueryClient } from '@tanstack/svelte-query';
import {
	addWordLink,
	createAnnotation,
	deleteAnnotation,
	listAnnotations,
	removeWordLink,
	updateAnnotation,
} from '$lib/api/sdk.gen';
import type {
	AnnotationResponse,
	CreateAnnotationRequest,
	UpdateAnnotationRequest,
} from '$lib/api/types.gen';

function requireData<T>(data: T | undefined, label: string): T {
	if (data === undefined) throw new Error(`${label}: empty response`);
	return data;
}

export function useTextAnnotations(textId: () => string | undefined) {
	return createQuery(() => ({
		queryKey: ['annotations', textId()],
		queryFn: async () => {
			const id = textId();
			if (!id) throw new Error('Missing textId');
			const { data, error } = await listAnnotations({ path: { textId: id } });
			if (error) throw error;
			return requireData(data, 'listAnnotations') as AnnotationResponse[];
		},
		enabled: !!textId(),
	}));
}

export function useCreateAnnotation() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({ textId, body }: { textId: string; body: CreateAnnotationRequest }) => {
			const { data, error } = await createAnnotation({ path: { textId }, body });
			if (error) throw error;
			return requireData(data, 'createAnnotation') as AnnotationResponse;
		},
		onSuccess: (
			_data: AnnotationResponse,
			variables: { textId: string; body: CreateAnnotationRequest }
		) => {
			qc.invalidateQueries({ queryKey: ['annotations', variables.textId] });
		},
	}));
}

export function useUpdateAnnotation() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({
			textId,
			id,
			body,
		}: {
			textId: string;
			id: string;
			body: UpdateAnnotationRequest;
		}) => {
			const { data, error } = await updateAnnotation({ path: { textId, id }, body });
			if (error) throw error;
			return requireData(data, 'updateAnnotation') as AnnotationResponse;
		},
		onSuccess: (
			_data: AnnotationResponse,
			variables: { textId: string; id: string; body: UpdateAnnotationRequest }
		) => {
			qc.invalidateQueries({ queryKey: ['annotations', variables.textId] });
		},
	}));
}

export function useDeleteAnnotation() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({ textId, id }: { textId: string; id: string }) => {
			const { error } = await deleteAnnotation({ path: { textId, id } });
			if (error) throw error;
		},
		onSuccess: (_data: unknown, variables: { textId: string; id: string }) => {
			qc.invalidateQueries({ queryKey: ['annotations', variables.textId] });
		},
	}));
}

export function useAddWordLink() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({
			textId,
			annotationId,
			wordId,
		}: {
			textId: string;
			annotationId: string;
			wordId: string;
		}) => {
			const { data, error } = await addWordLink({
				path: { textId, id: annotationId },
				body: { wordId },
			});
			if (error) throw error;
			return requireData(data, 'addWordLink') as AnnotationResponse;
		},
		onSuccess: (
			_data: AnnotationResponse,
			variables: { textId: string; annotationId: string; wordId: string }
		) => {
			qc.invalidateQueries({ queryKey: ['annotations', variables.textId] });
			qc.invalidateQueries({ queryKey: ['words', variables.wordId, 'annotations'] });
		},
	}));
}

export function useRemoveWordLink() {
	const qc = useQueryClient();
	return createMutation(() => ({
		mutationFn: async ({
			textId,
			annotationId,
			wordId,
		}: {
			textId: string;
			annotationId: string;
			wordId: string;
		}) => {
			const { data, error } = await removeWordLink({ path: { textId, id: annotationId, wordId } });
			if (error) throw error;
			return requireData(data, 'removeWordLink') as AnnotationResponse;
		},
		onSuccess: (
			_data: AnnotationResponse,
			variables: { textId: string; annotationId: string; wordId: string }
		) => {
			qc.invalidateQueries({ queryKey: ['annotations', variables.textId] });
			qc.invalidateQueries({ queryKey: ['words', variables.wordId, 'annotations'] });
		},
	}));
}
