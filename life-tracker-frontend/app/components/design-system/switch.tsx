"use client";

import type { ReactNode } from "react";
export default function Switch({ checked, onChange, children, disabled }: { checked: boolean; onChange: (checked: boolean) => void; children: ReactNode; disabled?: boolean }) {
  return <label className="flex min-h-20 cursor-pointer items-center justify-between gap-4 rounded-xl border border-divider bg-card p-4 has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-ring">
    <span className="flex items-center gap-3 font-medium">{children}</span>
    <span className="relative inline-flex shrink-0">
      <input className="peer absolute inset-0 z-10 h-full w-full cursor-pointer opacity-0" type="checkbox" role="switch" checked={checked} disabled={disabled} onChange={e => onChange(e.target.checked)} />
      <span aria-hidden="true" className="h-7 w-12 rounded-full bg-surface-strong transition-colors peer-checked:bg-action" />
      <span aria-hidden="true" className="pointer-events-none absolute left-1 top-1 size-5 rounded-full bg-card transition-transform peer-checked:translate-x-5 motion-reduce:transition-none" />
    </span>
  </label>;
}
