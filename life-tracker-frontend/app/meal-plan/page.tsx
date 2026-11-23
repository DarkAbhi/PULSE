import { redirect } from "next/navigation";
import { apiBaseURL, apiFetch } from "@/app/lib/server-api";
import { getSession } from "@/app/lib/session";
import { getUserTimeZone } from "@/app/lib/user-time-zone";
import { dateKey } from "@/app/lib/time-zone.mjs";
import { AppBarNotifications } from "@/app/components/top-app-bar";
import MealPlanGate from "@/app/meal-plan/onboarding/meal-plan-gate";
import { getMealOnboardingAction } from "@/app/meal-plan/onboarding/actions";
import { getMealPlansAction, type MealTime } from "@/app/meal-plan/actions";
import { mealWeekBounds } from "@/app/meal-plan/week";

export const metadata = { title: "Meal Plan | PULSE" };

async function loadMealTimes(): Promise<MealTime[]> {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/meal-times`, { signal: AbortSignal.timeout(10_000) });
    return response.ok ? (await response.json()) ?? [] : [];
  } catch {
    return [];
  }
}

export default async function MealPlanPage() {
  const [session, timeZone] = await Promise.all([getSession(), getUserTimeZone()]);
  if (!session) redirect("/");

  const today = dateKey(new Date(), timeZone);
  const { start, end } = mealWeekBounds(today);
  const [onboarding, meals, mealTimes] = await Promise.all([
    getMealOnboardingAction(),
    getMealPlansAction(start, end),
    loadMealTimes(),
  ]);

  return (
    <MealPlanGate
      notifications={<AppBarNotifications />}
      key={session.username}
      initialState={onboarding.ok ? onboarding.state : null}
      initialMeals={meals.ok ? meals.meals ?? [] : null}
      initialMealTimes={mealTimes}
      initialTodayStr={today}
    />
  );
}
