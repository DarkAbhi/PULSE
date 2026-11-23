"use client";

import { workoutDeleteClass } from "./workout-detail-classes";
import Button from "@/app/components/design-system/button";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { Trash2 } from "lucide-react";
import ConfirmationDialog from "@/app/components/design-system/confirmation-dialog";
import { deleteVisit } from "./actions";

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
      <Button type="button" variant="destructive" className={workoutDeleteClass}
        aria-haspopup="dialog" onClick={() => { setError(""); setIsConfirmingDelete(true); }}
        icon={<Trash2 size={19} aria-hidden="true" />}>
        Delete workout
      </Button>
      <ConfirmationDialog
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
