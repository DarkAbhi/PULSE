"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import Button from "@/app/components/design-system/button";
import { MaintenanceInterval, MaintenanceSchedule } from "./types";
import { saveMaintenanceSchedule } from "./actions";

export const scheduleSourceLabel = (source: string) => source === "manual" ? "Vehicle manual" : "My intervals";
const inputClass = "mt-1 w-full rounded-lg border border-border bg-background px-3 py-2";

export default function MaintenanceScheduleForm({ initial, vehicleId }: { initial: MaintenanceSchedule; vehicleId: string }) {
  const [value, setValue] = useState(initial);
  const [error, setError] = useState("");
  const [pending, startTransition] = useTransition();
  const router = useRouter();
  const selected = value.active_source === "personal" ? value.personal : value.manual;
  const valid = selected.kilometers > 0 || selected.duration > 0;
  function fields(key: "personal" | "manual") {
    const interval = value[key];
    const update = (patch: Partial<MaintenanceInterval>) => setValue({ ...value, [key]: { ...interval, ...patch } });
    return <fieldset className="space-y-3 rounded-lg border border-border p-4"><legend className="px-1 font-semibold">{scheduleSourceLabel(key)}</legend>
      <label className="block text-sm">Distance interval (km, optional)<input className={inputClass} type="number" min="1" max="1000000" step="1" value={interval.kilometers || ""} onChange={(e) => update({ kilometers: Number(e.target.value) })} /></label>
      <label className="text-sm">Time (months, optional)<input className={inputClass} type="number" min="1" max="1200" step="1" value={interval.duration || ""} onChange={(e) => update({ duration: Number(e.target.value) })} /></label>
    </fieldset>;
  }
  return <form className="mt-4 space-y-4" onSubmit={(e) => { e.preventDefault(); setError(""); startTransition(async () => {
      const result = await saveMaintenanceSchedule(vehicleId, value);
      if (!result.ok) { setError(result.error ?? "Could not save reminder settings."); return; }
      router.refresh();
    }); }}>
      <p className="text-sm text-muted-foreground">Save both sets and choose which is active. Remind after this distance or time since the last completed work, whichever comes first. Leave an unused limit blank.</p>
      <label className="block text-sm font-medium">Active schedule<select disabled={pending} className={inputClass} value={value.active_source} onChange={(e) => setValue({ ...value, active_source: e.target.value as MaintenanceSchedule["active_source"] })}>
        <option value="personal">My intervals</option><option value="manual">Vehicle manual</option>
      </select></label>

      <fieldset disabled={pending} className="grid gap-4 sm:grid-cols-2">{fields("personal")}{fields("manual")}</fieldset>
      <label className="block text-sm">Manual reference or page (optional)<input disabled={pending} className={inputClass} maxLength={160} value={value.manual_reference} onChange={(e) => setValue({ ...value, manual_reference: e.target.value })} /></label>
      <p className="text-xs text-muted-foreground">Manual values are entered by you and are not independently verified by Pulse. Inactive values stay saved.</p>
      {!valid && <p className="text-sm text-muted-foreground">Enter at least one interval for the active schedule.</p>}
      {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
      <Button type="submit" disabled={pending || !valid}>{pending ? "Saving…" : "Save reminder settings"}</Button>
    </form>;
}
