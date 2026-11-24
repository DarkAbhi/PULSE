"use server";

import { apiBaseURL, apiFetch } from "@/app/lib/server-api";
import { revalidatePath } from "next/cache";


export async function markGymVisitAction(date?: string) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/${date ? "gym-visits" : "workout/today"}`, {
      method: "POST",
      headers: {
        ...(date ? { "Content-Type": "application/json" } : {}),
      },
      ...(date ? { body: JSON.stringify({ date }) } : {}),
    });

    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      return {
        ok: false,
        error: body.error ?? "We couldn't save your workout.",
      };
    }

    revalidatePath("/dashboard");
    revalidatePath("/fitness");
    return { ok: true, id: body.id };
  } catch {
    return {
      ok: false,
      error: "We couldn't reach the server. Please try again.",
    };
  }
}

async function saveClassification(path: string, workoutType: string, focus: string, fallback: string) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/${path}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ workout_type: workoutType, workout_focus: focus }),
      signal: AbortSignal.timeout(10_000),
    });
    if (!response.ok) {
      const body = await response.json().catch(() => null);
      return { ok: false, error: body?.error ?? fallback };
    }
  } catch {
    return { ok: false, error: "We couldn't reach the server. Please try again." };
  }
  return { ok: true };
}

export async function saveWorkoutType(visitID: string, workoutType: string, focus: string) {
  const result = await saveClassification(
    `fitness-workouts/${encodeURIComponent(visitID)}/classification`,
    workoutType, focus, "We couldn't save this workout.",
  );
  if (result.ok) {
    revalidatePath(`/fitness/${encodeURIComponent(visitID)}`);
    revalidatePath("/fitness");
  }
  return result;
}

export async function saveWorkoutScheduleDay(weekday: number, workoutType: string, focus: string) {
  const result = await saveClassification(
    `fitness/workout-schedule/${encodeURIComponent(weekday)}`,
    workoutType, focus, "We couldn't save this scheduled day.",
  );
  if (result.ok) {
    revalidatePath("/fitness");
    revalidatePath("/quests");
  }
  return result;
}
