"use server";

import { revalidatePath } from "next/cache";
import { apiBaseURL, apiFetch } from "@/app/lib/server-api";

function optionalTarget(form: FormData, key: string) {
  const value = form.get(key);
  if (typeof value !== "string" || !value.trim()) return null;
  const target = Number(value);
  if (!Number.isSafeInteger(target) || target < 1) throw new Error("Quest targets must be positive whole numbers.");
  return target;
}

export async function saveQuestSettings(_: string | null, form: FormData): Promise<string | null> {
  let settings;
  try {
    settings = {
      gym_workout: form.get("gym_workout") === "on",
      step_target: optionalTarget(form, "step_target"),
      activity_rings: form.get("activity_rings") === "on",
      sleep_minutes: optionalTarget(form, "sleep_minutes"),
      meal_target: optionalTarget(form, "meal_target"),
    };
  } catch {
    return "Quest targets must be positive whole numbers.";
  }

  try {
    const response = await apiFetch(`${apiBaseURL}/api/quests/settings`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(settings),
      signal: AbortSignal.timeout(10_000),
    });
    if (!response.ok) {
      const result = await response.json().catch(() => null);
      return result?.error ?? "We couldn't save your quest goals.";
    }
  } catch {
    return "We couldn't reach the server. Please try again.";
  }
  revalidatePath("/quests");
  return null;
}
