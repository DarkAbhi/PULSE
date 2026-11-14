import { apiFetch } from "@/app/lib/server-api";
import { getUserTimeZone } from "@/app/lib/user-time-zone";

import { dateKey } from "@/app/lib/time-zone.mjs";
import { redirect } from "next/navigation";
import MealPlanGate from "./onboarding/meal-plan-gate";
import { getMealOnboardingAction } from "./onboarding/actions";
import { MealPlan, MealTime } from "./actions";

export const metadata = {
  title: "Meal Plan | PULSE",
};

const apiBaseURL =
  process.env.INTERNAL_API_BASE_URL ??
  "http://localhost:8080";

export default async function MealPlanPage() {
  // Validate session on the server
  const sessionResponse = await apiFetch(`${apiBaseURL}/api/auth/session`);
  if (!sessionResponse.ok) {
    redirect("/");
  }
  const session = await sessionResponse.json() as { username: string };

  let initialMeals: MealPlan[] = [];
  let initialMealTimes: MealTime[] = [];
  const onboardingPromise = getMealOnboardingAction();

  try {
    const [mealsRes, mealTimesRes] = await Promise.all([
      apiFetch(`${apiBaseURL}/api/meal-plans`, {
        cache: "no-store",
      }),
      apiFetch(`${apiBaseURL}/api/meal-times`, {
        cache: "no-store",
      }),
    ]);

    if (mealsRes.ok) {
      initialMeals = (await mealsRes.json()) as MealPlan[];
    }
    if (mealTimesRes.ok) {
      initialMealTimes = (await mealTimesRes.json()) as MealTime[];
    }
  } catch {
    // If backend is unreachable, client will handle gracefully
  }

  const todayStr = dateKey(new Date(), await getUserTimeZone());
  const onboarding = await onboardingPromise;

  return (
    <MealPlanGate
      key={session.username}
      initialState={onboarding.ok ? onboarding.state : null}
      initialMeals={initialMeals}
      initialMealTimes={initialMealTimes}
      initialTodayStr={todayStr}
    />
  );
}
