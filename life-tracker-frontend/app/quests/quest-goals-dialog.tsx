"use client";

import { useActionState, useId, useState, type ReactNode } from "react";
import Link from "next/link";
import { Bed, Check, Dumbbell, Footprints, Target, Utensils } from "lucide-react";
import PulseDialog, { PulseDialogAction, PulseDialogActions } from "@/app/components/design-system/pulse-dialog";
import { saveQuestSettings } from "@/app/quests/actions";
import { formatSleepGoal, parseSleepGoal, sleepGoalError, sleepGoalHint } from "@/app/quests/sleep-goal";
import type { QuestOverview } from "@/app/quests/types";

export type QuestGoalsSettings = Pick<QuestOverview, "settings" | "configured" | "has_workout_days">;

export default function QuestGoalsDialog({
  settings, configured, has_workout_days, close,
}: QuestGoalsSettings & { close: () => void }) {
  const title = useId();
  const [error, action, pending] = useActionState(async (previous: string | null, data: FormData) => {
    const result = await saveQuestSettings(previous, data);
    if (!result) close();
    return result;
  }, null);

  const dismiss = () => {
    if (!pending) close();
  };

  return (
    <PulseDialog
      labelledBy={title}
      onDismiss={dismiss}
      panelClassName="bg-(--quest-panel)! text-(--quest-text)! max-sm:px-5.5! sheet:w-[min(600px,calc(100vw-1.5rem))] sheet:max-w-none"
    >
      <header>
        <h2 id={title} className="text-[clamp(1.6rem,3vw,1.9rem)] leading-[1.1] font-extrabold tracking-[-0.035em]">
          Quest goals
        </h2>
        <p className="mt-1.5 text-(--quest-muted)">
          {configured ? "Changes apply today and update today’s XP and hearts." : "Choose the quests you want to start today."}
        </p>
      </header>

      <form action={action}>
        <fieldset className="mt-5.5 grid gap-5.5" disabled={pending}>
          <legend className="sr-only">Quest goals</legend>
          <div className="grid gap-2.5">
            <GoalToggle name="gym_workout" title="Gym workout" value={settings.gym_workout}
              icon={<Dumbbell className="size-5" aria-hidden="true" />}>
              {has_workout_days ? "Uses your scheduled workout days." : "Add workout days to activate this quest."}{" "}
              <Link href="/fitness" onClick={dismiss}
                className="relative z-10 font-bold text-(--quest-blue) underline underline-offset-3">
                Workout schedule
              </Link>
            </GoalToggle>
            <GoalToggle name="activity_rings" title="Close activity rings" value={settings.activity_rings}
              icon={<Target className="size-5" aria-hidden="true" />}>
              Move, Exercise, and Stand.
            </GoalToggle>
            <GoalToggle name="daily_meals" title="Daily meal goal" value={settings.daily_meals}
              icon={<Utensils className="size-5" aria-hidden="true" />}>
              On days with meals in your{" "}
              <Link href="/meal-plan" onClick={dismiss}
                className="relative z-10 font-bold text-(--quest-blue) underline underline-offset-3">
                meal plan
              </Link>
              , mark each one as eaten to complete this quest.
            </GoalToggle>
          </div>

          <div className="grid gap-3.5">
            <p className="font-semibold text-(--quest-muted)">Leave a target blank to turn off that quest.</p>
            <div className="grid gap-3.5 sm:grid-cols-2">
              <TargetField name="step_target" label="Daily steps" value={settings.step_target} max={100000}
                icon={<Footprints className="size-4.5" aria-hidden="true" />} />
              <TargetField name="sleep_duration" label="Sleep (hours)" value={settings.sleep_minutes}
                icon={<Bed className="size-4.5" aria-hidden="true" />} />
            </div>
          </div>
        </fieldset>

        {error && <p role="alert" className="mt-3 text-sm text-destructive">{error}</p>}
        <PulseDialogActions className="gap-3!">
          <PulseDialogAction type="button" variant="secondary" onClick={dismiss} disabled={pending}
            className="min-h-13.5! border-(--quest-border)! font-extrabold! text-(--quest-text)!">
            Cancel
          </PulseDialogAction>
          <PulseDialogAction disabled={pending}
            className="min-h-13.5! font-extrabold! [--button-fill:linear-gradient(90deg,var(--quest-accent),#b22bd8)] [--button-foreground:white] dark:[--button-fill:var(--quest-text)] dark:[--button-foreground:var(--quest-canvas)]">
            {pending ? "Saving…" : "Save goals"}
          </PulseDialogAction>
        </PulseDialogActions>
      </form>
    </PulseDialog>
  );
}

function GoalToggle({ name, title, value, icon, children }: {
  name: string;
  title: string;
  value: boolean;
  icon: ReactNode;
  children: ReactNode;
}) {
  const id = useId();
  const [checked, setChecked] = useState(value);
  return (
    <div className="relative grid min-h-19 grid-cols-[auto_minmax(0,1fr)_auto] items-center gap-3.5 rounded-[20px] border-[1.5px] border-transparent bg-(--quest-inset) px-4 py-3 transition-colors hover:border-(--quest-border) has-checked:border-(--quest-accent)/55 has-checked:bg-(--quest-accent)/8 has-focus-visible:outline-2 has-focus-visible:outline-offset-2 has-focus-visible:outline-(--quest-accent) motion-reduce:transition-none">
      <span className="grid size-11 place-items-center rounded-xl bg-(--quest-panel) text-(--quest-blue)">{icon}</span>
      <div>
        <label htmlFor={id} className="font-extrabold tracking-tight after:absolute after:inset-0 after:cursor-pointer">
          {title}
        </label>
        <div id={`${id}-description`} className="mt-0.5 text-sm leading-snug text-(--quest-muted)">{children}</div>
      </div>
      <input id={id} type="checkbox" name={name} checked={checked} onChange={event => setChecked(event.target.checked)}
        aria-describedby={`${id}-description`} className="peer sr-only" />
      <span aria-hidden="true"
        className="grid size-7 place-items-center rounded-[10px] border-2 border-(--quest-border) bg-(--quest-panel) text-transparent peer-checked:border-transparent peer-checked:bg-(image:--quest-gradient) peer-checked:text-white">
        <Check className="size-4" strokeWidth={3} />
      </span>
    </div>
  );
}

type TargetFieldProps = {
  label: string;
  value: number | null;
  icon: ReactNode;
} & ({ name: "step_target"; max: number } | { name: "sleep_duration"; max?: never });

function TargetField({ label, name, value, max, icon }: TargetFieldProps) {
  const id = useId();
  const isSleep = name === "sleep_duration";
  const [target, setTarget] = useState(isSleep ? formatSleepGoal(value) : (value?.toString() ?? ""));
  const [invalid, setInvalid] = useState(false);
  const isOff = target.trim() === "";
  const hint = invalid
    ? max === undefined ? sleepGoalError : `Enter a whole number from 1 to ${max.toLocaleString("en-US")}.`
    : isOff ? "Quest off" : "Quest on";

  return (
    <div className="min-w-0">
      <label htmlFor={id} className="mb-2 flex items-center gap-2 text-sm font-bold">
        <span className="text-(--quest-blue)">{icon}</span>
        {label}
      </label>
      <input id={id} name={name} type={isSleep ? "text" : "number"} inputMode={isSleep ? "text" : "numeric"}
        min={isSleep ? undefined : 1} max={max} step={isSleep ? undefined : 1} value={target}
        placeholder={isSleep ? "8 or 7:30" : undefined}
        onChange={event => {
          const input = event.currentTarget;
          if (isSleep) input.setCustomValidity(parseSleepGoal(input.value) === undefined ? sleepGoalError : "");
          setTarget(input.value);
          setInvalid(!input.validity.valid);
        }}
        aria-invalid={invalid || undefined} aria-describedby={isSleep ? `${id}-format ${id}-hint` : `${id}-hint`}
        className="h-13 w-full rounded-2xl border-[1.5px] border-(--quest-border) bg-transparent px-4 font-bold tabular-nums focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-(--quest-accent) aria-invalid:border-destructive" />
      {isSleep && <span id={`${id}-format`} className="sr-only">{sleepGoalHint}</span>}
      <p id={`${id}-hint`} aria-live="polite"
        className={`mt-1.5 text-xs font-semibold ${invalid ? "text-destructive" : isOff ? "text-(--quest-muted)" : "text-(--quest-green)"}`}>
        {hint}
      </p>
    </div>
  );
}
