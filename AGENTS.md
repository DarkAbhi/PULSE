# Repository Instructions

## Next.js Frontend

These rules apply to all files within `life-tracker-frontend/`.

### Import Conventions

- ALWAYS use the `@/` path alias for imports of
  application files within the frontend.
- NEVER introduce relative imports (`./` or `../`)
  for application modules.
- This applies to components, hooks, utilities,
  types, services, constants, and styles.
- Preserve package imports such as `react`,
  `next/navigation`, and third-party libraries.
- When modifying files, convert any affected
  relative application imports to `@/` aliases.
- Ensure all new imports comply with the existing
  TypeScript path configuration.

### Examples

Incorrect:
  import { Button } from "../../components/ui/Button";
  import { useAuth } from "../hooks/useAuth";

Correct:
  import { Button } from "@/components/ui/Button";
  import { useAuth } from "@/hooks/useAuth";