"use client";

import { currencySymbol } from "@/app/lib/currency.mjs";
import { useCurrency } from "@/app/components/currency-provider";
import { useTimeZone } from "@/app/components/time-zone-provider";
import { dateKey, fromDateTimeInput, formatInstant } from "@/app/lib/time-zone.mjs";

import Button from "@/app/components/design-system/button";
import { ClipboardList, Paperclip, Upload } from "lucide-react";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { createMaintenanceRecord, SaveMaintenancePayload, updateMaintenanceRecord } from "./actions";
import MaintenanceWorkFields from "./maintenance-work-fields";
import { MaintenanceAction, MaintenanceAttachment, MaintenanceRecord } from "./types";
import Dialog, { DialogAction, DialogActions } from "@/app/components/design-system/dialog";

const categories = ["service", "repair", "insurance", "washing", "tyres"] as const;
type Category = (typeof categories)[number];
const apiBaseURL = "";
const fieldClass = "mt-1.5 min-h-11 w-full min-w-0 rounded-lg border border-input bg-background px-3 py-2 text-base focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring sm:text-sm";
const optionalClass = "font-normal text-muted-foreground";
export default function MaintenanceRecordModal({ vehicleId, record }: { vehicleId: string; record?: MaintenanceRecord }) {
  const { currency } = useCurrency();
  const { timeZone } = useTimeZone();
  const router = useRouter();
  const [entryTimeZone] = useState(timeZone);
  const [category, setCategory] = useState<Category>(record?.category ?? "service");
  const [title, setTitle] = useState(record?.title ?? "");
  const [amount, setAmount] = useState(record ? String(record.amount) : "");
  const [occurredAt, setOccurredAt] = useState(dateKey(record?.occurred_at ?? new Date(), entryTimeZone));
  const [odometer, setOdometer] = useState(record?.odometer_km == null ? "" : String(record.odometer_km));
  const [provider, setProvider] = useState(record?.provider_name ?? "");
  const [notes, setNotes] = useState(record?.notes ?? "");
  const [work, setWork] = useState<MaintenanceAction[]>(record?.actions ?? []);
  const [error, setError] = useState("");
  const [isPending, startTransition] = useTransition();
  const [attachments, setAttachments] = useState<MaintenanceAttachment[]>(record?.attachments ?? []);
  const [isUploading, setIsUploading] = useState(false);

  function close() { router.replace(`/garage/${vehicleId}?tab=maintenance`, { scroll: false }); }

  async function uploadAttachment(file: File) {
    if (!record) return;
    setError("");
    if (file.size === 0 || file.size > 10 * 1024 * 1024) {
      setError("Choose a file between 1 byte and 10 MB.");
      return;
    }
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
    let occurredInstant: string;
    try {
      occurredInstant = record && dateKey(record.occurred_at, entryTimeZone) === occurredAt
        ? record.occurred_at : fromDateTimeInput(`${occurredAt}T12:00`, entryTimeZone);
    } catch (error) { setError(error instanceof Error ? error.message : "Choose a valid date."); return; }
    const payload: SaveMaintenancePayload = {
      actions: work, category, title, amount: Number(amount), occurred_at: occurredInstant,
      odometer_km: odometer === "" ? null : Number(odometer), provider_name: provider || null, notes: notes || null,
    };
    startTransition(async () => {
      const result = record ? await updateMaintenanceRecord(vehicleId, record.id, payload) : await createMaintenanceRecord(vehicleId, payload);
      if (!result.ok) { setError(result.error ?? "We couldn't save that record."); return; }
      close();
    });
  }

  return (
    <Dialog labelledBy="maintenance-record-title" size="lg">
      <form onSubmit={submit}>
        <header className="flex items-start gap-3 border-b border-divider pb-5 sm:gap-4">
          <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-primary/15 text-action" aria-hidden="true">
            <ClipboardList className="size-5" />
          </span>
          <div>
            <h2 className="text-xl font-bold tracking-tight sm:text-2xl" id="maintenance-record-title">
              {record ? "Edit maintenance record" : "Add maintenance or expense"}
            </h2>
            <p className="mt-1 text-sm text-muted-foreground">
              {record ? "Update the details, work performed, or receipts." : "Log a service, repair, or other vehicle expense."}
            </p>
          </div>
        </header>

        <fieldset className="mt-5 min-w-0">
          <legend className="font-semibold">Record details</legend>
          <div className="mt-3 grid min-w-0 gap-x-4 gap-y-4 sm:grid-cols-2">
            <label className="min-w-0 text-sm font-medium sm:col-span-2">
              Title
              <input className={fieldClass} maxLength={160} required placeholder="e.g. Annual service" value={title} onChange={(e) => setTitle(e.target.value)} />
            </label>
            <label className="min-w-0 text-sm font-medium">
              Category
              <select className={fieldClass} value={category} onChange={(e) => setCategory(e.target.value as Category)}>
                {categories.map((item) => <option key={item} value={item}>{item[0].toUpperCase() + item.slice(1)}</option>)}
              </select>
            </label>
            <label className="min-w-0 text-sm font-medium">
              Date
              <input className={fieldClass} type="date" required value={occurredAt} onChange={(e) => setOccurredAt(e.target.value)} />
            </label>
            <label className="min-w-0 text-sm font-medium">
              Amount <span className={optionalClass}>({currencySymbol(currency)})</span>
              <input className={fieldClass} min="0" required step="0.01" type="number" value={amount} onChange={(e) => setAmount(e.target.value)} />
            </label>
            <label className="min-w-0 text-sm font-medium">
              Odometer <span className={optionalClass}>(km, optional)</span>
              <input className={fieldClass} min="0" step="0.1" type="number" value={odometer} onChange={(e) => setOdometer(e.target.value)} />
            </label>
            <label className="min-w-0 text-sm font-medium sm:col-span-2">
              Service centre or provider <span className={optionalClass}>(optional)</span>
              <input className={fieldClass} maxLength={160} value={provider} onChange={(e) => setProvider(e.target.value)} />
            </label>
            <label className="min-w-0 text-sm font-medium sm:col-span-2">
              Notes <span className={optionalClass}>(optional)</span>
              <textarea className={fieldClass} rows={2} value={notes} onChange={(e) => setNotes(e.target.value)} />
            </label>
          </div>
        </fieldset>

        <MaintenanceWorkFields value={work} onChange={setWork} disabled={isPending} />

        <section aria-labelledby="maintenance-receipts-heading" className="mt-5 border-t border-divider pt-5">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h3 id="maintenance-receipts-heading" className="flex items-center gap-2 font-semibold">
              <Paperclip className="size-4 text-action" aria-hidden="true" /> Receipts and service bills
            </h3>
            {record && (
              <label className="inline-flex min-h-10 cursor-pointer items-center gap-2 rounded-lg border border-border px-3 py-2 text-sm font-semibold text-action focus-within:outline-2 focus-within:outline-offset-2 focus-within:outline-ring hover:bg-accent has-disabled:cursor-not-allowed has-disabled:opacity-50">
                <Upload className="size-4" aria-hidden="true" />
                {isUploading ? "Uploading…" : "Upload file"}
                <input className="sr-only" disabled={isUploading} type="file" onChange={(event) => {
                  const file = event.target.files?.[0];
                  if (file) void uploadAttachment(file);
                  event.currentTarget.value = "";
                }} />
              </label>
            )}
          </div>
          <p className="mt-2 text-sm text-muted-foreground">
            {record ? "Files are private. Maximum 10 MB per file." : "Save the record, then open it in edit mode to attach a file."}
          </p>
          {record && attachments.length > 0 && (
            <ul className="mt-3 divide-y divide-divider border-t border-divider">
              {attachments.map((attachment) => (
                <li className="flex flex-wrap items-center justify-between gap-2 py-2.5 text-sm" key={attachment.id}>
                  <div className="min-w-0">
                    <a className="break-all font-medium text-action hover:underline" href={`${apiBaseURL}/api/vehicles/${vehicleId}/maintenance-records/${record.id}/attachments/${attachment.id}`}>{attachment.file_name}</a>
                    <p className="mt-1 text-xs text-muted-foreground">
                      <time dateTime={attachment.created_at}>{formatInstant(attachment.created_at, timeZone, { dateStyle: "medium", timeStyle: "medium" })}</time>
                      <span className="ml-2">{Math.max(1, Math.round(attachment.size_bytes / 1024))} KB</span>
                    </p>
                  </div>
                  <Button variant="destructiveOutline" size="sm" onClick={() => void deleteAttachment(attachment.id)} type="button">Remove</Button>
                </li>
              ))}
            </ul>
          )}
        </section>

        <DialogActions className="grid-cols-2 border-t border-divider pt-3">
          {error && <p className="text-sm text-destructive col-span-2" role="alert">{error}</p>}
          <DialogAction variant="secondary" disabled={isPending} onClick={close} type="button">Cancel</DialogAction>
          <DialogAction disabled={isPending} type="submit">{isPending ? "Saving…" : record ? "Save changes" : "Save record"}</DialogAction>
        </DialogActions>
      </form>
    </Dialog>
  );
}
