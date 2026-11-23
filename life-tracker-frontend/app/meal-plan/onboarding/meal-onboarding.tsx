"use client";

import { useEffect, useRef, useState, type ReactNode } from "react";
import { Check, Leaf, Soup, Sprout } from "lucide-react";
import AppBar from "@/app/components/design-system/app-bar";
import Button from "@/app/components/design-system/button";
import { useTimeZone } from "@/app/components/time-zone-provider";
import { getMealOnboardingAction, resolveMealOnboardingAction, saveMealPreferencesAction } from "./actions";
import { avoidances, barriers, choiceLabel, diets, firstInvalidStep, goals, initialPreferences, mealKinds, questions, stepError, toggleChoice } from "./questionnaire";
import type { MealKind, MealPreferences, OnboardingState } from "./types";
import type { MealTime } from "../actions";
import CustomMealTimes from "./custom-meal-times";
import styles from "./meal-preferences.module.css";

const goalIcons = [Soup, Leaf, Sprout];
const mealColors = {
  breakfast: "#FFC83D",
  lunch: "#F0603A",
  evening_snacks: "#D91CAB",
  dinner: "#5B2CE0",
};
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

  return <div className={styles.timeControls} role="group" aria-label={`Usual time for ${label}`}>
    <select aria-label={`${label} hour`} value={hour12} onChange={event => update(Number(event.target.value), minute, period)}>
      {hours.map(option => <option key={option}>{option}</option>)}
    </select>
    <span aria-hidden="true">:</span>
    <select aria-label={`${label} minutes`} value={minute} onChange={event => update(hour12, event.target.value, period)}>
      {minutes.map(option => <option key={option}>{option}</option>)}
    </select>
    <div className={styles.period}>
      {periods.map(option => <button key={option} type="button" aria-label={`${label} ${option}`} aria-pressed={period === option} onClick={() => update(hour12, minute, option)}>{option}</button>)}
    </div>
  </div>;
}

function Question({ index, error, children }: { index: number; error?: string; children: ReactNode }) {
  const { title, description } = questions[index];
  const id = `meal-question-${index + 1}`;
  return <fieldset className={styles.question} aria-labelledby={id} aria-describedby={`${id}-hint${error ? ` ${id}-error` : ""}`}>
    <div className={styles.questionHeader}>
      <span className={styles.number} aria-hidden="true">{index + 1}</span>
      <div>
        <h2 id={id} tabIndex={-1}>{title}</h2>
        <p id={`${id}-hint`}>{description}</p>
      </div>
      <span className={styles.tag}>{questionTags[index]}</span>
    </div>
    {children}
    {error && <p className={styles.error} id={`${id}-error`} role="alert">{error}</p>}
  </fieldset>;
}

function Choice({ name, value, label, checked, onChange, type = "checkbox", pill = false, icon }: {
  name: string; value: string; label: string; checked: boolean; onChange: (checked: boolean) => void;
  type?: "checkbox" | "radio" | "switch"; pill?: boolean; icon?: ReactNode;
}) {
  return <label className={pill ? styles.pill : styles.choice}>
    <input type={type === "switch" ? "checkbox" : type} role={type === "switch" ? "switch" : undefined} name={name} value={value} checked={checked} onChange={event => onChange(event.target.checked)} />
    {pill
      ? <span><Check aria-hidden="true" />{label}</span>
      : <span className={styles.choiceBox}>
          {icon && <span className={styles.choiceIcon} aria-hidden="true">{icon}</span>}
          <strong>{label}</strong>
          <span className={styles.tick} aria-hidden="true"><Check /></span>
        </span>}
  </label>;
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

  return <main className={styles.page}>
    <div id="meal-setup-journey" className={styles.wrap}>
      <AppBar
        back={editing ? { onClick: onCancel, disabled: busy } : { href: "/dashboard" }}
        backLabel={editing ? "Back to Meal Plan" : "Back to Dashboard"}
        notifications={notifications}
      />
      <div className="flex justify-end">
        {editing
          ? <Button variant="tertiary" onClick={onCancel} disabled={busy}>Cancel editing</Button>
          : <Button variant="tertiary" onClick={() => void commit(true)} disabled={busy}>Skip setup</Button>}
      </div>
      {failure?.skip && <p className={styles.error} role="alert">{failure.message}</p>}

      <header className={styles.hero}>
        <svg className={styles.watermark} viewBox="0 0 24 24" aria-hidden="true">
          <circle cx="12" cy="12" r="5.5" />
          <circle cx="12" cy="12" r="2" />
          <path d="M3 4.5v4a1.5 1.5 0 0 0 3 0v-4M4.5 10v9.5M21 4.5c-1.6 1-2.2 3-2.2 5.5h2.2v9.5" />
        </svg>
        <h1 ref={heading} tabIndex={-1}>Meal preferences</h1>
        <p>Six quick questions to shape your meal plan. You can change any of them later.</p>
      </header>

      <form className={styles.layout} aria-label="Meal preference questions" aria-busy={busy} noValidate onSubmit={event => { event.preventDefault(); void commit(false); }}>
        <fieldset className={styles.questions} disabled={busy} aria-label="Questions">
          <Question index={0} error={validationError(0)}>
            <div className={styles.goals}>
              {goals.map(([value, label], index) => {
                const Icon = goalIcons[index];
                return <Choice key={value} type="radio" name="goal" value={value} label={label} icon={<Icon />} checked={preferences.goal === value} onChange={() => patch({ goal: value })} />;
              })}
            </div>
          </Question>
          <Question index={1}>
            <div className={styles.options}>
              {barriers.map(([value, label]) => <Choice key={value} name="barriers" value={value} label={label} checked={preferences.barriers.includes(value)} onChange={() => patch({ barriers: toggleChoice(preferences.barriers, value) })} />)}
            </div>
          </Question>
          <Question index={2} error={validationError(2)}>
            <div className={styles.pills}>
              {diets.map(([value, label]) => <Choice key={value} pill type="radio" name="diet" value={value} label={label} checked={preferences.dietary_pattern === value} onChange={() => patch({ dietary_pattern: value })} />)}
            </div>
          </Question>
          <Question index={3}>
            <div>
              <div className={styles.pills}>
                {avoidances.map(([value, label]) => <Choice key={value} pill name="avoidances" value={value} label={label} checked={preferences.food_avoidances.includes(value)} onChange={() => patch({ food_avoidances: toggleChoice(preferences.food_avoidances, value) })} />)}
              </div>
              <div className={styles.count}>
                <span aria-live="polite">{preferences.food_avoidances.length ? `${preferences.food_avoidances.length} selected` : "None selected"}</span>
                <button type="button" className={styles.clear} disabled={!preferences.food_avoidances.length} onClick={() => patch({ food_avoidances: [] })}>Clear all</button>
              </div>
            </div>
          </Question>
          <Question index={4} error={validationError(4)}>
            {initial && !initial.planning_slots_configured && !preferences.planning_slots_configured &&
              <p className={styles.muted}>All meal times are currently available in Add Meal. Change a switch and save to use these selections.</p>}
            <div className={styles.pills}>
              {mealKinds.map(({ kind, label }) => <Choice key={kind} pill type="switch" name="meals" value={kind} label={label} checked={preferences.meals.some(meal => meal.kind === kind)} onChange={selected => toggleMeal(kind, selected)} />)}
              <CustomMealTimes times={customTimes} onCreated={onCreated} onDeleted={onDeleted} />
            </div>
          </Question>
          <Question index={5} error={validationError(5)}>
            <p className={styles.muted}>Local times in {timeZone}</p>
            {!enabledMeals.length && !customTimes.length && <p className={styles.muted}>Select a meal above to set its usual time.</p>}
            <div className={styles.times}>
              {enabledMeals.map(({ kind, label, time }) => <div key={kind} className={styles.timeRow}>
                <span className={styles.timeName}><span className={styles.dot} style={{ background: mealColors[kind] }} />{label}</span>
                <UsualTime label={label} value={time} onChange={value => changeTime(kind, value)} />
              </div>)}
              {customTimes.map(time => <div key={time.id} className={styles.timeRow}>
                <span className={styles.timeName}><span className={styles.dot} />{time.name}</span>
                <span className={styles.muted}>{formatTime(time.start_time)}–{formatTime(time.end_time)}</span>
              </div>)}
            </div>
          </Question>
        </fieldset>

        <aside className={styles.summary} aria-label="Your plan so far">
          <div className={styles.summaryDetails}>
            <h2>Your plan so far</h2>
            <dl>
              <div><dt>Goal</dt><dd>{preferences.goal ? choiceLabel(goals, preferences.goal) : <span className={styles.muted}>Not chosen yet</span>}</dd></div>
              <div><dt>How you like to eat</dt><dd>{preferences.dietary_pattern ? choiceLabel(diets, preferences.dietary_pattern) : <span className={styles.muted}>Not chosen yet</span>}</dd></div>
              <div>
                <dt>Avoiding</dt>
                <dd>{preferences.food_avoidances.length
                  ? <span className={styles.summaryPills}>{preferences.food_avoidances.map(value => <span key={value}>{choiceLabel(avoidances, value)}</span>)}</span>
                  : <span className={styles.muted}>Nothing selected</span>}</dd>
              </div>
              <div>
                <dt>Meals and usual times</dt>
                <dd>{enabledMeals.length || customTimes.length
                  ? <ul>
                      {enabledMeals.map(meal => <li key={meal.kind}><span>{meal.label}</span><time dateTime={meal.time}>{formatTime(meal.time)}</time></li>)}
                      {customTimes.map(time => <li key={time.id}><span>{time.name}</span><span className={styles.muted}>{formatTime(time.start_time)}–{formatTime(time.end_time)}</span></li>)}
                    </ul>
                  : <span className={styles.muted}>None selected</span>}</dd>
              </div>
            </dl>
          </div>
          {failure && !failure.skip && <p className={styles.error} role="alert">{failure.message}</p>}
          <div className={styles.saveControls}>
            <span className={styles.status} data-unsaved={unsaved} role="status">
              {unsaved ? "Unsaved changes" : initial ? "All changes saved" : "Ready to customize"}
            </span>
            <button className={styles.save} type="submit" disabled={busy}>{busy ? "Saving…" : "Save preferences"}</button>
          </div>
        </aside>
      </form>
    </div>
  </main>;
}
