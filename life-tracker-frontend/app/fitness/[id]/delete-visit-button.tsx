"use client";

import PulseButton from "@/app/components/design-system/pulse-button";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { Trash2 } from "lucide-react";
import PulseConfirmationDialog from "@/app/components/design-system/pulse-confirmation-dialog";
import { deleteVisit } from "@/app/fitness/[id]/actions";

interface DeleteVisitButtonProps {
  visitID: string;
}

export default function DeleteVisitButton({ visitID }: DeleteVisitButtonProps) {
  const router = useRouter();
  const [isConfirmingDelete, setIsConfirmingDelete] = useState(false);
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState("");

  function handleDelete() {
    setError("");
    startTransition(async () => {
      const res = await deleteVisit(visitID);
      if (!res.ok) {
        setError(res.error ?? "We couldn't delete this gym visit. Please try again.");
      } else {
        setIsConfirmingDelete(false);
        router.replace("/fitness");
      }
    });
  }

  return (
    <>
      <PulseButton type="button" variant="outlineDanger" size="lg" labelStyle="strong"
        aria-haspopup="dialog" onClick={() => { setError(""); setIsConfirmingDelete(true); }}
        icon={<Trash2 size={19} aria-hidden="true" />}>
        Delete workout
      </PulseButton>
      <PulseConfirmationDialog
        isOpen={isConfirmingDelete}
        onClose={() => setIsConfirmingDelete(false)}
        onConfirm={handleDelete}
        title="Delete this workout?"
        description="This permanently removes the workout and every exercise and set saved with it."
        confirmText="Delete Workout"
        confirmLoadingText="Deleting…"
        isLoading={isPending}
        error={error}
        variant="destructive"
      />
    </>
  );
}
