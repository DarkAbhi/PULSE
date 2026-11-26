import type { ComponentProps } from "react";
import type { MealKind } from "@/features/meal-plan/onboarding/onboarding-types";

export type MealSlot = MealKind | "custom";

export const mealSlotColors: Record<MealSlot, string> = {
  breakfast: "[--slot-color:var(--meal-breakfast)]",
  lunch: "[--slot-color:var(--meal-lunch)]",
  evening_snacks: "[--slot-color:var(--meal-snack)]",
  dinner: "[--slot-color:var(--meal-dinner)]",
  custom: "[--slot-color:var(--meal-custom)]",
};

export function MealPlate(props: ComponentProps<"svg">) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...props}>
      <circle cx="12" cy="12" r="5.5" />
      <circle cx="12" cy="12" r="2" />
      <path d="M3 4.5v4a1.5 1.5 0 0 0 3 0v-4M4.5 10v9.5M21 4.5c-1.6 1-2.2 3-2.2 5.5h2.2v9.5" />
    </svg>
  );
}
