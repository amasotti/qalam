export function formatTrainingMode(mode: string): string {
	return mode.charAt(0) + mode.slice(1).toLowerCase();
}

export function formatTrainingDate(value: string): string {
	return new Intl.DateTimeFormat('en-GB', {
		day: 'numeric',
		month: 'short',
		year: 'numeric',
	}).format(new Date(value));
}

export function formatAccuracy(accuracy: number): string {
	return `${Math.round(accuracy * 100)}%`;
}
