"use client";

import { useEffect, useState } from "react";
import Button from "@/app/components/design-system/button";
import MealPlanClient from "../meal-plan-client";
import type { MealPlan, MealTime } from "../actions";
import MealOnboarding from "./meal-onboarding";
import { getMealOnboardingAction } from "./actions";
import { shouldShowOnboarding } from "./questionnaire";
import type { OnboardingState } from "./types";

export default function MealPlanGate({ initialState, ...plannerProps }: { initialState: OnboardingState | null; initialMeals: MealPlan[]; initialMealTimes: MealTime[]; initialTodayStr: string }) {
  const [state, setState] = useState(initialState);
  const [editing, setEditing] = useState(false);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  useEffect(() => {
    if (state?.status !== "pending") return;
    let active = true;
    const reconcile = async () => {
      const result = await getMealOnboardingAction();
      if (active && result.ok && result.state.status !== "pending") setState(result.state);
    };
    // Browser history restores can reuse a page without a fresh server render.
    window.addEventListener("pageshow", reconcile);
    return () => { active = false; window.removeEventListener("pageshow", reconcile); };
  }, [state?.status]);
  if (!state) return <main className="flex min-h-screen items-center justify-center bg-background p-6 text-foreground"><div className="max-w-md text-center">
    <h1 className="text-2xl font-semibold">Let's load your meal preferences.</h1><p role="alert" className="my-4 text-muted-foreground">{error || "We couldn't check your meal setup. Please try again."}</p>
    <Button disabled={loading} onClick={async () => { setLoading(true); const result = await getMealOnboardingAction(); if (result.ok) setState(result.state); else setError(result.error); setLoading(false); }}>{loading ? "Loading…" : "Try again"}</Button>
  </div></main>;
  if (shouldShowOnboarding(state) || editing) return <MealOnboarding key={editing ? "edit" : "setup"} initial={state.preferences} editing={editing} onDone={next => { setState(next); setEditing(false); setNotice(current => current || (next.preferences ? "Meal preferences saved. You can edit them anytime." : "Setup skipped. You can add meal preferences anytime.")); }} onCancel={() => setEditing(false)} onNotice={setNotice} />;
  return <>
    {notice && <div role="status" className="flex items-center justify-center gap-3 bg-success-soft px-4 py-2 text-sm text-success">{notice}<Button size="sm" variant="tertiary" aria-label="Dismiss message" onClick={() => setNotice("")}>Dismiss</Button></div>}
    <MealPlanClient {...plannerProps} preferences={state.preferences} onEditPreferences={() => { setNotice(""); setEditing(true); }} />
  </>;
}
