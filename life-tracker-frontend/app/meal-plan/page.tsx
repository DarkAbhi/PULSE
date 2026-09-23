import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import MealPlanClient from "./meal-plan-client";
import { MealPlan, MealTime } from "./actions";

export const metadata = {
  title: "Meal Plan | Life Tracker",
};

const apiBaseURL =
  process.env.NEXT_PUBLIC_INTERNAL_API_BASE_URL ??
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  "http://localhost:8080";

const APP_TIMEZONE = "Asia/Kolkata";

function getTodayYMD(tz: string = APP_TIMEZONE): string {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: tz,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date());
}

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

  const todayStr = getTodayYMD();

  return (
    <MealPlanClient
      initialMeals={initialMeals}
      initialMealTimes={initialMealTimes}
      initialTodayStr={todayStr}
    />
  );
}
