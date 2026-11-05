"use client";

import Image from "next/image";
import { useEffect, useRef, useState } from "react";
import { ArrowLeft, Check, Moon, Sun, Sunrise, Utensils } from "lucide-react";
import Button from "@/app/components/design-system/button";
import BackLink from "@/app/components/design-system/back-link";
import PulseCard from "@/app/components/design-system/pulse-card";
import ChoiceGroup from "@/app/components/design-system/choice-group";
import Switch from "@/app/components/design-system/switch";
import TimeInput from "@/app/components/design-system/time-input";
import { useTimeZone } from "@/app/components/time-zone-provider";
import { getMealOnboardingAction, resolveMealOnboardingAction, saveMealPreferencesAction } from "./actions";
import { avoidances, barriers, choiceLabel, diets, firstInvalidStep, goals, initialPreferences, mealKinds, questions, stepError } from "./questionnaire";
import type { MealPreferences, OnboardingState } from "./types";

const mealIcons = { breakfast: Sunrise, lunch: Sun, dinner: Moon };
export default function MealOnboarding({ initial, editing = false, onDone, onCancel, onNotice }: {
  initial: MealPreferences | null; editing?: boolean; onDone: (state: OnboardingState) => void; onCancel: () => void; onNotice: (message: string) => void;
}) {
  const [preferences, setPreferences] = useState(() => initialPreferences(initial));
  const [step, setStep] = useState(0);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const busyRef = useRef(false);
  const [returnToReview, setReturnToReview] = useState(false);
  const heading = useRef<HTMLHeadingElement>(null);
  // Preserve edited meal times when the user toggles a meal off and back on.
  const timeDrafts = useRef(Object.fromEntries(preferences.meals.map(m => [m.kind, m.usual_time])));
  const { timeZone } = useTimeZone();
  const review = step === 6;
  useEffect(() => { heading.current?.focus({ preventScroll: true }); window.scrollTo({ top: 0, behavior: "instant" }); }, [step]);

  function patch(value: Partial<MealPreferences>) { setPreferences(p => ({ ...p, ...value })); setError(""); }
  function goTo(next: number) { setError(""); setStep(next); }
  function continueJourney() {
    const message = stepError(step, preferences);
    if (message) { setError(message); return; }
    goTo(returnToReview ? 6 : step + 1);
    setReturnToReview(false);
  }
  async function commit(skip: boolean) {
    if (busyRef.current) return;
    if (!skip) {
      const invalid = firstInvalidStep(preferences);
      if (invalid !== -1) { goTo(invalid); setError(stepError(invalid, preferences)); return; }
    }
    busyRef.current = true;
    setBusy(true); setError("");
    try {
      const result = editing ? await saveMealPreferencesAction(preferences) : await resolveMealOnboardingAction(skip ? "skipped" : "completed", skip ? undefined : preferences);
      if (result.ok) { onDone(result.state); return; }
      if (result.state && result.state.status !== "pending") {
        onNotice("Meal setup was already finished in another tab. You can edit your preferences here anytime.");
        onDone(result.state); return;
      }
      // A timeout may have happened after the database committed. Reconcile before
      // offering a retry, without persisting a browser-only dismissal flag.
      if (!editing) {
        const current = await getMealOnboardingAction();
        if (current.ok && current.state.status !== "pending") { onDone(current.state); return; }
      }
      setError(result.error);
    } catch { setError("Could not save. Your choices are still here. Please try again."); }
    finally { busyRef.current = false; setBusy(false); }
  }

  const summary = [
    { title: "Your goal", value: choiceLabel(goals, preferences.goal) },
    { title: "What gets in the way", value: preferences.barriers.map(v => choiceLabel(barriers, v)).join(", ") || "None selected" },
    { title: "Eating style", value: choiceLabel(diets, preferences.dietary_pattern) },
    { title: "Foods to avoid", value: preferences.food_avoidances.map(v => choiceLabel(avoidances, v)).join(", ") || "None selected" },
    { title: "Meals to plan", value: preferences.meals.map(m => mealKinds.find(k => k.kind === m.kind)?.label).join(", ") },
    { title: "Usual meal times", value: preferences.meals.map(m => `${mealKinds.find(k => k.kind === m.kind)?.label}: ${m.usual_time}`).join(", ") },
  ];

  return <main className="min-h-screen bg-background text-foreground">
    <div className="mx-auto max-w-6xl px-4 pt-5 sm:px-8 sm:pt-8">
      <header className="flex items-center justify-between gap-4">
        <BackLink href="/dashboard">Dashboard</BackLink>
        {editing ? <Button variant="tertiary" onClick={onCancel} disabled={busy}>Cancel editing</Button> : <Button variant="tertiary" onClick={() => commit(true)} disabled={busy}>{busy ? "Saving…" : "Skip setup"}</Button>}
      </header>
      <div className="mt-5 grid gap-6 lg:mt-8 lg:grid-cols-[0.85fr_1.15fr] lg:gap-14">
        <aside className="relative h-36 overflow-hidden rounded-3xl bg-accent sm:h-48 lg:sticky lg:top-8 lg:h-[min(720px,85dvh)]" aria-hidden="true">
          <div className="absolute bottom-7 left-7 z-10 hidden max-w-64 lg:block">
            <Utensils className="mb-4 size-7 text-action" />
            <p className="text-3xl font-semibold tracking-tight text-accent-foreground">A little more you.<br />A little less figuring it out.</p>
            <p className="mt-4 text-sm leading-6 text-accent-foreground">Make room for meals that fit your day.</p>
          </div>
          <Image src={step % 2 === 0 ? "/meal-plan/onboarding/salad.webp" : "/meal-plan/onboarding/pesto.webp"} alt="" width={900} height={900} priority className="absolute -right-10 -top-24 w-72 max-w-none rotate-[-12deg] sm:-top-32 sm:w-96 lg:-right-28 lg:-top-20 lg:w-[560px]" sizes="(min-width: 1024px) 560px, (min-width: 640px) 384px, 288px" />
        </aside>
        <section className="min-w-0 pb-4" aria-labelledby="meal-setup-heading" aria-busy={busy}>
          <div className="mb-6 flex items-center justify-between gap-4">
            <Button variant="iconSecondary" aria-label={step === 0 ? "Back to meal planner" : "Previous question"} disabled={busy || (!editing && step === 0)} onClick={() => { if (step === 0) onCancel(); else { setReturnToReview(false); goTo(step - 1); } }}><ArrowLeft className="size-4" /></Button>
            <span className="text-sm text-muted-foreground" aria-live="polite">{review ? "Ready when you are" : `Question ${step + 1} of 6`}</span>
          </div>
          <div role="progressbar" aria-label="Meal setup progress" aria-valuemin={0} aria-valuemax={6} aria-valuenow={Math.min(step + 1, 6)} className="mb-7 flex gap-1.5">
            {questions.map((_, index) => <span key={index} className={`h-1 flex-1 rounded-full ${index <= step ? "bg-action" : "bg-surface-strong"}`} />)}
          </div>
          <h1 ref={heading} id="meal-setup-heading" tabIndex={-1} className="max-w-xl text-3xl font-bold leading-tight tracking-tight outline-none sm:text-4xl">{review ? "A plan that feels like you." : questions[step].title}</h1>
          <p className="mt-3 max-w-lg text-base leading-7 text-muted-foreground">{review ? "Take a look, then save your preferences. You can change them anytime." : questions[step].description}</p>
          <form className="mt-7" onSubmit={e => { e.preventDefault(); if (review) void commit(false); else continueJourney(); }}>
            <fieldset disabled={busy} className="min-w-0" aria-describedby={error ? "meal-setup-error" : undefined}>
              <legend className="sr-only">{review ? "Review preferences" : questions[step].title}</legend>
              {step === 0 && <ChoiceGroup label="Your goal" value={preferences.goal} options={goals.map(([value, label]) => ({ value, label }))} onChange={goal => patch({ goal })} />}
              {step === 1 && <ChoiceGroup multiple label="What gets in the way" value={preferences.barriers} options={barriers.map(([value, label]) => ({ value, label }))} onChange={barriers => patch({ barriers })} />}
              {step === 2 && <ChoiceGroup label="Eating style" value={preferences.dietary_pattern} options={diets.map(([value, label]) => ({ value, label }))} onChange={dietary_pattern => patch({ dietary_pattern })} />}
              {step === 3 && <ChoiceGroup multiple columns label="Foods to avoid" value={preferences.food_avoidances} options={avoidances.map(([value, label, icon]) => ({ value, label, icon }))} onChange={food_avoidances => patch({ food_avoidances })} />}
              {step === 4 && <div className="grid gap-3">{mealKinds.map(meal => {
                const Icon = mealIcons[meal.kind];
                return <Switch key={meal.kind} checked={preferences.meals.some(m => m.kind === meal.kind)} onChange={selected => {
                  const meals = selected ? [...preferences.meals, { kind: meal.kind, usual_time: timeDrafts.current[meal.kind] ?? meal.defaultTime }] : preferences.meals.filter(m => m.kind !== meal.kind);
                  patch({ meals: mealKinds.flatMap(k => meals.filter(m => m.kind === k.kind)) });
                }}><Icon className="size-6 text-action" aria-hidden="true" />{meal.label}</Switch>;
              })}</div>}
              {step === 5 && <div className="grid gap-3">
                <p className="mb-1 text-sm text-muted-foreground">Local times in {timeZone}</p>
                {preferences.meals.map(meal => <TimeInput key={meal.kind} label={mealKinds.find(m => m.kind === meal.kind)!.label} value={meal.usual_time} onChange={usual_time => {
                  timeDrafts.current[meal.kind] = usual_time;
                  patch({ meals: preferences.meals.map(m => m.kind === meal.kind ? { ...m, usual_time } : m) });
                }} />)}
              </div>}
              {review && <PulseCard className="divide-y divide-divider px-5">{summary.map((item, index) => <div key={item.title} className="flex items-start justify-between gap-4 py-4">
                <div><h2 className="text-xs font-medium text-muted-foreground">{item.title}</h2><p className="mt-1 text-sm leading-6">{item.value}</p></div>
                <Button type="button" variant="tertiary" size="sm" aria-label={`Edit ${item.title.toLowerCase()}`} onClick={() => { setReturnToReview(true); goTo(index); }}>Edit</Button>
              </div>)}</PulseCard>}
            </fieldset>
            <div className="bottom-0 z-20 -mx-4 mt-6 border-t border-divider bg-background px-4 pt-4 pb-[max(1rem,env(safe-area-inset-bottom))] sm:mx-0 sm:px-0 lg:sticky">
              {error && <p id="meal-setup-error" role="alert" className="mb-3 text-sm text-destructive">{error}</p>}
              <Button type="submit" size="lg" className="w-full" disabled={busy} icon={review ? <Check className="size-4" aria-hidden="true" /> : undefined}>{busy ? "Saving…" : review ? "Save preferences" : returnToReview ? "Back to review" : "Continue"}</Button>
              <p className="mt-3 text-center text-xs leading-5 text-muted-foreground">{editing ? "Changes are saved only when you choose Save preferences." : "Your answers are saved only when you choose Save preferences."}</p>
            </div>
          </form>
        </section>
      </div>
    </div>
  </main>;
}
