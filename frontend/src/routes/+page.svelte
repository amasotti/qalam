<script lang="ts">
	import { Button } from '$lib/components/ui/button';
	import { Badge } from '$lib/components/ui/badge';
	import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
</script>

<main class="min-h-screen bg-background p-8">
	<div class="mx-auto max-w-2xl space-y-6">
		<div>
			<h1 class="text-3xl font-bold text-foreground">قلم</h1>
			<p class="text-muted-foreground mt-1">Arabic learning tool — design system check</p>
		</div>

		<Card>
			<CardHeader>
				<CardTitle>Tailwind v4 + shadcn-svelte</CardTitle>
			</CardHeader>
			<CardContent class="space-y-4">
				<p class="text-sm text-muted-foreground">
					If you can read this inside a card with styled text, the stack is working.
				</p>
				<div class="flex gap-2 flex-wrap">
					<Badge>Default</Badge>
					<Badge variant="secondary">Secondary</Badge>
					<Badge variant="outline">Outline</Badge>
					<Badge variant="destructive">Destructive</Badge>
				</div>
				<div class="flex gap-2">
					<Button>Primary</Button>
					<Button variant="secondary">Secondary</Button>
					<Button variant="outline">Outline</Button>
					<Button variant="destructive">Danger</Button>
				</div>
			</CardContent>
		</Card>
	</div>
</main>
