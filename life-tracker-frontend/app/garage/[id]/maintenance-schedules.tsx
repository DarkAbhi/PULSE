"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import PulseCard from "@/app/components/design-system/pulse-card";
import Button from "@/app/components/design-system/button";
import { MaintenanceInterval, MaintenanceSchedule } from "./types";
import { saveMaintenanceSchedule } from "./actions";

const sources = [
  { key: "personal", label: "My intervals" },
  { key: "manual", label: "Vehicle manual" },
] as const;
const inputClass = "mt-1.5 block min-h-11 w-full min-w-0 rounded-lg bg-secondary px-3 py-2 text-base tabular-nums focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring";

export default function MaintenanceScheduleForm({ initial, vehicleId }: { initial: MaintenanceSchedule; vehicleId: string }) {
  const [value, setValue] = useState(initial);
  const [error, setError] = useState("");
  const [pending, startTransition] = useTransition();
  const router = useRouter();
  const selected = value[value.active_source];
  const valid = selected.kilometers > 0 || selected.duration > 0;

  function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    startTransition(async () => {
      const result = await saveMaintenanceSchedule(vehicleId, value);
      if (!result.ok) {
        setError(result.error ?? "Could not save reminder settings.");
        return;
      }
      router.refresh();
    });
  }

  return (
    <form className="mt-5 sm:mt-6" onSubmit={submit}>
      <fieldset disabled={pending} className="min-w-0">
        <legend className="text-lg font-semibold">Reminder intervals</legend>
        <p id="reminder-interval-help" className="mt-1 max-w-2xl text-sm text-muted-foreground">
          Choose which schedule Pulse uses. The reminder is due when either limit is reached after the last completed work. Leave a limit blank to skip it.
        </p>
        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          {sources.map(({ key, label }) => {
            const interval = value[key];
            const active = value.active_source === key;
            const update = (patch: Partial<MaintenanceInterval>) =>
              setValue((current) => ({ ...current, [key]: { ...current[key], ...patch } }));

            return (
              <PulseCard as="section" key={key} aria-label={label} className="overflow-hidden rounded-xl">
                <label className={`flex min-h-12 cursor-pointer items-center gap-3 border-b px-4 py-3 sm:px-5 ${active ? "border-action/25 bg-accent text-accent-foreground" : "border-divider bg-secondary/45"}`}>
                  <input
                    type="radio"
                    name="active-schedule"
                    value={key}
                    checked={active}
                    aria-describedby="reminder-interval-help"
                    onChange={() => setValue((current) => ({ ...current, active_source: key }))}
                    className="size-4 shrink-0 accent-action focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"
                  />
                  <span className="flex-1 font-semibold">{label}</span>
                  {active && <span className="text-xs font-semibold" aria-hidden="true">Active</span>}
                </label>
                <div className="space-y-4 p-4 sm:p-5">
                  <div className="grid grid-cols-2 gap-3">
                    <label className="min-w-0 text-sm font-medium">
                      Distance <span className="font-normal text-muted-foreground">(km)</span>
                      <input className={inputClass} type="number" min="1" max="1000000" step="1" value={interval.kilometers || ""} onChange={(e) => update({ kilometers: Number(e.target.value) })} />
                    </label>
                    <label className="min-w-0 text-sm font-medium">
                      Time <span className="font-normal text-muted-foreground">(months)</span>
                      <input className={inputClass} type="number" min="1" max="1200" step="1" value={interval.duration || ""} onChange={(e) => update({ duration: Number(e.target.value) })} />
                    </label>
                  </div>
                  {key === "manual" && (
                    <label className="block text-sm font-medium">
                      Manual page or reference <span className="font-normal text-muted-foreground">(optional)</span>
                      <input className={inputClass} maxLength={160} value={value.manual_reference} onChange={(e) => setValue((current) => ({ ...current, manual_reference: e.target.value }))} />
                    </label>
                  )}
                </div>
              </PulseCard>
            );
          })}
        </div>
      </fieldset>
      <p className="mt-3 text-xs text-muted-foreground">
        Pulse saves both sets of intervals. Check the values you enter against your vehicle manual.
      </p>
      <div className="mt-5 flex flex-wrap items-center justify-end gap-3 border-t border-divider pt-4">
        {!valid && <p className="mr-auto text-sm text-muted-foreground">Enter at least one interval for the active schedule.</p>}
        {error && <p role="alert" className="mr-auto text-sm text-destructive">{error}</p>}
        <Button type="submit" disabled={pending || !valid}>{pending ? "Saving…" : "Save reminder settings"}</Button>
      </div>
    </form>
  );
}
