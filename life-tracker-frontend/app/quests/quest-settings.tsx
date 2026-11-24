"use client";

import { useActionState, useId, useState } from "react";
import Link from "next/link";
import { Settings2 } from "lucide-react";
import PulseButton from "@/app/components/design-system/pulse-button";
import PulseDialog, { PulseDialogAction, PulseDialogActions, PulseDialogHeader } from "@/app/components/design-system/pulse-dialog";
import { saveQuestSettings } from "@/app/quests/actions";
import type { QuestOverview } from "@/app/quests/types";

type SettingsProps = Pick<QuestOverview, "settings" | "configured" | "has_workout_days">;

function QuestSettingsDialog({ settings, configured, has_workout_days, close }: SettingsProps & { close: () => void }) {
  const title = useId();
  const [gymWorkout, setGymWorkout] = useState(settings.gym_workout);
  const [activityRings, setActivityRings] = useState(settings.activity_rings);
  const [error, action, pending] = useActionState(async (previous: string | null, data: FormData) => {
    const result = await saveQuestSettings(previous, data);
    if (!result) close();
    return result;
  }, null);
  const dismiss = () => {
    if (!pending) close();
  };

  return (
    <PulseDialog labelledBy={title} onDismiss={dismiss} size="md">
      <PulseDialogHeader id={title} onClose={dismiss} closeDisabled={pending}
        description={configured ? "Changes take effect at your next midnight." : "Choose the quests you want to start today."}>
        Quest goals
      </PulseDialogHeader>
      <form action={action}>
        <fieldset className="mt-5 grid gap-4" disabled={pending}>
          <legend className="sr-only">Quest goals</legend>
          <div className="grid gap-2">
            <label className="flex items-center gap-3 font-semibold">
              <input type="checkbox" name="gym_workout" checked={gymWorkout} onChange={event => setGymWorkout(event.target.checked)} className="size-5 accent-primary" />
              Gym workout
            </label>
            <p className="text-sm text-muted-foreground">
              {has_workout_days ? "Uses your scheduled workout days." : "Add workout days to activate this quest."}{" "}
              <Link href="/fitness" className="font-semibold text-action underline underline-offset-2">Workout schedule</Link>
            </p>
          </div>
          <label className="flex items-center gap-3 font-semibold">
            <input type="checkbox" name="activity_rings" checked={activityRings} onChange={event => setActivityRings(event.target.checked)} className="size-5 accent-primary" />
            Close activity rings
          </label>
          <p className="text-sm text-muted-foreground">Leave a target blank to turn off that quest.</p>
          <div className="grid gap-3 sm:grid-cols-3">
            <TargetField name="step_target" label="Daily steps" value={settings.step_target} max={100000} />
            <TargetField name="sleep_minutes" label="Sleep (minutes)" value={settings.sleep_minutes} max={1440} />
            <TargetField name="meal_target" label="Meals eaten" value={settings.meal_target} max={24} />
          </div>
        </fieldset>
        {error && <p role="alert" className="mt-3 text-sm text-destructive">{error}</p>}
        <PulseDialogActions>
          <PulseDialogAction type="button" variant="secondary" onClick={dismiss} disabled={pending}>Cancel</PulseDialogAction>
          <PulseDialogAction disabled={pending}>{pending ? "Saving…" : "Save goals"}</PulseDialogAction>
        </PulseDialogActions>
      </form>
    </PulseDialog>
  );
}

function TargetField({ label, name, value, max }: { label: string; name: string; value: number | null; max: number }) {
  const [target, setTarget] = useState(value?.toString() ?? "");
  return (
    <label className="grid min-w-0 gap-1.5 text-sm font-semibold">
      {label}
      <input name={name} type="number" inputMode="numeric" min={1} max={max} step={1} value={target} onChange={event => setTarget(event.target.value)}
        className="min-h-11 w-full min-w-0 rounded-lg border border-divider bg-background px-3 font-normal focus-visible:outline-2 focus-visible:outline-ring" />
    </label>
  );
}

export default function QuestSettingsButton(props: SettingsProps) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <PulseButton type="button" variant={props.configured ? "iconOverlay" : "primary"} shape="pill" size="lg"
        aria-label={props.configured ? "Edit quest goals" : undefined}
        className={props.configured ? "border border-(--quest-border) bg-(--quest-panel)" : undefined}
        onClick={() => setOpen(true)}
        icon={<Settings2 className="size-5" aria-hidden="true" />}>
        {!props.configured && "Set quest goals"}
      </PulseButton>
      {open && <QuestSettingsDialog {...props} close={() => setOpen(false)} />}
    </>
  );
}
