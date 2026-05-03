import { render, screen } from '@testing-library/svelte';
import { describe, expect, it } from 'vitest';
import Markdown from './Markdown.svelte';

describe('Markdown', () => {
	it('renders markdown content as HTML inside prose wrapper', () => {
		const { container } = render(Markdown, {
			props: {
				content: '## Heading\n\n**bold** text',
				class: 'extra-class',
			},
		});

		expect(container.firstElementChild).toHaveClass('prose', 'extra-class');
		expect(screen.getByRole('heading', { level: 2, name: 'Heading' })).toBeInTheDocument();
		expect(container.querySelector('strong')).toHaveTextContent('bold');
	});
});
