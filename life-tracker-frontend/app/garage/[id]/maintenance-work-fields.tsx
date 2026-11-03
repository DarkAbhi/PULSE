"use client";

import Select from "@/app/components/design-system/select";
import Button from "@/app/components/design-system/button";
import { Plus } from "lucide-react";
import { MaintenanceAction } from "./types";

export const components = ["engine_oil", "oil_filter", "air_filter", "clutch_cable", "brake_cable", "front_brake_pads", "rear_brake_pads", "brake_fluid", "chain", "spark_plug", "battery", "general_service"];
const actions = ["replaced", "inspected", "adjusted", "repaired", "topped_up", "cleaned", "lubricated", "completed"];
export const workLabel = (key: string) => key.replaceAll("_", " ").replace(/^./, (c) => c.toUpperCase());
const inputClass = "mt-1.5 min-h-11 w-full min-w-0 rounded-lg bg-secondary px-3 py-2 text-base focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring sm:text-sm";

export default function MaintenanceWorkFields({ value, onChange, disabled }: { value: MaintenanceAction[]; onChange: (value: MaintenanceAction[]) => void; disabled: boolean }) {
  function update(index: number, patch: Partial<MaintenanceAction>) {
    onChange(value.map((a, i) => i === index ? { ...a, ...patch } : a));
  }
  return <fieldset disabled={disabled} className="mt-5 min-w-0 space-y-3 rounded-xl bg-secondary/55 px-3 pb-4 sm:px-4">
    <legend className="px-1 font-semibold">Work performed</legend>
    <p className="pt-1 text-sm text-muted-foreground">Log completed work here. Choose Replaced for an oil change. For a full service, choose General service and Completed. Keep requested or uncertain work in notes.</p>
    {value.map((a, i) => <div key={i} className="grid min-w-0 gap-3 rounded-lg bg-card p-3 sm:grid-cols-2">
      <label className="text-sm font-medium">Component<Select className={inputClass} value={components.includes(a.component) ? a.component : "custom"} onChange={(e) => update(i, { component: e.target.value, ...(e.target.value === "general_service" ? { action: "completed" } : {}) })}>
        {components.map((c) => <option key={c} value={c}>{workLabel(c)}</option>)}<option value="custom">Other component</option>
      </Select></label>
      <label className="text-sm font-medium">Action<Select className={inputClass} value={a.action} onChange={(e) => update(i, { action: e.target.value })}>{actions.map((action) => <option key={action} value={action}>{workLabel(action)}</option>)}</Select></label>
      {!components.includes(a.component) && <label className="text-sm font-medium sm:col-span-2">Custom component<input required pattern="[a-z][a-z0-9_]{0,63}" maxLength={64} className={inputClass} placeholder="e.g. left_switch" value={a.component === "custom" ? "" : a.component} onChange={(e) => update(i, { component: e.target.value.toLowerCase().replaceAll(" ", "_") })} /><span className="text-xs text-muted-foreground">Letters, numbers and underscores.</span></label>}
      <label className="text-sm font-medium sm:col-span-2">Invoice wording or notes (optional)<textarea className={inputClass} maxLength={500} value={a.description} onChange={(e) => update(i, { description: e.target.value })} /></label>
      <Button type="button" variant="secondary" size="sm" className="justify-self-start sm:col-span-2" onClick={() => onChange(value.filter((_, index) => index !== i))}>Remove this action</Button>
    </div>)}
    <Button type="button" variant="secondary" disabled={value.length >= 100} onClick={() => onChange([...value, { component: "engine_oil", action: "replaced", description: "" }])}><Plus className="size-4" aria-hidden="true" /> Add work performed</Button>
  </fieldset>;
}
