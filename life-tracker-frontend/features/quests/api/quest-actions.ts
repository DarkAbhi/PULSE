"use server";

import { revalidatePath } from "next/cache";
import { apiBaseURL, apiFetch } from "@/lib/api/server-api";
import { parseSleepGoal, sleepGoalError } from "@/features/quests/utils/sleep-goal";

function optionalTarget(form: FormData, key: string) {
  const value = form.get(key);
  if (typeof value !== "string" || !value.trim()) return null;
  const target = Number(value);
  if (!Number.isSafeInteger(target) || target < 1) throw new Error("Quest targets must be positive whole numbers.");
  return target;
}

export async function saveQuestSettings(_: string | null, form: FormData): Promise<string | null> {
  const sleep = form.get("sleep_duration");
  const sleepMinutes = parseSleepGoal(typeof sleep === "string" ? sleep : "");
  if (sleepMinutes === undefined) return sleepGoalError;

  let settings;
  try {
    settings = {
      gym_workout: form.get("gym_workout") === "on",
      step_target: optionalTarget(form, "step_target"),
      activity_rings: form.get("activity_rings") === "on",
      sleep_minutes: sleepMinutes,
      daily_meals: form.get("daily_meals") === "on",
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
