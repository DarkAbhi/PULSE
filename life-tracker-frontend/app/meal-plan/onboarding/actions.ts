"use server";

import { revalidatePath } from "next/cache";
import { apiFetch } from "@/app/lib/server-api";
import type { MealPreferences, OnboardingState, PreferenceResult } from "./types";

const base = process.env.INTERNAL_API_BASE_URL ?? "http://localhost:8080";
async function request(path: string, init: RequestInit = {}): Promise<PreferenceResult> {
  try {
    const res = await apiFetch(`${base}/api/${path}`, { ...init, cache: "no-store", signal: AbortSignal.timeout(15000) });
    const body = await res.json();
    if (!res.ok) return { ok: false, error: res.status === 401 ? "Your session has expired. Sign in again to save your preferences." : body.error ?? "Could not save your preferences. Please try again.", state: body.state };
    return { ok: true, state: body as OnboardingState };
  } catch { return { ok: false, error: "Could not connect. Your choices are still here. Please try again." }; }
}
export async function getMealOnboardingAction(): Promise<PreferenceResult> { return request("meal-plan-onboarding"); }
export async function resolveMealOnboardingAction(status: "skipped" | "completed", preferences?: MealPreferences): Promise<PreferenceResult> {
  const result = await request("meal-plan-onboarding", { method: "PUT", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ status, questionnaire_version: 1, ...(preferences ? { preferences } : {}) }) });
  if (result.ok) revalidatePath("/meal-plan");
  return result;
}
export async function saveMealPreferencesAction(preferences: MealPreferences): Promise<PreferenceResult> {
  const result = await request("meal-preferences", { method: "PUT", headers: { "Content-Type": "application/json" }, body: JSON.stringify(preferences) });
  if (result.ok) revalidatePath("/meal-plan");
  return result;
}
