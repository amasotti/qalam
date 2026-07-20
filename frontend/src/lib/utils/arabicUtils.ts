const ARABIC_DIACRITICS =
	/[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06DC\u06DF-\u06E8\u06EA-\u06ED]/g;

type DiacriticRemover = (s: string) => string;

export const removeArabicDiacritics: DiacriticRemover = (arText: string) => {
	return arText.replace(ARABIC_DIACRITICS, '');
};
