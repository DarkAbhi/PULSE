"use server";

import { revalidatePath } from "next/cache";
import { cookies } from "next/headers";

const apiBaseURL =
  process.env.NEXT_PUBLIC_INTERNAL_API_BASE_URL ??
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  "http://localhost:8080";

export type MealTime = {
  id: number;
  name: string;
  start_time: string; // "HH:MM"
  end_time: string;   // "HH:MM"
  is_default: boolean;
  user_id?: number;
};

export type MealPlan = {
  id: number;
  date: string;
  name: string;
  meal_time_id?: number;
  meal_time_name?: string;
  start_time?: string;
  end_time?: string;
  created_at: string;
};

export async function getMealTimesAction() {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  try {
    const res = await fetch(`${apiBaseURL}/api/meal-times`, {
      headers: {
        Cookie: cookieHeader,
      },
      cache: "no-store",
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      return { ok: false, error: err.error ?? "Failed to fetch meal times." };
    }

    const mealTimes = (await res.json()) as MealTime[];
    return { ok: true, mealTimes };
  } catch {
    return { ok: false, error: "Network error while fetching meal times." };
  }
}

export async function createMealTimeAction(name: string, startTime: string, endTime: string) {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  try {
    const res = await fetch(`${apiBaseURL}/api/meal-times`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Cookie: cookieHeader,
      },
      body: JSON.stringify({ name, start_time: startTime, end_time: endTime }),
    });

    const body = await res.json().catch(() => ({}));
    if (!res.ok) {
      return { ok: false, error: body.error ?? "Failed to add meal time." };
    }

    revalidatePath("/meal-plan");
    return { ok: true, mealTime: body as MealTime };
  } catch {
    return { ok: false, error: "Network error while adding meal time." };
  }
}

export async function deleteMealTimeAction(id: number) {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  try {
    const res = await fetch(`${apiBaseURL}/api/meal-times/${id}`, {
      method: "DELETE",
      headers: {
        Cookie: cookieHeader,
      },
    });

    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      return { ok: false, error: body.error ?? "Failed to delete meal time." };
    }

    revalidatePath("/meal-plan");
    return { ok: true };
  } catch {
    return { ok: false, error: "Network error while deleting meal time." };
  }
}

export async function getMealPlansAction(startDate?: string, endDate?: string, date?: string) {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  const query = new URLSearchParams();
  if (date) query.set("date", date);
  if (startDate) query.set("start_date", startDate);
  if (endDate) query.set("end_date", endDate);

  try {
    const res = await fetch(`${apiBaseURL}/api/meal-plans?${query.toString()}`, {
      headers: {
        Cookie: cookieHeader,
      },
      cache: "no-store",
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      return { ok: false, error: err.error ?? "Failed to fetch meal plans." };
    }

    const meals = (await res.json()) as MealPlan[];
    return { ok: true, meals };
  } catch {
    return { ok: false, error: "Network error while fetching meal plans." };
  }
}

export async function addMealPlanAction(date: string, name: string, mealTimeID?: number) {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  try {
    const res = await fetch(`${apiBaseURL}/api/meal-plans`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Cookie: cookieHeader,
      },
      body: JSON.stringify({ date, name, meal_time_id: mealTimeID ?? null }),
    });

    const body = await res.json().catch(() => ({}));
    if (!res.ok) {
      return { ok: false, error: body.error ?? "Failed to add meal." };
    }

    revalidatePath("/meal-plan");
    return { ok: true, meal: body as MealPlan };
  } catch {
    return { ok: false, error: "Network error while adding meal." };
  }
}

export async function deleteMealPlanAction(id: number) {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  try {
    const res = await fetch(`${apiBaseURL}/api/meal-plans/${id}`, {
      method: "DELETE",
      headers: {
        Cookie: cookieHeader,
      },
    });

    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      return { ok: false, error: body.error ?? "Failed to delete meal." };
    }

    revalidatePath("/meal-plan");
    return { ok: true };
  } catch {
    return { ok: false, error: "Network error while deleting meal." };
  }
}
