"use client";

import PulseButton from "@/app/components/design-system/pulse-button";

import { useState, useEffect, useTransition } from "react";
import { deleteAirFill, deleteFuelFill } from "@/app/garage/[id]/actions";
import PulseConfirmationDialog from "@/app/components/design-system/pulse-confirmation-dialog";
import { Trash2 } from "lucide-react";

interface DeleteButtonProps {
  vehicleId: string;
  recordId: number;
  kind: "air-fills" | "fuel-fillups";
}

export default function DeleteButton({
  vehicleId,
  recordId,
  kind,
}: DeleteButtonProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [toastError, setToastError] = useState("");
  const [isPending, startTransition] = useTransition();

  // Auto-dismiss toast after 4 seconds
  useEffect(() => {
    if (toastError) {
      const timer = setTimeout(() => setToastError(""), 4000);
      return () => clearTimeout(timer);
    }
  }, [toastError]);

  const handleClear = () => {
    startTransition(async () => {
      const action = kind === "air-fills" ? deleteAirFill : deleteFuelFill;
      const res = await action(vehicleId, recordId);
      if (!res.ok) {
        setToastError(res.error ?? "We couldn't delete that record.");
      } else {
        setIsOpen(false);
      }
    });
  };

  return (
    <>
      <PulseButton
        variant="iconChipDanger"
        size="lg"
        shape="pill"
        onClick={() => setIsOpen(true)}
        type="button"
        aria-haspopup="dialog"
        aria-label={kind === "air-fills" ? "Delete air fill" : "Delete fuel fill-up"}
      >
        <Trash2 className="size-5" aria-hidden="true" />
      </PulseButton>

      <PulseConfirmationDialog
        isOpen={isOpen}
        onClose={() => setIsOpen(false)}
        onConfirm={handleClear}
        title="Delete this record?"
        description="This will permanently remove this record from your history. This cannot be undone."
        confirmText="Yes, delete"
        confirmLoadingText="Deleting…"
        isLoading={isPending}
        variant="destructive"
      />

      {/* Error Toast Notification */}
      {toastError && (
        <div
          className="fixed bottom-6 right-6 z-50 flex items-center gap-3 rounded-xl bg-destructive/10 p-4 text-destructive shadow-xl backdrop-blur-md transition-all duration-300"
          role="alert"
        >
          <span className="text-sm font-medium">{toastError}</span>
          <PulseButton variant="iconDanger" size="sm"
            onClick={() => setToastError("")}
            type="button"
            aria-label="Close"
          >
            &times;
          </PulseButton>
        </div>
      )}
    </>
  );
}
