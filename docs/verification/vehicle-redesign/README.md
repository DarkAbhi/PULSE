# Vehicle form redesign verification

Verified on 31 October 2026 using the repository's Android and Go toolchains.

- Go vehicle integration suite passed, including exact/case/space matching,
  invalid inputs, no match, ambiguous matches, authentication and vehicle CRUD.
- Android data, network, garage and design-system unit suites passed (103 tests).
- Kotlin style checks and debug app/test builds passed.
- The garage device suite passed 21 cases on the first unblocked full run.
  Three assertions needed adaptation to the wizard and explicit second-fuel
  controls. All three then passed in focused reruns.
- Six focused vehicle cases passed: catalogue matching, dual-fuel step restoration,
  cancel/discard positioning, garage CRUD, optional pressure persistence, and
  artwork cycling until a vehicle kind is explicitly selected.
- After final spacing changes, the manual-match-to-save flow passed on the tablet
  and at 1080×2400 / 440 dpi with dark mode and 1.4× text. Display settings were
  restored after verification.

The Android 17 debug local-network permission is granted by the garage test
fixture before launching the app, preventing the system prompt from masking it.

Previews use fixture data:

- [Tablet manual match](manual-match.png)
- [Tablet tyre pressure and review](review.png)
- [Dark phone with larger text](phone-dark-review.png)

Deploy the new Go catalogue-match endpoint alongside the app. Year is omitted as
agreed. Existing catalogue migrations 27–28 remain required; this change adds no
database migration.
