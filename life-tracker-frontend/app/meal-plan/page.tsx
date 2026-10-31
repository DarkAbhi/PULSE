
import { getUserTimeZone } from "@/app/lib/user-time-zone";

import { dateKey } from "@/app/lib/time-zone.mjs";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import MealPlanClient from "./meal-plan-client";
import { MealPlan, MealTime } from "./actions";

export const metadata = {
  title: "Meal Plan | PULSE",
};

const apiBaseURL =
  process.env.INTERNAL_API_BASE_URL ??
  "http://localhost:8080";


export default async function MealPlanPage() {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  // Validate session on the server
  const sessionResponse = await fetch(`${apiBaseURL}/api/auth/session`, {
    headers: {
      Cookie: cookieHeader,
    },
  });
  if (!sessionResponse.ok) {
    redirect("/");
  }

  let initialMeals: MealPlan[] = [];
  let initialMealTimes: MealTime[] = [];

  try {
    const [mealsRes, mealTimesRes] = await Promise.all([
      fetch(`${apiBaseURL}/api/meal-plans`, {
        headers: { Cookie: cookieHeader },
        cache: "no-store",
      }),
      fetch(`${apiBaseURL}/api/meal-times`, {
        headers: { Cookie: cookieHeader },
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

  return (
    <MealPlanClient
      initialMeals={initialMeals}
      initialMealTimes={initialMealTimes}
      initialTodayStr={todayStr}
    />
  );
}
