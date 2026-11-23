"use client";

import dynamic from "next/dynamic";
import { useEffect, useState, type ReactNode } from "react";
import PulseAppBar from "@/app/components/design-system/pulse-app-bar";
import PulseButton from "@/app/components/design-system/pulse-button";
import { frame } from "@/app/components/design-system/page-frame";
import MealPlanClient from "@/app/meal-plan/meal-plan-client";
import type { MealPlan, MealTime } from "@/app/meal-plan/actions";
import { getMealOnboardingAction } from "@/app/meal-plan/onboarding/actions";
import { shouldShowOnboarding } from "@/app/meal-plan/onboarding/questionnaire";
import type { OnboardingState } from "@/app/meal-plan/onboarding/types";

const MealOnboarding = dynamic(() => import("@/app/meal-plan/onboarding/meal-onboarding"));

export default function MealPlanGate({ initialState, notifications, ...plannerProps }: { notifications: ReactNode; initialState: OnboardingState | null; initialMeals: MealPlan[] | null; initialMealTimes: MealTime[]; initialTodayStr: string }) {
  const [state, setState] = useState(initialState);
  const [editing, setEditing] = useState(false);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [mealTimes, setMealTimes] = useState(plannerProps.initialMealTimes);
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
  const appBar = <PulseAppBar
    back={{ href: "/dashboard" }}
    backLabel="Back to Dashboard"
    notifications={notifications}
  />;

  if (!state) return (
    <main id="main-content" tabIndex={-1} className={`${frame.page} flex flex-col bg-background text-foreground`}>
      <div className={`${frame.content} flex w-full flex-1 flex-col`}>
        {appBar}
        <div className="m-auto max-w-md py-6 text-center">
          <h1 className="text-2xl font-semibold">Let's load your meal preferences.</h1>
          <p role="alert" className="my-4 text-muted-foreground">{error || "We couldn't check your meal setup. Please try again."}</p>
          <PulseButton disabled={loading} onClick={async () => { setLoading(true); const result = await getMealOnboardingAction(); if (result.ok) setState(result.state); else setError(result.error); setLoading(false); }}>{loading ? "Loading…" : "Try again"}</PulseButton>
        </div>
      </div>
    </main>
  );
  if (shouldShowOnboarding(state) || editing) return <MealOnboarding notifications={notifications} key={editing ? "edit" : "setup"} initial={state.preferences} editing={editing} onDone={next => { setState(next); setEditing(false); setNotice(current => current || (next.preferences ? "Meal preferences saved. You can edit them anytime." : "Setup skipped. You can add meal preferences anytime.")); }} onCancel={() => setEditing(false)} onNotice={setNotice}
    times={mealTimes}
    onCreated={time => setMealTimes(current => [...current.filter(slot => slot.id !== time.id), time])}
    onDeleted={id => setMealTimes(current => current.filter(time => time.id !== id))}
  />;
  return <>
    {notice && <div role="status" className="flex items-center justify-center gap-3 bg-success-soft px-4 py-2 text-sm text-success">{notice}<PulseButton size="sm" variant="tertiary" aria-label="Dismiss message" onClick={() => setNotice("")}>Dismiss</PulseButton></div>}
    <MealPlanClient appBar={appBar} {...plannerProps} initialMealTimes={mealTimes} preferences={state.preferences} onEditPreferences={() => { setNotice(""); setEditing(true); }} />
  </>;
}
