"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { createMaintenanceRecord, SaveMaintenancePayload, updateMaintenanceRecord } from "./actions";
import { MaintenanceRecord } from "./types";

const categories = ["service", "repair", "insurance", "washing", "tyres"] as const;
type Category = (typeof categories)[number];

const localDate = (value?: string) =>
  value ? new Date(value).toISOString().slice(0, 10) : new Date().toISOString().slice(0, 10);

export default function MaintenanceRecordModal({ vehicleId, record }: { vehicleId: string; record?: MaintenanceRecord }) {
  const router = useRouter();
  const [category, setCategory] = useState<Category>(record?.category ?? "service");
  const [title, setTitle] = useState(record?.title ?? "");
  const [amount, setAmount] = useState(record ? String(record.amount) : "");
  const [occurredAt, setOccurredAt] = useState(localDate(record?.occurred_at));
  const [odometer, setOdometer] = useState(record?.odometer_km == null ? "" : String(record.odometer_km));
  const [provider, setProvider] = useState(record?.provider_name ?? "");
  const [notes, setNotes] = useState(record?.notes ?? "");
  const [error, setError] = useState("");
  const [isPending, startTransition] = useTransition();

  function close() { router.replace(`/garage/${vehicleId}`); }
  function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    const payload: SaveMaintenancePayload = {
      category, title, amount: Number(amount), occurred_at: new Date(`${occurredAt}T12:00:00`).toISOString(),
      odometer_km: odometer === "" ? null : Number(odometer), provider_name: provider || null, notes: notes || null,
    };
    startTransition(async () => {
      const result = record ? await updateMaintenanceRecord(vehicleId, record.id, payload) : await createMaintenanceRecord(vehicleId, payload);
      if (!result.ok) { setError(result.error ?? "We couldn't save that record."); return; }
      close();
    });
  }

  return <div className="fixed inset-0 z-20 overflow-y-auto bg-overlay-bg px-6 py-8" role="dialog" aria-modal="true" aria-labelledby="maintenance-record-title">
    <form className="mx-auto w-full max-w-2xl rounded-2xl border border-border bg-card p-6 shadow-2xl sm:p-8" onSubmit={submit}>
      <h2 className="text-2xl font-bold tracking-tight" id="maintenance-record-title">{record ? "Edit record" : "Add maintenance or expense"}</h2>
      <p className="mt-2 text-sm text-muted-foreground">Keep this vehicle&apos;s costs and maintenance history together.</p>
      <div className="mt-6 grid gap-4 sm:grid-cols-2">
        <label className="text-sm font-medium">Category<select className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2" value={category} onChange={(e) => setCategory(e.target.value as Category)}>{categories.map((item) => <option key={item} value={item}>{item[0].toUpperCase() + item.slice(1)}</option>)}</select></label>
        <label className="text-sm font-medium">Date<input className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2" type="date" required value={occurredAt} onChange={(e) => setOccurredAt(e.target.value)} /></label>
        <label className="text-sm font-medium sm:col-span-2">Title<input className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2" maxLength={160} required placeholder="e.g. Annual service" value={title} onChange={(e) => setTitle(e.target.value)} /></label>
        <label className="text-sm font-medium">Amount (₹)<input className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2" min="0.01" required step="0.01" type="number" value={amount} onChange={(e) => setAmount(e.target.value)} /></label>
        <label className="text-sm font-medium">Odometer (km, optional)<input className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2" min="0" step="0.1" type="number" value={odometer} onChange={(e) => setOdometer(e.target.value)} /></label>
        <label className="text-sm font-medium sm:col-span-2">Service centre or provider (optional)<input className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2" maxLength={160} value={provider} onChange={(e) => setProvider(e.target.value)} /></label>
        <label className="text-sm font-medium sm:col-span-2">Notes (optional)<textarea className="mt-1 min-h-24 w-full rounded-lg border border-border bg-background px-3 py-2" value={notes} onChange={(e) => setNotes(e.target.value)} /></label>
      </div>
      {error && <p className="mt-4 text-sm text-destructive" role="alert">{error}</p>}
      <div className="mt-6 flex gap-3"><button className="flex-1 rounded-lg border border-border px-4 py-3 text-sm font-semibold" disabled={isPending} onClick={close} type="button">Cancel</button><button className="flex-1 rounded-lg bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground disabled:opacity-50" disabled={isPending} type="submit">{isPending ? "Saving…" : "Save record"}</button></div>
    </form>
  </div>;
}
