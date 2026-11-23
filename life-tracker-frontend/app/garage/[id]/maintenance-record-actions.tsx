"use client";

import Button from "@/app/components/design-system/button";

import { useState, useTransition } from "react";
import Link from "next/link";
import { Pencil, Trash2 } from "lucide-react";
import ConfirmationDialog from "@/app/components/design-system/confirmation-dialog";
import { deleteMaintenanceRecord } from "@/app/garage/[id]/actions";
import { detail } from "@/app/garage/[id]/vehicle-detail-classes";

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
    <Link className={detail.iconLink} aria-label="Edit maintenance record" href={`/garage/${vehicleId}?edit-maintenance=${recordId}`}>
      <Pencil className="size-5" aria-hidden="true" />
    </Link>
    <Button
      variant="iconChipDanger"
      size="lg"
      shape="pill"
      onClick={() => setIsOpen(true)}
      type="button"
      aria-label="Delete maintenance record"
      aria-haspopup="dialog"
    >
      <Trash2 className="size-5" aria-hidden="true" />
    </Button>
    <ConfirmationDialog isOpen={isOpen} onClose={() => setIsOpen(false)} onConfirm={remove} title="Delete this record?" description="This will permanently remove this maintenance or expense record." confirmText="Yes, delete" confirmLoadingText="Deleting…" isLoading={isPending} error={error} variant="destructive" />
  </>;
}
