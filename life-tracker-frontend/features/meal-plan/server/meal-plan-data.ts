import "server-only";
import { apiBaseURL, apiFetch } from "@/lib/api/server-api";
import type { MealTime } from "@/features/meal-plan/api/meal-plan-actions";

export async function loadMealTimes(): Promise<MealTime[]> {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/meal-times`, { signal: AbortSignal.timeout(10_000) });
    return response.ok ? (await response.json()) ?? [] : [];
  } catch {
    return [];
  }
}
