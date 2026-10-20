# Add exercise redesign

The existing Kotlin/Compose exercise editor now groups exercise identity, catalogue metadata, instructions, and set logging into distinct sections using Pulse colors, typography, spacing, shapes, and controls.

## Interaction

- Start from the search surface and a compact empty state; set entry appears after choosing a catalogue exercise or accepting a custom name.
- Search opens a bounded, centered tablet dialog and a near-full-screen compact-window dialog. Results select immediately. Search, clear, retry, custom names, selection status, and equipment/muscle metadata use the existing catalogue data.
- Replacement queries use the same debounced ViewModel search job without editing the current draft. Dismissal preserves the name, match, alias preference, and every raw set value.
- The warm identity card provides Change, View instructions (when data exists), and Edit name. Catalogue unlinking is available with the name editor. An entered alias is distinguished from its catalogue name.
- Instructions use a reusable responsive dialog, also shared by workout detail. Only supported numbered instructions, muscle information, and equipment appear.
- Set rows retain presentation keys when preceding rows are removed. Numeric keyboards and focus traversal remain available. Larger text stacks fields as needed. Compose motion uses Pulse's duration and the platform animation scale.
- The remember-name checkbox remains exclusive to catalogue-linked drafts and preserves its existing alias semantics.
- The single save button uses the existing domain validator to show availability. Submission, parsing, precision, limits, backend contracts, and persistence remain unchanged.

## Scope

UI-only draft metadata retains the selected catalogue record while replacement search results change. The ViewModel adds a query entry point to the existing search implementation; it does not add another search pipeline. Domain models, APIs, repositories, and persistence schemas are untouched.

## Verification

Fitness unit tests: 26 passed. Domain unit tests: 14 passed. Feature lint: no issues. Kotlin formatting checks and debug app/test packaging passed.

The final focused Android suite passed all eight tests on each emulator (16 device test executions). Device verification covers phone and tablet, initial/custom/catalogue states, immediate selection, instructions dismissal, replacement cancellation with raw invalid values retained, alias preference, add/remove/max sets, invalid repetitions, save availability, keyboard interactions, 160% font size, dark theme, configuration restoration, resizing, and real fixture-backed saving.

Screenshots are in `phone/` and `tablet/`: `empty.png`, `search.png`, `selected.png`, `instructions.png`, and `large-type-dark.png`.

- [Phone selected exercise](phone/selected.png)
- [Tablet selected exercise](tablet/selected.png)
- [Tablet instructions](tablet/instructions.png)
- [Phone large text and dark theme](phone/large-type-dark.png)

Manual TalkBack exploration and physical-device testing were not performed. An older combined tabs/logout test also timed out after returning from profile to Fitness; the focused editor rotation-and-save check covers configuration retention separately.
