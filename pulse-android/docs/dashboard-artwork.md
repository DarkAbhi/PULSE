# Dashboard artwork

Generated with the built-in image generation tool. Optimized as WebP for Android; the two card illustrations retain alpha transparency.

## Greeting panel — 19 October 2026

Home uses the shared theme background. The former room image and Pulse branding header have been removed. The greeting is a white rounded surface with a saturated cyan-to-blue-to-magenta glass orb. A single six-second phase drives smooth breathing (0.94–1.04 scale) and three staggered expanding, fading ripple rings, following the supplied PulseOrb example. Animation state is read during drawing rather than recomposing the greeting, and the system motion duration scale disables breathing and rings when animations are removed. Time, date, greeting and display-size name share the panel; notification/profile actions remain available. Ink and gray-violet text keep the white hero readable in either app theme. The layout grows for wrapped names and enlarged text.

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

## Aurora greeting verification — 19 October 2026

- Debug app and instrumentation APK builds, Home formatting checks, all four Home unit tests and Home lint pass (no lint issues).
- All four dashboard device tests pass on both API 35 emulators: 427dp phone and 1280dp tablet, including light/dark, 200% text, exact balances, actions, matching card heights and independent retry. New assertions check the greeting panel and absence of Pulse branding.
- Reviewed light/dark phone screenshots and tablet light/200% text screenshots. Test screenshots now use Android's native UI automation capture after Compose PixelCopy timed out in the initial runs.
- Physical-device animation performance and manual TalkBack were not checked. No new dependencies or backend changes.

## Breathing Pulse orb follow-up — 19 October 2026

Replaced the faint static sphere with the supplied example's saturated cyan/blue/magenta treatment, six-second breathing cycle, three expanding ripple rings and glass highlight. Phone placement reserves white space below the orb; enlarged text moves the orb into the top corner to protect date/greeting contrast. Tablet retains the larger right-hand orb. The drawing uses the existing Compose APIs and respects system motion duration scale without adding dependencies.

Validation: debug app/test builds, Home formatting, all four Home unit tests and Home lint pass. All five dashboard device tests pass on both phone and tablet, including the new frame comparison that verifies visible animation. Native screenshot checks now wait for window transitions and drawing to settle. Reviewed normal and 200% text previews. Physical-device animation performance, changing the system animation setting during use and manual TalkBack were not tested.
