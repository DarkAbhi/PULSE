"use client";

import { useState, useTransition } from "react";
import { Trash2 } from "lucide-react";
import { workoutDeleteIconClass } from "./workout-detail-classes";
import Button from "@/app/components/design-system/button";
import ConfirmationDialog from "@/app/components/design-system/confirmation-dialog";
import { deleteExercise } from "./actions";

export default function DeleteExerciseButton({ visitID, exerciseID, exerciseName }: {
  visitID: string;
  exerciseID: number;
  exerciseName: string;
}) {
  const [isOpen, setIsOpen] = useState(false);
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState("");

  function handleDelete() {
    setError("");
    startTransition(async () => {
      const result = await deleteExercise(visitID, exerciseID);
      if (result.ok) setIsOpen(false);
      else setError(result.error ?? "We couldn't delete this exercise. Please try again.");
    });
  }

  return (
    <>
      <Button type="button" variant="icon" size="lg" className={workoutDeleteIconClass}
        title="Delete exercise"
        aria-label={`Delete ${exerciseName}`}
        disabled={isPending}
        onClick={() => { setError(""); setIsOpen(true); }}
      >
        <Trash2 aria-hidden="true" size={19} />
      </Button>
      <ConfirmationDialog
        isOpen={isOpen}
        onClose={() => setIsOpen(false)}
        onConfirm={handleDelete}
        title="Delete this exercise?"
        description={`This permanently removes “${exerciseName}” and all its sets from this workout.`}
        confirmText="Delete exercise"
        confirmLoadingText="Deleting…"
        isLoading={isPending}
        error={error}
        variant="destructive"
      />
    </>
  );
}
