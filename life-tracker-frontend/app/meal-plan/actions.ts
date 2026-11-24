"use server";

import { apiBaseURL, apiFetch } from "@/app/lib/server-api";
import { revalidatePath } from "next/cache";


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
  recipe_id?: string;
  meal_time_id?: number;
  meal_time_name?: string;
  start_time?: string;
  end_time?: string;
  is_consumed: boolean;
  created_at: string;
};

export async function getMealTimesAction() {
  try {
    const res = await apiFetch(`${apiBaseURL}/api/meal-times`, {
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
  try {
    const res = await apiFetch(`${apiBaseURL}/api/meal-times`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
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
  try {
    const res = await apiFetch(`${apiBaseURL}/api/meal-times/${id}`, {
      method: "DELETE",
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
  const query = new URLSearchParams();
  if (date) query.set("date", date);
  if (startDate) query.set("start_date", startDate);
  if (endDate) query.set("end_date", endDate);

  try {
    const res = await apiFetch(`${apiBaseURL}/api/meal-plans?${query.toString()}`, {
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

export async function addMealPlanAction(date: string, name: string, mealTimeID?: number, recipeID?: string) {
  try {
    const res = await apiFetch(`${apiBaseURL}/api/meal-plans`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ date, name, meal_time_id: mealTimeID ?? null, recipe_id: recipeID }),
    });

    const body = await res.json().catch(() => ({}));
    if (!res.ok) {
      return { ok: false, error: body.error ?? "Failed to add meal." };
    }

    revalidatePath("/meal-plan");
    revalidatePath("/quests");
    return { ok: true, meal: body as MealPlan };
  } catch {
    return { ok: false, error: "Network error while adding meal." };
  }
}

export async function deleteMealPlanAction(id: number) {
  try {
    const res = await apiFetch(`${apiBaseURL}/api/meal-plans/${id}`, {
      method: "DELETE",
    });

    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      return { ok: false, error: body.error ?? "Failed to delete meal." };
    }

    revalidatePath("/meal-plan");
    revalidatePath("/quests");
    return { ok: true };
  } catch {
    return { ok: false, error: "Network error while deleting meal." };
  }
}

export async function updateMealPlanConsumedAction(id: number, isConsumed: boolean) {
  try {
    const res = await apiFetch(`${apiBaseURL}/api/meal-plans/${id}/consumed`, {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ is_consumed: isConsumed }),
    });

    const body = await res.json().catch(() => ({}));
    if (!res.ok) {
      return { ok: false, error: body.error ?? "Failed to update meal status." };
    }

    revalidatePath("/meal-plan");
    revalidatePath("/quests");
    return { ok: true, meal: body as MealPlan };
  } catch {
    return { ok: false, error: "Network error while updating meal status." };
  }
}

export async function updateMealPlanAction(
  id: number,
  input: { name?: string; date?: string; meal_time_id?: number; is_consumed?: boolean }
) {
  try {
    const res = await apiFetch(`${apiBaseURL}/api/meal-plans/${id}`, {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(input),
    });

    const body = await res.json().catch(() => ({}));
    if (!res.ok) {
      return { ok: false, error: body.error ?? "Failed to update meal." };
    }

    revalidatePath("/meal-plan");
    revalidatePath("/quests");
    return { ok: true, meal: body as MealPlan };
  } catch {
    return { ok: false, error: "Network error while updating meal." };
  }
}

export type RecipeSummary = {
  id: string;
  name: string;
  dish_type: string;
  is_ready_to_eat: boolean;
  ready_in_minutes?: number;
};

export async function searchRecipesAction(search: string, page: number) {
  const query = new URLSearchParams({ q: search, page: String(page) });
  try {
    const response = await apiFetch(`${apiBaseURL}/api/meal-recipes?${query}`, { cache: "no-store" });
    if (response.ok) return await response.json() as { recipes: RecipeSummary[]; has_more: boolean };
  } catch {
    // Render the same retry state for both API and network errors.
  }
  return null;
}
