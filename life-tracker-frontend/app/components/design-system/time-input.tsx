"use client";
import { useId } from "react";
export default function TimeInput({ label, value, onChange, disabled }: { label: string; value: string; onChange: (value: string) => void; disabled?: boolean }) {
  const id = useId();
  return <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-divider bg-card p-5">
    <label htmlFor={id} className="font-medium">{label}</label>
    <input id={id} type="time" required step="60" value={value} disabled={disabled} onChange={e => onChange(e.target.value)} className="min-h-12 max-w-full rounded-lg border border-divider bg-background px-3 py-2 text-xl font-semibold text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring" />
  </div>;
}
