export type MealKind = "breakfast" | "lunch" | "dinner";
export type MealPreferences = {
  goal: string;
  barriers: string[];
  dietary_pattern: string;
  food_avoidances: string[];
  meals: { kind: MealKind; usual_time: string }[];
};
export type OnboardingState = {
  status: "pending" | "skipped" | "completed";
  questionnaire_version: number;
  decided_at?: string;
  preferences: MealPreferences | null;
};
export type PreferenceResult = { ok: true; state: OnboardingState } | { ok: false; error: string; state?: OnboardingState };
