# Planned purchase form

The form reuses Pulse typography, spacing, colors, fields and actions. A centered 560 dp maximum width adapts to phones, tablets, split-screen and landscape without stretching inputs or adding a redundant summary. The existing screen supplies scrolling and IME insets.

The purchase title lives in the navigation bar beside Back, matching the fixed-obligation editor. The duplicate in-content heading, bag badge and subtitle are removed. The inactive quick-actions overflow is hidden while a purchase draft is open and returns on the Financial Horizon list. The planning-period card is removed so the form starts directly with the purchase fields.

The amount uses display-only grouping with explicit cursor-offset mapping. INR uses Indian grouping and a rupee prefix; other account currencies retain their configured symbol and standard grouping. Drafts and requests retain unformatted decimal values. Existing zero, maximum-value and precision rules remain intact.

The optional link validates after blur or submission, clears stale errors when corrected, preserves the complete URL, and offers a clear action. Submission focuses the first invalid field. Existing ViewModel loading, duplicate-submission protection, saved-state persistence, uncertain-save recovery, success feedback and discard confirmation are retained.

## API constraints

`PlannedPurchasesDto` returns only `items` and `total`; purchase items contain `id`, `name`, `price` and `url`. The overview projections contain relative horizons rather than a resolved planning month. The Go purchase service resolves the target month using server time and the account timezone but does not return it. The planning-period card is omitted; the server continues to determine the target month without any client-side date calculation.

The existing purchase routes support list, create, delete and clear, with no update route. The Android purchase change also has no ID. Existing purchase creation and deletion remain available; this redesign does not add an unsupported edit operation or simulate one by deleting and recreating purchases. An actual edit flow and a resolved calendar label require API support outside this UI change.

## Verification

`PurchaseInputTest` covers Indian and standard grouping, cursor mapping, precision and limits, optional/valid/invalid/long links, Unicode name limits and lossless draft persistence. `HorizonDeviceTest` includes purchase creation/deletion, inline validation and focus, optional link clearing, recreation, and responsive phone/tablet/landscape/large-text checks.

Verified: 23 Horizon unit tests, all three purchase device scenarios, APK builds, and the Horizon/core UI/app style checks passed. The broader app unit suite passes 13 of 14 tests; its existing localization test flags the unchanged `PulseText("›")` in `HorizonLedger.kt:193`. No tests were removed or weakened.

The planning-card removal compiles and passes app/Horizon style checks. Its responsive replay stopped at an emulator PixelCopy screenshot timeout on both attempts, so the saved screenshots below show the preceding iteration with the card still present.

Previous screenshots: [phone](phone.png), [tablet](tablet.png), [landscape](landscape.png), [1.8× text](large-text.png). Phone and landscape forms scroll.

## Planned purchases tab redesign

The tab now follows the supplied reference with a warm gold summary, soft layered cloud curves, a visible Add purchase action, accurate singular/plural counts, and rounded purchase cards. Sorting is omitted. Wide cards place the amount beside the purchase details; phone and large-text layouts stack content. All content uses Pulse theme colors, including dark-mode colors, and headings expose accessibility semantics. Decorative icons are excluded from accessibility announcements.

The empty state includes a shopping-cart illustration, a short explanation of forecast planning, and a Plan your first purchase action. Delete and clear actions appear only when purchases exist, retain confirmation dialogs, and respect the existing saving/review safeguards. The summary keeps the server-provided total and next-month semantics. Optional saved links remain visible; no misleading add-link or edit button is shown because updates are unsupported by the current API.

The purchase interaction test now exercises both in-tab create actions, count updates, individual deletion and clearing, the empty state, and absence of sorting. Its purchase fixture calculates numeric totals from the actual records instead of returning a fixed amount.

Previews: [populated tablet](list-filled.png), [empty tablet](list-empty.png).

Tab validation: Android debug and test APK builds, all 23 Horizon unit tests, Horizon style checks, the purchase add/delete/clear device test, and the phone/tablet/1.8× text device test passed. Screenshots were inspected for the populated and empty tablet states plus phone and large-text layouts. At 1.8× text the existing app-wide bottom navigation wraps its labels; purchase content remains scrollable and its actions are reachable.

Responsive previews: [phone](list-phone.png), [tablet](list-tablet.png), [large text](list-large-text.png).

## Reference polish

The summary has no outline. Its badge sits beside the complete text group on wide screens, with a small, tracked uppercase eyebrow, a 28 sp bold total, and quieter 14 sp supporting text. Purchase titles and the section count use 18 sp bold text. These use Pulse’s existing DM Sans family, with sizes, weights and placement adapted to the supplied reference. Phone and large-text layouts stack without truncating the amount.

Two scalable cubic-bezier cloud silhouettes replace the oversized circles. Their translucent gold fill respects light and dark theme colors. New cart, plus and trash vector drawables stay crisp at any display density. The card delete action is a bordered icon button beside the amount with a purchase-specific accessibility label; the clear action includes a trash icon.

The shared PulseButton accepts an optional leadingIcon slot. Its Text style reuses PulseButtonVariant.Destructive for destructive actions; existing callers retain their appearance. Loading buttons show their spinner instead of the icon. Add purchase and Plan your first purchase use the plus icon; Clear planned purchases uses the trash icon.
Polish validation: debug/test APK builds and 23 Horizon unit tests passed; core UI and Horizon style checks passed. Phone/tablet/1.8× text and purchase add/delete/clear device scenarios passed. One action-test run timed out during ActivityScenario teardown; the unchanged scenario passed on retry. Updated screenshots were visually inspected for typography, border removal, cloud contours, icons and empty-state guidance.
