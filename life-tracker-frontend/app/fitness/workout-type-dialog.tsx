"use client";

import { useId, useState, useTransition, type SubmitEvent } from "react";
import { Dumbbell } from "lucide-react";
import PulseDialog, { PulseDialogAction, PulseDialogActions, PulseDialogHeader } from "@/app/components/design-system/pulse-dialog";
import PulseSelect from "@/app/components/design-system/pulse-select";
import { workoutTypes } from "@/app/fitness/workout-types";

const fieldClass = "min-h-11 w-full rounded-control border border-divider bg-background px-3 py-2 text-control";

export default function WorkoutTypeDialog({ title, initialType, initialFocus, weekday, onClose, onSave }: {
  title: string;
  initialType?: string | null;
  initialFocus?: string | null;
  weekday?: string;
  onClose: () => void;
  onSave: (workoutType: string, focus: string) => Promise<{ ok: boolean; error?: string }>;
}) {
  const titleID = useId();
  const [workoutType, setWorkoutType] = useState(initialType ?? "");
  const [focus, setFocus] = useState(initialFocus ?? "");
  const [error, setError] = useState("");
  const [pending, startTransition] = useTransition();
  const hasFocus = workoutType !== "" && workoutType !== "rest_day";

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    startTransition(async () => {
      const result = await onSave(workoutType, hasFocus ? focus : "");
      if (result.ok) onClose();
      else setError(result.error ?? "We couldn't save your changes.");
    });
  }

  return (
    <PulseDialog labelledBy={titleID} onDismiss={pending ? undefined : onClose}>
      <PulseDialogHeader id={titleID} icon={<Dumbbell size={18} />} onClose={onClose} closeDisabled={pending}
        description={weekday ? `Repeats every ${weekday}.` : undefined}>
        {title}
      </PulseDialogHeader>
      <form onSubmit={submit}>
        <fieldset disabled={pending} className="mt-5 grid min-w-0 gap-4">
          <label className="grid gap-1.5 text-control font-semibold">
            Workout type
            <PulseSelect className={fieldClass} value={workoutType} onChange={(event) => setWorkoutType(event.target.value)}>
              <option value="">{weekday ? "No plan" : "Unspecified"}</option>
              {weekday && <option value="rest_day">Rest day</option>}
              {Object.entries(workoutTypes).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </PulseSelect>
          </label>
          {hasFocus && (
            <label className="grid gap-1.5 text-control font-semibold">
              Focus (optional)
              <input className={fieldClass} value={focus} onChange={(event) => setFocus(event.target.value)}
                placeholder="e.g. Chest and triceps" maxLength={120} />
            </label>
          )}
        </fieldset>
        {error && <p className="mt-3 text-sm text-destructive" role="alert">{error}</p>}
        <PulseDialogActions>
          <PulseDialogAction type="button" variant="secondary" disabled={pending} onClick={onClose}>Cancel</PulseDialogAction>
          <PulseDialogAction disabled={pending}>{pending ? "Saving…" : "Save"}</PulseDialogAction>
        </PulseDialogActions>
      </form>
    </PulseDialog>
  );
}
