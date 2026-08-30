"use client";

import { useState, useTransition } from "react";
import Link from "next/link";
import { Trash2 } from "lucide-react";
import ConfirmationDialog from "../../components/design-system/confirmation-dialog";
import { deleteMaintenanceRecord } from "./actions";

export default function MaintenanceRecordActions({ vehicleId, recordId }: { vehicleId: string; recordId: number }) {
  const [isOpen, setIsOpen] = useState(false);
  const [error, setError] = useState("");
  const [isPending, startTransition] = useTransition();
  function remove() {
    setError("");
    startTransition(async () => {
      const result = await deleteMaintenanceRecord(vehicleId, recordId);
      if (!result.ok) { setError(result.error ?? "We couldn't delete that record."); return; }
      setIsOpen(false);
    });
  }
  return <>
    <span className="flex items-center gap-3">
      <Link className="text-sm font-semibold text-primary hover:opacity-85" href={`/garage/${vehicleId}?edit-maintenance=${recordId}`}>Edit</Link>
      <button className="flex items-center gap-1 text-sm font-semibold text-destructive hover:opacity-80" onClick={() => setIsOpen(true)} type="button"><Trash2 className="h-4 w-4" /> Delete</button>
    </span>
    <ConfirmationDialog isOpen={isOpen} onClose={() => setIsOpen(false)} onConfirm={remove} title="Delete this record?" description="This will permanently remove this maintenance or expense record." confirmText="Yes, delete" confirmLoadingText="Deleting…" isLoading={isPending} error={error} variant="destructive" />
  </>;
}
