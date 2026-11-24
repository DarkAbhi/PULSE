"use client";

import { useEffect, useRef, useState, type ReactNode } from "react";
import { Check, Leaf, Soup, Sprout } from "lucide-react";
import PulseAppBar from "@/app/components/design-system/pulse-app-bar";
import PulseButton from "@/app/components/design-system/pulse-button";
import { PulseChoiceControl } from "@/app/components/design-system/pulse-choice-group";
import PulseCard from "@/app/components/design-system/pulse-card";
import { frame } from "@/app/components/design-system/page-frame";
import { useTimeZone } from "@/app/components/profile-preferences-provider";
import { getMealOnboardingAction, resolveMealOnboardingAction, saveMealPreferencesAction } from "@/app/meal-plan/onboarding/actions";
import { avoidances, barriers, choiceLabel, diets, firstInvalidStep, goals, initialPreferences, mealKinds, questions, stepError, toggleChoice } from "@/app/meal-plan/onboarding/questionnaire";
import type { MealKind, MealPreferences, OnboardingState } from "@/app/meal-plan/onboarding/types";
import type { MealTime } from "@/app/meal-plan/actions";
import CustomMealTimes from "@/app/meal-plan/onboarding/custom-meal-times";
import { gradientClass, pillClass } from "@/app/meal-plan/onboarding/meal-preference-classes";

import { MealPlate, mealSlotColors } from "@/app/meal-plan/meal-visuals";

const goalIcons = [Soup, Leaf, Sprout];
const mutedClass = "text-control/snug text-muted-foreground";
const errorClass = "text-sm font-bold text-destructive";
const pillGroupClass = "flex flex-wrap gap-2.5";
const choiceFocusClass = "peer-focus-visible:outline-3 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-brand";
const timeSelectClass = "h-11 min-w-[74px] cursor-pointer rounded-control border-[1.5px] border-divider bg-card px-3 font-bold tabular-nums max-[401px]:min-w-[50px] max-[401px]:px-1";
const timeRowClass = "flex flex-wrap items-center justify-between gap-4 px-4 py-3.5 max-sheet:flex-col max-sheet:items-start max-sheet:gap-2 max-[401px]:px-2.5";
const timeNameClass = "flex min-w-0 items-center gap-3 font-bold wrap-anywhere";
const dotClass = "size-3 shrink-0 rounded-full bg-(--slot-color)";
const summaryTimeClass = "shrink-0 text-muted-foreground tabular-nums";

const questionTags = ["Required", "Optional", "Pick one", "Optional", "Pick at least one", "Times"];
const hours = Array.from({ length: 12 }, (_, index) => index + 1);
const minutes = Array.from({ length: 60 }, (_, index) => twoDigits(index));
const periods = ["AM", "PM"] as const;

function twoDigits(value: number) {
  return String(value).padStart(2, "0");
}

function formatTime(value: string) {
  const [hour, minute] = value.split(":");
  const hour24 = Number(hour);
  return `${hour24 % 12 || 12}:${minute} ${hour24 >= 12 ? "PM" : "AM"}`;
}

function UsualTime({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  const [hour, minute] = value.split(":");
  const hour24 = Number(hour);
  const hour12 = hour24 % 12 || 12;
  const period = hour24 >= 12 ? "PM" : "AM";

  function update(nextHour: number, nextMinute: string, nextPeriod: "AM" | "PM") {
    onChange(`${twoDigits(nextHour % 12 + (nextPeriod === "PM" ? 12 : 0))}:${nextMinute}`);
  }

  return <div className="flex max-w-full flex-wrap items-center gap-2 font-extrabold max-[401px]:gap-1" role="group" aria-label={`Usual time for ${label}`}>
    <select className={timeSelectClass} aria-label={`${label} hour`} value={hour12} onChange={event => update(Number(event.target.value), minute, period)}>
      {hours.map(option => <option key={option}>{option}</option>)}
    </select>
    <span aria-hidden="true">:</span>
    <select className={timeSelectClass} aria-label={`${label} minutes`} value={minute} onChange={event => update(hour12, event.target.value, period)}>
      {minutes.map(option => <option key={option}>{option}</option>)}
    </select>
    <div className="ml-1 inline-flex rounded-full bg-chip p-1 max-[401px]:ml-0 max-[401px]:p-0.5">
      {periods.map(option => <button key={option} className="min-h-9 min-w-11 rounded-full px-2.5 text-sm font-bold text-muted-foreground aria-pressed:bg-card aria-pressed:text-foreground aria-pressed:shadow-card max-[401px]:px-1" type="button" aria-label={`${label} ${option}`} aria-pressed={period === option} onClick={() => update(hour12, minute, option)}>{option}</button>)}
    </div>
  </div>;
}

function Question({ index, error, children }: { index: number; error?: string; children: ReactNode }) {
  const { title, description } = questions[index];
  const id = `meal-question-${index + 1}`;
  return <PulseCard as="fieldset" className="grid gap-5 p-4 shadow-card sm:p-6" aria-labelledby={id} aria-describedby={`${id}-hint${error ? ` ${id}-error` : ""}`}>
    <div className="flex items-start gap-3.5">
      <span className={`grid size-10 shrink-0 place-items-center rounded-full font-extrabold ${gradientClass}`} aria-hidden="true">{index + 1}</span>
      <div>
        <h2 id={id} tabIndex={-1} className="scroll-mt-5 scroll-mb-28 text-[clamp(1.25rem,2vw,1.5rem)] leading-[1.2] font-extrabold tracking-[-0.03em]">{title}</h2>
        <p id={`${id}-hint`} className={`mt-[3px] ${mutedClass}`}>{description}</p>
      </div>
      <span className="ml-auto hidden shrink-0 rounded-full bg-chip px-2.5 py-1 text-xs/[inherit] font-bold text-muted-foreground sheet:inline">{questionTags[index]}</span>
    </div>
    {children}
    {error && <p className={errorClass} id={`${id}-error`} role="alert">{error}</p>}
  </PulseCard>;
}

function Choice({ name, value, label, checked, onChange, type = "checkbox", pill = false, icon }: {
  name: string; value: string; label: string; checked: boolean; onChange: (checked: boolean) => void;
  type?: "checkbox" | "radio" | "switch"; pill?: boolean; icon?: ReactNode;
}) {
  const unselectedBorder = "border-divider group-hover:border-[color-mix(in_srgb,var(--meal-accent)_45%,var(--divider))]";
  const choiceBorder = checked
    ? "border-(--meal-accent) bg-[color-mix(in_srgb,var(--meal-accent)_9%,var(--card))]"
    : unselectedBorder;
  const pillColor = checked
    ? `border-transparent ${gradientClass}`
    : unselectedBorder;

  return <PulseChoiceControl type={type} name={name} value={value} checked={checked} onCheckedChange={onChange}>
    {pill
      ? <span className={`${pillClass} gap-2 px-4 font-semibold ${pillColor} ${choiceFocusClass}`}>
          {checked && <Check className="size-4 shrink-0 stroke-3" aria-hidden="true" />}{label}
        </span>
      : <span className={`flex h-full min-h-17 items-center rounded-[20px] border-2 px-4 py-3.5 leading-tight max-[401px]:px-3 ${icon ? "justify-between gap-3 sheet:min-h-[150px] sheet:flex-col sheet:items-start" : "gap-3.5 max-[401px]:gap-2.5"} ${choiceBorder} ${choiceFocusClass}`}>
          {icon && <span className={`grid size-11 shrink-0 place-items-center rounded-control [&_svg]:size-5 ${checked ? gradientClass : "bg-chip text-(--meal-accent)"}`} aria-hidden="true">{icon}</span>}
          <strong className={icon ? "flex-1 text-[1.05rem] sheet:flex-none" : "flex-1"}>{label}</strong>
          <span className={`grid size-6 shrink-0 place-items-center rounded-full border-2 ${icon ? "sheet:absolute sheet:top-3.5 sheet:right-3.5" : ""} ${checked ? `border-transparent ${gradientClass}` : "border-divider text-transparent"}`} aria-hidden="true">
            <Check className="size-4 stroke-3" />
          </span>
        </span>}
  </PulseChoiceControl>;
}

export default function MealOnboarding({ notifications, initial, editing = false, onDone, onCancel, onNotice, times, onCreated, onDeleted }: {
  notifications: ReactNode;
  initial: MealPreferences | null; editing?: boolean; onDone: (state: OnboardingState) => void; onCancel: () => void; onNotice: (message: string) => void;
  times: MealTime[]; onCreated: (time: MealTime) => void; onDeleted: (id: number) => void;
}) {
  const [baseline] = useState(() => initialPreferences(initial));
  const [preferences, setPreferences] = useState(baseline);
  const [showValidation, setShowValidation] = useState(false);
  const [failure, setFailure] = useState<{ message: string; skip: boolean } | null>(null);
  const [busy, setBusy] = useState(false);
  const busyRef = useRef(false);
  const heading = useRef<HTMLHeadingElement>(null);
  const timeDrafts = useRef(Object.fromEntries(baseline.meals.map(meal => [meal.kind, meal.usual_time])));
  const { timeZone } = useTimeZone();
  const unsaved = JSON.stringify(preferences) !== JSON.stringify(baseline);
  const customTimes = times.filter(time => !time.is_default);
  const enabledMeals = mealKinds.flatMap(meal => {
    const selection = preferences.meals.find(slot => slot.kind === meal.kind);
    return selection ? [{ ...meal, time: selection.usual_time }] : [];
  });

  useEffect(() => {
    heading.current?.focus({ preventScroll: true });
    window.scrollTo({ top: 0, behavior: "instant" });
  }, []);

  function patch(value: Partial<MealPreferences>) {
    setPreferences(current => ({ ...current, ...value }));
    setFailure(null);
  }

  function toggleMeal(kind: MealKind, selected: boolean) {
    const meals = mealKinds.flatMap(meal => {
      const current = preferences.meals.find(slot => slot.kind === meal.kind);
      if (meal.kind !== kind) return current ? [current] : [];
      return selected ? [{ kind, usual_time: timeDrafts.current[kind] ?? meal.defaultTime }] : [];
    });
    patch({ meals, planning_slots_configured: true });
  }

  function changeTime(kind: MealKind, usual_time: string) {
    timeDrafts.current[kind] = usual_time;
    patch({ meals: preferences.meals.map(meal => meal.kind === kind ? { ...meal, usual_time } : meal) });
  }

  function validationError(index: number) {
    return showValidation ? stepError(index, preferences) : "";
  }

  async function commit(skip: boolean) {
    if (busyRef.current) return;
    setFailure(null);
    if (!skip) {
      const invalid = firstInvalidStep(preferences);
      if (invalid !== -1) {
        setShowValidation(true);
        document.getElementById(`meal-question-${invalid + 1}`)?.focus();
        return;
      }
    }

    busyRef.current = true;
    setBusy(true);
    try {
      const result = editing
        ? await saveMealPreferencesAction(preferences)
        : await resolveMealOnboardingAction(skip ? "skipped" : "completed", skip ? undefined : preferences);
      if (result.ok) { onDone(result.state); return; }
      if (result.state && result.state.status !== "pending") {
        onNotice("Meal setup was already finished in another tab. You can edit your preferences here anytime.");
        onDone(result.state);
        return;
      }
      // Reconcile a lost response before offering a retry.
      if (!editing) {
        const current = await getMealOnboardingAction();
        if (current.ok && current.state.status !== "pending") { onDone(current.state); return; }
      }
      setFailure({ message: result.error, skip });
    } catch {
      setFailure({ message: "Could not save. Your choices are still here. Please try again.", skip });
    } finally {
      busyRef.current = false;
      setBusy(false);
    }
  }

  return <main id="main-content" tabIndex={-1} className={`${frame.page} [&_:is(button,select):focus-visible]:outline-3 [&_:is(button,select):focus-visible]:outline-offset-2 [&_:is(button,select):focus-visible]:outline-brand`}>
    <div id="meal-setup-journey" className={`${frame.content} grid gap-5`}>
      <PulseAppBar
        back={editing ? { onClick: onCancel, disabled: busy } : { href: "/dashboard" }}
        backLabel={editing ? "Back to Meal Plan" : "Back to Dashboard"}
        notifications={notifications}
      />
      {!editing && <div className="flex justify-end">
        <PulseButton variant="ghost" onClick={() => void commit(true)} disabled={busy}>Skip setup</PulseButton>
      </div>}
      {failure?.skip && <p className={errorClass} role="alert">{failure.message}</p>}

      <header className={`relative isolate flex min-h-[170px] flex-col justify-center gap-2 overflow-hidden rounded-[40px] p-[clamp(22px,3vw,36px)] shadow-(--meal-hero-shadow) ${gradientClass}`}>
        <MealPlate className="absolute top-1/2 -right-5 -z-10 size-[260px] -translate-y-1/2 opacity-15" />
        <h1 ref={heading} tabIndex={-1} className="text-[clamp(2.2rem,5vw,3.6rem)] leading-none font-extrabold tracking-[-0.045em]">Meal preferences</h1>
        <p className="max-w-[46ch] text-[clamp(1rem,1.6vw,1.2rem)] leading-[1.4] font-medium">Six quick questions to shape your meal plan. You can change any of them later.</p>
      </header>

      <form className="grid grid-cols-1 items-start gap-5 min-[1051px]:grid-cols-[minmax(0,1fr)_340px]" aria-label="Meal preference questions" aria-busy={busy} noValidate onSubmit={event => { event.preventDefault(); void commit(false); }}>
        <fieldset className="grid min-w-0 gap-5" disabled={busy} aria-label="Questions">
          <Question index={0} error={validationError(0)}>
            <div className="grid gap-3 sheet:grid-cols-3">
              {goals.map(([value, label], index) => {
                const Icon = goalIcons[index];
                return <Choice key={value} type="radio" name="goal" value={value} label={label} icon={<Icon />} checked={preferences.goal === value} onChange={() => patch({ goal: value })} />;
              })}
            </div>
          </Question>
          <Question index={1}>
            <div className="grid gap-3">
              {barriers.map(([value, label]) => <Choice key={value} name="barriers" value={value} label={label} checked={preferences.barriers.includes(value)} onChange={() => patch({ barriers: toggleChoice(preferences.barriers, value) })} />)}
            </div>
          </Question>
          <Question index={2} error={validationError(2)}>
            <div className={pillGroupClass}>
              {diets.map(([value, label]) => <Choice key={value} pill type="radio" name="diet" value={value} label={label} checked={preferences.dietary_pattern === value} onChange={() => patch({ dietary_pattern: value })} />)}
            </div>
          </Question>
          <Question index={3}>
            <div>
              <div className={pillGroupClass}>
                {avoidances.map(([value, label]) => <Choice key={value} pill name="avoidances" value={value} label={label} checked={preferences.food_avoidances.includes(value)} onChange={() => patch({ food_avoidances: toggleChoice(preferences.food_avoidances, value) })} />)}
              </div>
              <div className="mt-1.5 flex min-h-11 items-center justify-between gap-3 text-sm font-semibold text-muted-foreground">
                <span aria-live="polite">{preferences.food_avoidances.length ? `${preferences.food_avoidances.length} selected` : "None selected"}</span>
                <button type="button" className="min-h-11 px-1.5 font-bold text-(--meal-accent)" disabled={!preferences.food_avoidances.length} onClick={() => patch({ food_avoidances: [] })}>Clear all</button>
              </div>
            </div>
          </Question>
          <Question index={4} error={validationError(4)}>
            {initial && !initial.planning_slots_configured && !preferences.planning_slots_configured &&
              <p className={mutedClass}>All meal times are currently available in Add Meal. Change a switch and save to use these selections.</p>}
            <div className={pillGroupClass}>
              {mealKinds.map(({ kind, label }) => <Choice key={kind} pill type="switch" name="meals" value={kind} label={label} checked={preferences.meals.some(meal => meal.kind === kind)} onChange={selected => toggleMeal(kind, selected)} />)}
              <CustomMealTimes times={customTimes} onCreated={onCreated} onDeleted={onDeleted} />
            </div>
          </Question>
          <Question index={5} error={validationError(5)}>
            <p className={mutedClass}>Local times in {timeZone}</p>
            {!enabledMeals.length && !customTimes.length && <p className={mutedClass}>Select a meal above to set its usual time.</p>}
            <div className="divide-y divide-divider rounded-[20px] bg-muted">
              {enabledMeals.map(({ kind, label, time }) => <div key={kind} className={timeRowClass}>
                <span className={timeNameClass}><span className={`${dotClass} ${mealSlotColors[kind]}`} />{label}</span>
                <UsualTime label={label} value={time} onChange={value => changeTime(kind, value)} />
              </div>)}
              {customTimes.map(time => <div key={time.id} className={timeRowClass}>
                <span className={timeNameClass}><span className={`${dotClass} ${mealSlotColors.custom}`} />{time.name}</span>
                <span className={mutedClass}>{formatTime(time.start_time)}–{formatTime(time.end_time)}</span>
              </div>)}
            </div>
          </Question>
        </fieldset>

        <PulseCard as="aside" className={`sticky bottom-[max(12px,env(safe-area-inset-bottom))] z-[8] flex flex-col gap-2 border border-divider bg-card p-2.5 shadow-[0_12px_30px_-10px_rgb(0_0_0/35%)] min-[1051px]:top-4 min-[1051px]:bottom-auto min-[1051px]:z-auto min-[1051px]:max-h-[calc(100dvh-32px)] min-[1051px]:gap-[18px] min-[1051px]:rounded-card min-[1051px]:border-0 min-[1051px]:p-[clamp(16px,2.4vw,24px)] min-[1051px]:shadow-card ${failure && !failure.skip ? "rounded-card" : "rounded-full"}`} aria-label="Your plan so far">
          <div className="hidden overflow-y-auto border-b border-divider pb-[18px] min-[1051px]:block [&_dt]:text-[0.72rem] [&_dt]:font-bold [&_dt]:tracking-[0.05em] [&_dt]:text-muted-foreground [&_dt]:uppercase [&_dd]:mt-[3px] [&_dd]:leading-[1.3] [&_dd]:font-bold">
            <h2 className="mb-[18px] text-[1.2rem] font-extrabold tracking-[-0.02em]">Your plan so far</h2>
            <dl className="grid gap-3.5">
              <div><dt>Goal</dt><dd>{preferences.goal ? choiceLabel(goals, preferences.goal) : <span className={mutedClass}>Not chosen yet</span>}</dd></div>
              <div><dt>How you like to eat</dt><dd>{preferences.dietary_pattern ? choiceLabel(diets, preferences.dietary_pattern) : <span className={mutedClass}>Not chosen yet</span>}</dd></div>
              <div>
                <dt>Avoiding</dt>
                <dd>{preferences.food_avoidances.length
                  ? <span className="mt-1.5 flex flex-wrap gap-1.5 [&>span]:rounded-full [&>span]:bg-chip [&>span]:px-2.5 [&>span]:py-[3px] [&>span]:text-[0.82rem] [&>span]:font-semibold">{preferences.food_avoidances.map(value => <span key={value}>{choiceLabel(avoidances, value)}</span>)}</span>
                  : <span className={mutedClass}>Nothing selected</span>}</dd>
              </div>
              <div>
                <dt>Meals and usual times</dt>
                <dd>{enabledMeals.length || customTimes.length
                  ? <ul className="mt-1.5 grid gap-1.5 text-[0.95rem] font-semibold [&>li]:flex [&>li]:justify-between [&>li]:gap-2.5 [&>li]:wrap-anywhere">
                      {enabledMeals.map(meal => <li key={meal.kind}><span>{meal.label}</span><time className={summaryTimeClass} dateTime={meal.time}>{formatTime(meal.time)}</time></li>)}
                      {customTimes.map(time => <li key={time.id}><span>{time.name}</span><span className={`leading-[1.4] ${summaryTimeClass}`}>{formatTime(time.start_time)}–{formatTime(time.end_time)}</span></li>)}
                    </ul>
                  : <span className={mutedClass}>None selected</span>}</dd>
              </div>
            </dl>
          </div>
          {failure && !failure.skip && <p className={`${errorClass} max-[1051px]:px-2`} role="alert">{failure.message}</p>}
          <div className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-2 pl-2 min-[1051px]:grid-cols-1 min-[1051px]:gap-[18px] min-[1051px]:pl-0">
            <span className="flex items-center gap-2 text-[0.88rem]/[1.2] font-semibold text-muted-foreground before:size-2 before:shrink-0 before:rounded-full before:bg-success data-[unsaved=true]:before:bg-warning" data-unsaved={unsaved} role="status">
              {unsaved ? "Unsaved changes" : initial ? "All changes saved" : "Ready to customize"}
            </span>
            <PulseButton variant="primary" size="lg" labelStyle="strong" className="whitespace-nowrap [--button-fill:var(--meal-preferences-gradient)] [--button-foreground:white]" type="submit" disabled={busy}>
              {busy ? "Saving…" : "Save preferences"}
            </PulseButton>
          </div>
        </PulseCard>
      </form>
    </div>
  </main>;
}
