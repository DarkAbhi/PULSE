"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { createMaintenanceRecord, SaveMaintenancePayload, updateMaintenanceRecord } from "./actions";
import { MaintenanceAttachment, MaintenanceRecord } from "./types";
import Dialog, { DialogAction, DialogActions } from "../../components/design-system/dialog";

const categories = ["service", "repair", "insurance", "washing", "tyres"] as const;
type Category = (typeof categories)[number];
const apiBaseURL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

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
  const [attachments, setAttachments] = useState<MaintenanceAttachment[]>(record?.attachments ?? []);
  const [isUploading, setIsUploading] = useState(false);

  function close() { router.replace(`/garage/${vehicleId}`); }

  async function uploadAttachment(file: File) {
    if (!record) return;
    setError("");
    setIsUploading(true);
    const formData = new FormData();
    formData.append("file", file);
    try {
      const response = await fetch(`${apiBaseURL}/api/vehicles/${vehicleId}/maintenance-records/${record.id}/attachments`, { method: "POST", body: formData, credentials: "include" });
      const body = await response.json().catch(() => ({}));
      if (!response.ok) { setError(body.error ?? "We couldn't upload that file."); return; }
      setAttachments((current) => [...current, body as MaintenanceAttachment]);
      router.refresh();
    } catch { setError("We couldn't reach the server."); }
    finally { setIsUploading(false); }
  }

  async function deleteAttachment(attachmentId: number) {
    if (!record) return;
    setError("");
    try {
      const response = await fetch(`${apiBaseURL}/api/vehicles/${vehicleId}/maintenance-records/${record.id}/attachments/${attachmentId}`, { method: "DELETE", credentials: "include" });
      if (!response.ok) { setError("We couldn't delete that file."); return; }
      setAttachments((current) => current.filter((attachment) => attachment.id !== attachmentId));
      router.refresh();
    } catch { setError("We couldn't reach the server."); }
  }
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

  return <Dialog labelledBy="maintenance-record-title" size="lg" panelClassName="p-6 sm:p-8">
    <form onSubmit={submit}>
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
      <section className="mt-6 rounded-xl border border-border p-4">
        <h3 className="font-semibold">Receipts and service bills</h3>
        {!record ? <p className="mt-1 text-sm text-muted-foreground">Save this record first, then reopen it to attach receipts or bills.</p> : <>
          <label className="mt-3 inline-flex cursor-pointer rounded-lg border border-border px-3 py-2 text-sm font-semibold text-primary hover:bg-accent">
            {isUploading ? "Uploading…" : "Upload file"}
            <input className="sr-only" disabled={isUploading} type="file" onChange={(event) => { const file = event.target.files?.[0]; if (file) void uploadAttachment(file); event.currentTarget.value = ""; }} />
          </label>
          <p className="mt-2 text-xs text-muted-foreground">Files are stored privately in your configured S3 bucket. Maximum 10 MB per file.</p>
          {attachments.length > 0 && <ul className="mt-3 space-y-2">{attachments.map((attachment) => <li className="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-muted/50 px-3 py-2 text-sm" key={attachment.id}><a className="font-medium text-primary hover:underline" href={`${apiBaseURL}/api/vehicles/${vehicleId}/maintenance-records/${record.id}/attachments/${attachment.id}`}>{attachment.file_name}</a><button className="font-semibold text-destructive hover:opacity-80" onClick={() => void deleteAttachment(attachment.id)} type="button">Remove</button></li>)}</ul>}
        </>}
      </section>
      {error && <p className="mt-4 text-sm text-destructive" role="alert">{error}</p>}
      <DialogActions><DialogAction variant="secondary" disabled={isPending} onClick={close} type="button">Cancel</DialogAction><DialogAction disabled={isPending} type="submit">{isPending ? "Saving…" : "Save record"}</DialogAction></DialogActions>
    </form>
  </Dialog>;
}
