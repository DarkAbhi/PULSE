import { redirect } from "next/navigation";
import { loadMealTimes } from "@/features/meal-plan/server/meal-plan-data";
import { getSession } from "@/lib/auth/session";
import { getUserTimeZone } from "@/lib/user/user-time-zone";
import { dateKey } from "@/lib/time-zone/time-zone.mjs";
import { AppBarNotifications } from "@/features/notifications/server";
import MealPlanGate from "@/features/meal-plan/onboarding/meal-plan-gate";
import { getMealOnboardingAction } from "@/features/meal-plan/onboarding/meal-onboarding-actions";
import { getMealPlansAction } from "@/features/meal-plan/api/meal-plan-actions";
import { mealWeekBounds } from "@/features/meal-plan/utils/meal-week";

export async function MealPlanScreen() {
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
