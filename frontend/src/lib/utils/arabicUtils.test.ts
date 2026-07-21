import { describe, expect, it } from 'vitest';
import { removeArabicDiacritics } from './arabicUtils';

describe('removeArabicDiacritics', () => {
	it('returns plain text unchanged', () => {
		expect(removeArabicDiacritics('hello')).toBe('hello');
	});

	it('returns empty string unchanged', () => {
		expect(removeArabicDiacritics('')).toBe('');
	});

	it('strips fatha (U+064E)', () => {
		// كَ → ك
		expect(removeArabicDiacritics('كَ')).toBe('ك');
	});

	it('strips kasra (U+0650)', () => {
		// كِ → ك
		expect(removeArabicDiacritics('كِ')).toBe('ك');
	});

	it('strips damma (U+064F)', () => {
		// كُ → ك
		expect(removeArabicDiacritics('كُ')).toBe('ك');
	});

	it('strips shadda (U+0651)', () => {
		// كّ → ك
		expect(removeArabicDiacritics('كّ')).toBe('ك');
	});

	it('strips sukun (U+0652)', () => {
		// كْ → ك
		expect(removeArabicDiacritics('كْ')).toBe('ك');
	});

	it('strips tanwin fath (U+064B)', () => {
		// كً → ك
		expect(removeArabicDiacritics('كً')).toBe('ك');
	});

	it('strips superscript alef (U+0670)', () => {
		// ٰ
		expect(removeArabicDiacritics('كٰ')).toBe('ك');
	});

	it('strips all diacritics from a fully vowelised word', () => {
		// كَتَبَ → كتب
		expect(removeArabicDiacritics('كَتَبَ')).toBe('كتب');
	});

	it('strips diacritics from a sentence, preserving spaces and letters', () => {
		// بِسْمِ اللَّهِ → بسم الله
		const input = 'بِسْمِ اللَّهِ';
		const expected = 'بسم الله';
		expect(removeArabicDiacritics(input)).toBe(expected);
	});

	it('preserves non-Arabic scripts mixed with Arabic', () => {
		// Latin mixed in
		expect(removeArabicDiacritics('abc كَتَبَ xyz')).toBe('abc كتب xyz');
	});
});
