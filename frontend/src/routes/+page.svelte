<script lang="ts">
	import { Button } from '$lib/components/ui/button';
	import { Badge } from '$lib/components/ui/badge';
	import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '$lib/components/ui/card';
</script>

<main class="min-h-screen bg-background p-8">
	<div class="mx-auto max-w-3xl space-y-8">

		<!-- Stack verification -->
		<div>
			<h1 class="text-3xl font-bold text-foreground">قلم — Design System</h1>
			<p class="text-muted-foreground mt-1">Font stack + component verification</p>
		</div>

		<!-- Arabic font stack demo -->
		<Card>
			<CardHeader>
				<CardTitle>Arabic Fonts</CardTitle>
				<CardDescription>Three roles — body, display, text passages</CardDescription>
			</CardHeader>
			<CardContent class="space-y-6">

				<!-- Body font: Noto Naskh Arabic -->
				<div class="space-y-1">
					<p class="text-xs text-muted-foreground uppercase tracking-widest">
						Body · Noto Naskh Arabic (font-arabic)
					</p>
					<p class="arabic">
						كَتَبَ الطالِبُ رِسالةً إلى أستاذِهِ في موضوعِ النَّحوِ العربيِّ القديمِ.
					</p>
					<p class="transliteration">kataba ṭ-ṭālibu risālatan ilā ustādhihi fī mawḍūʿi n-naḥwi l-ʿarabiyyi l-qadīm</p>
					<p class="text-sm text-muted-foreground italic">
						The student wrote a letter to his professor on the subject of classical Arabic grammar.
					</p>
				</div>

				<!-- Display font: Lateef -->
				<div class="space-y-1">
					<p class="text-xs text-muted-foreground uppercase tracking-widest">
						Display · Lateef (font-arabic-display)
					</p>
					<p class="arabic-display">
						القَلَمُ أقوى مِن السَّيف
					</p>
					<p class="transliteration">al-qalamu ʾaqwā min as-sayf</p>
					<p class="text-sm text-muted-foreground italic">
						The pen is mightier than the sword.
					</p>
				</div>

				<!-- Text / passage font: Markazi Text -->
				<div class="space-y-1">
					<p class="text-xs text-muted-foreground uppercase tracking-widest">
						Text passage · Markazi Text (font-arabic-text)
					</p>
					<p class="arabic-text">
						في البَدءِ كانَ الكَلِمةُ، وكانَتِ الكَلِمةُ عِندَ اللهِ، وكانَتِ الكَلِمةُ اللهَ.
					</p>
					<p class="transliteration">fī l-badʾi kāna l-kalimatu, wa-kānati l-kalimatu ʿinda llāhi, wa-kānati l-kalimatu llāha</p>
					<p class="text-sm text-muted-foreground italic">
						In the beginning was the Word, and the Word was with God, and the Word was God.
					</p>
				</div>

			</CardContent>
		</Card>

		<!-- Component spot check -->
		<Card>
			<CardHeader>
				<CardTitle>Components</CardTitle>
			</CardHeader>
			<CardContent class="space-y-4">
				<div class="flex gap-2 flex-wrap">
					<Badge>Default</Badge>
					<Badge variant="secondary">Secondary</Badge>
					<Badge variant="outline">Outline</Badge>
					<Badge variant="destructive">Destructive</Badge>
				</div>
				<div class="flex gap-2 flex-wrap">
					<Button>Primary</Button>
					<Button variant="secondary">Secondary</Button>
					<Button variant="outline">Outline</Button>
					<Button variant="ghost">Ghost</Button>
					<Button variant="destructive">Danger</Button>
				</div>
			</CardContent>
		</Card>

	</div>
</main>
