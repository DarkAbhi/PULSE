# Dashboard artwork

Generated with the built-in image generation tool. Optimized as WebP for Android; the two card illustrations retain alpha transparency.

## room

Use case: illustration-story. Asset type: background artwork for a native Android personal dashboard. Create a warm sunlit room, soft hand-painted editorial illustration matching the supplied reference mood: creamy plaster wall filling upper 75 percent as quiet empty space, golden afternoon window light diagonally across wall, a wooden low bench and black motorcycle helmet at bottom left behind a lush leafy houseplant, window frame at far right, small framed green mountain landscape on upper far right. Wide landscape composition 3:2, soft ivory and honey palette, subtle paper texture, polished cozy illustration. No text, no logos, no UI, no people. Leave center and upper left mostly empty cream wall for readable native text. Save as dashboard room background.

Asset: `feature/home/src/main/res/drawable-nodpi/dashboard_room.webp`

## cat

Use case: illustration-story. Asset type: transparent decorative illustration for an Android dashboard finance card. A cute relaxed cream calico cat stretching asleep on a plump golden yellow oval floor cushion, tail lifted, small dark ink outlines and soft hand-painted shading. Three-quarter side view. Cozy editorial illustration, honey yellow, cream, charcoal patches, subtle peach ears. Entire cat and cushion in frame, isolated with actual transparent background. No text, no UI, no logos, no surrounding room. Wide 3:2 composition.

Asset: `feature/home/src/main/res/drawable-nodpi/dashboard_cat.webp`

## fitness

Use case: illustration-story. Asset type: transparent decorative illustration for an Android fitness dashboard card. One charcoal black dumbbell in front, a deep forest green reusable water bottle with black screw cap and loop behind it, folded cream gym towel beside it. Entire group in frame isolated on actual transparent background. Cozy hand-painted editorial illustration with clean dark ink outlines and soft shading, cream and muted sage accents, matching a cute calico cat illustration in a sunlit room. Wide 3:2 composition, no text, no logos, no UI, no extra objects.

Asset: `feature/home/src/main/res/drawable-nodpi/dashboard_fitness.webp`

## Verification — 18 October 2026

- Debug app and instrumentation APKs build; dashboard lint reports no issues; formatting passes.
- 17 app/dashboard unit tests pass, including module boundaries, localization resources and existing exact-decimal/date checks.
- Four dashboard device tests pass on both phone and tablet: light-theme actions/exact amount, dark-theme readability, 200% text with scrolling, and independent retry.
- Three existing integration tests pass: login/notifications/logout, partial dashboard failure/retry, and Home-to-Horizon navigation.
- Light/dark and large-text screenshots were inspected on both emulators. Fixtures contain no live account data. Physical devices and manual TalkBack remain unverified.

## Responsive layout correction

Both cards use the tallest intrinsic content height at the current column width, in either stacked or side-by-side layouts. Footer artwork and actions align at the bottom without clipping wrapped text. Home draws behind the status bar on phone and tablet, and behind the bottom system bar on tablet. Scrollable content still respects the top and bottom safe-area insets.

Validation: build, dashboard lint and 17 app/dashboard unit tests pass. All four dashboard device tests pass on phone and tablet, now asserting equal heights in light, dark, 200% text and retry states. The full login/navigation test asserts that the background top matches the root window top on phone and tablet, and also checks the bottom edge on tablet.

Status-bar follow-up: the debug build and phone login/navigation regression pass with the new top-edge assertion. The tablet emulator disconnected during its run, so the updated tablet status-bar check has not completed.
